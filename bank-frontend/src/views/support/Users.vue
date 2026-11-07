<script setup lang="ts">
/**
 * 用户管理 — 实名核验与证件资料 (客服工作台).
 *
 * The backend exposes exactly two staff endpoints here, both backed by
 * `t_id_card_lib` (the identity-card + face-feature library that
 * `/user/face/realname/verify` compares against):
 *
 *   POST /support/admin/idcard/enroll  {idCard, name, faceImage}
 *     IdCardLibServiceImpl.enroll:33  requires a staff token (currentStaff())
 *     IdCardLibServiceImpl:36         rejects an already-enrolled 身份证号 (ID_CARD_EXISTS)
 *     IdCardLibServiceImpl:39         extracts the 512-dim face feature — a photo
 *                                     WITHOUT a detectable face makes the face
 *                                     service return 404 and the enroll fails
 *   GET  /support/admin/idcard/list  -> IdCardLibVO[] {id, idCard, name, creationDate}
 *
 * The face feature itself is never returned to the client, so this page is a
 * 录入 + 台账 view rather than an editor.
 */
import { computed, h, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  BadgeCheck,
  IdCard,
  ImagePlus,
  RefreshCw,
  ScanFace,
  Search,
  ShieldCheck,
  Trash2,
  Upload,
  Users,
} from 'lucide-vue-next'
import { Alert, Badge, Button, Card, EmptyState, Input, Modal, StatCard, Table } from '@/components/ui'
import { PageHeader } from '@/components/layout'
import { usePageMeta } from '@/composables/usePageMeta'
import { enrollIdCard, getIdCardList } from '@/api/support'
import { errorMessage, readImageAsBase64, validateIdCard, validateUserName, type FieldErrors } from '@/utils/validation'
import { formatDate, formatDateTime } from '@/utils/format'
import type { IdCardLibEntry } from '@/types'

const { title, subtitle } = usePageMeta()

const loading = ref(true)
const pageError = ref('')
const pageNotice = ref('')
const entries = ref<IdCardLibEntry[]>([])
const keyword = ref('')
/** Row ids whose full 身份证号 is currently revealed (PII defaults to masked). */
const revealed = ref<number[]>([])

const filtered = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return entries.value
  return entries.value.filter((entry) =>
    [entry.name, entry.idCard].filter(Boolean).some((field) => String(field).toLowerCase().includes(q)),
  )
})

/** 今日新增 — derived client-side; creationDate is the only timestamp the VO exposes. */
const todayCount = computed(() => {
  const today = formatDate(new Date())
  return entries.value.filter((entry) => formatDate(entry.creationDate) === today).length
})

const latest = computed(() => entries.value[0]?.creationDate ?? '')

/** `110101199001011234` -> `110101********1234` */
function maskIdCard(idCard: string): string {
  const value = (idCard ?? '').trim()
  if (value.length < 10) return value || '--'
  return `${value.slice(0, 6)}${'*'.repeat(value.length - 10)}${value.slice(-4)}`
}

function toggleReveal(id: number) {
  const index = revealed.value.indexOf(id)
  if (index === -1) revealed.value.push(id)
  else revealed.value.splice(index, 1)
}

async function load() {
  loading.value = true
  pageError.value = ''
  try {
    entries.value = (await getIdCardList()) ?? []
  } catch (e) {
    pageError.value = errorMessage(e, '证件库加载失败')
    entries.value = []
  } finally {
    loading.value = false
  }
}

/* ------------------------------------------------------------ enroll modal */

const enrollOpen = ref(false)
const enrollForm = ref({ idCard: '', name: '' })
const enrollErrors = ref<FieldErrors>({})
const enrollError = ref('')
const enrolling = ref(false)
const photoPreview = ref('')
const photoName = ref('')
const photoSize = ref(0)
/** Raw base64 (no `data:` prefix) — the face service base64-decodes the payload as-is. */
const photoPayload = ref('')

const MAX_PHOTO_BYTES = 4 * 1024 * 1024
const photoInput = ref<HTMLInputElement | null>(null)

function openEnroll() {
  enrollForm.value = { idCard: '', name: '' }
  enrollErrors.value = {}
  enrollError.value = ''
  clearPhoto()
  enrollOpen.value = true
}

function clearPhoto() {
  photoPreview.value = ''
  photoPayload.value = ''
  photoName.value = ''
  photoSize.value = 0
}

async function onPhotoChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  enrollError.value = ''
  if (!/^image\/(png|jpeg|jpg)$/i.test(file.type)) {
    enrollError.value = '仅支持 JPG / PNG 格式的证件照'
    return
  }
  if (file.size > MAX_PHOTO_BYTES) {
    enrollError.value = '照片不能超过 4MB'
    return
  }

  try {
    const [withPrefix, raw] = await Promise.all([readImageAsBase64(file, true), readImageAsBase64(file, false)])
    photoPreview.value = withPrefix
    photoPayload.value = raw
    photoName.value = file.name
    photoSize.value = file.size
  } catch {
    enrollError.value = '照片读取失败，请重新选择'
  }
}

function validateEnroll(): boolean {
  const errors: FieldErrors = {}
  const idCardError = validateIdCard(enrollForm.value.idCard)
  if (idCardError) errors.idCard = idCardError
  const nameError = validateUserName(enrollForm.value.name)
  if (nameError) errors.name = nameError
  if (!photoPayload.value) errors.faceImage = '请上传人脸照片'
  enrollErrors.value = errors
  return Object.keys(errors).length === 0
}

async function submitEnroll() {
  enrollError.value = ''
  if (!validateEnroll()) return

  enrolling.value = true
  try {
    await enrollIdCard({
      idCard: enrollForm.value.idCard.trim().toUpperCase(),
      name: enrollForm.value.name.trim(),
      faceImage: photoPayload.value,
    })
    enrollOpen.value = false
    pageNotice.value = `已录入 ${enrollForm.value.name.trim()} 的证件资料，用户完成人脸实名核验时将比对该记录`
    await load()
  } catch (e) {
    enrollError.value = errorMessage(e, '录入失败：请确认照片中能清晰检测到人脸')
  } finally {
    enrolling.value = false
  }
}

/* ------------------------------------------------------------------ copying */

const copiedId = ref<number | null>(null)
let copiedTimer: ReturnType<typeof setTimeout> | undefined

async function copyIdCard(entry: IdCardLibEntry) {
  try {
    await navigator.clipboard.writeText(entry.idCard)
    copiedId.value = entry.id
    if (copiedTimer) clearTimeout(copiedTimer)
    copiedTimer = setTimeout(() => (copiedId.value = null), 1500)
  } catch {
    pageError.value = '当前浏览器环境禁止访问剪贴板，请手动展开后复制'
  }
}

onBeforeUnmount(() => {
  if (copiedTimer) clearTimeout(copiedTimer)
})

/* ------------------------------------------------------------------ columns */

const columns = [
  {
    key: 'id',
    header: '编号',
    width: '80px',
    render: (row: IdCardLibEntry) => h('span', { class: 'font-mono text-xs text-navy-500 dark:text-navy-400' }, `#${row.id}`),
  },
  {
    key: 'name',
    header: '姓名',
    width: '160px',
    render: (row: IdCardLibEntry) => h('span', { class: 'font-medium text-navy-900 dark:text-navy-50' }, row.name || '--'),
  },
  {
    key: 'idCard',
    header: '身份证号',
    width: '260px',
    render: (row: IdCardLibEntry) => {
      const shown = revealed.value.includes(row.id)
      return h('div', { class: 'flex items-center gap-2' }, [
        h(
          'span',
          { class: 'font-mono text-sm text-navy-700 dark:text-navy-200' },
          shown ? row.idCard : maskIdCard(row.idCard),
        ),
        h(
          'button',
          {
            type: 'button',
            class: 'text-navy-400 transition-colors hover:text-primary-600 dark:hover:text-primary-400',
            title: shown ? '隐藏' : '查看完整号码',
            onClick: () => toggleReveal(row.id),
          },
          shown ? '隐藏' : '查看',
        ),
        h(
          'button',
          {
            type: 'button',
            class: 'text-navy-400 transition-colors hover:text-primary-600 dark:hover:text-primary-400',
            title: '复制身份证号',
            onClick: () => copyIdCard(row),
          },
          copiedId.value === row.id ? '已复制' : '复制',
        ),
      ])
    },
  },
  {
    key: 'faceFeature',
    header: '人脸特征',
    width: '130px',
    render: () =>
      h(Badge, { variant: 'success', dot: true }, () => '已采集 512 维'),
  },
  {
    key: 'creationDate',
    header: '录入时间',
    width: '180px',
    render: (row: IdCardLibEntry) =>
      h('span', { class: 'text-sm text-navy-600 dark:text-navy-300' }, formatDateTime(row.creationDate)),
  },
]

onMounted(load)
</script>

<template>
  <div class="app-container">
    <PageHeader
      :title="title"
      :subtitle="subtitle"
    >
      <template #actions>
        <Button
          variant="secondary"
          size="sm"
          :loading="loading"
          @click="load"
        >
          <RefreshCw class="h-4 w-4" />
          刷新
        </Button>
        <Button
          size="sm"
          @click="openEnroll"
        >
          <ImagePlus class="h-4 w-4" />
          录入证件资料
        </Button>
      </template>
    </PageHeader>

    <Alert
      v-if="pageError"
      variant="danger"
      title="操作未完成"
      :description="pageError"
      dismissible
      class="mb-6"
      @dismiss="pageError = ''"
    />
    <Alert
      v-if="pageNotice"
      variant="success"
      title="录入成功"
      :description="pageNotice"
      dismissible
      class="mb-6"
      @dismiss="pageNotice = ''"
    />

    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <StatCard
        label="证件库记录"
        :value="entries.length"
        hint="已录入的实名资料总数"
        :icon="Users"
        :loading="loading"
      />
      <StatCard
        label="今日新增"
        :value="todayCount"
        hint="今天完成录入的证件"
        :icon="BadgeCheck"
        :loading="loading"
      />
      <StatCard
        label="最近录入"
        :value="latest ? formatDate(latest) : '--'"
        hint="证件库最后一次更新时间"
        :icon="ShieldCheck"
        :loading="loading"
      />
    </div>

    <Card class="mb-6">
      <div class="card-body flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="relative flex-1 sm:max-w-md">
          <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-navy-400" />
          <Input
            v-model="keyword"
            placeholder="搜索姓名或身份证号"
            class="pl-9"
          />
        </div>
        <p class="text-body-sm text-navy-500 dark:text-navy-400">
          共 {{ filtered.length }} 条记录 · 身份证号默认脱敏显示
        </p>
      </div>
    </Card>

    <Card v-if="!loading && !entries.length && !pageError">
      <EmptyState
        title="证件库还是空的"
        description="录入第一条身份证资料后，用户即可通过人脸识别完成实名认证。"
        :icon="IdCard"
      >
        <template #action>
          <Button @click="openEnroll">
            <ImagePlus class="h-4 w-4" />
            录入证件资料
          </Button>
        </template>
      </EmptyState>
    </Card>

    <Card v-else>
      <Table
        :columns="columns"
        :data="filtered"
        key-field="id"
        :loading="loading"
        :hoverable="true"
        :striped="true"
        bleed
        empty-message="没有匹配的证件记录"
      />
    </Card>

    <!-- 录入证件资料 -->
    <Modal
      v-model="enrollOpen"
      title="录入身份证资料"
      size="lg"
      :close-on-overlay-click="!enrolling"
    >
      <div class="space-y-5">
        <Alert
          v-if="enrollError"
          variant="danger"
          :description="enrollError"
        />

        <Alert
          variant="info"
          title="人脸照片要求"
          description="照片需清晰可辨、光线充足，且只能包含一张人脸。系统会即时提取 512 维人脸特征并加密入库，用于后续实名核验比对。"
        />

        <div>
          <label
            class="label"
            for="enroll-idcard"
          >身份证号</label>
          <Input
            id="enroll-idcard"
            v-model="enrollForm.idCard"
            placeholder="18 位身份证号"
            :error="enrollErrors.idCard"
            maxlength="18"
          />
        </div>

        <div>
          <label
            class="label"
            for="enroll-name"
          >姓名</label>
          <Input
            id="enroll-name"
            v-model="enrollForm.name"
            placeholder="与身份证一致的姓名"
            :error="enrollErrors.name"
            maxlength="20"
          />
        </div>

        <div>
          <span class="label">人脸照片</span>
          <div class="flex flex-col gap-4 sm:flex-row sm:items-start">
            <div
              class="h-32 w-32 shrink-0 overflow-hidden rounded-xl border-2 border-dashed border-navy-300 bg-navy-50 dark:border-navy-600 dark:bg-navy-800/40"
            >
              <img
                v-if="photoPreview"
                :src="photoPreview"
                alt="证件照预览"
                class="h-full w-full object-cover"
              >
              <div
                v-else
                class="flex h-full w-full flex-col items-center justify-center gap-1 text-navy-400"
              >
                <ScanFace class="h-7 w-7" />
                <span class="text-xs">暂无照片</span>
              </div>
            </div>

            <div class="flex-1 space-y-2">
              <input
                ref="photoInput"
                type="file"
                accept="image/png,image/jpeg"
                class="hidden"
                @change="onPhotoChange"
              >
              <div class="flex flex-wrap items-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  @click="photoInput?.click()"
                >
                  <Upload class="h-4 w-4" />
                  {{ photoPreview ? '更换照片' : '选择照片' }}
                </Button>
                <Button
                  v-if="photoPreview"
                  variant="ghost"
                  size="sm"
                  @click="clearPhoto"
                >
                  <Trash2 class="h-4 w-4" />
                  移除
                </Button>
              </div>
              <p
                v-if="photoName"
                class="text-body-xs text-navy-500 dark:text-navy-400"
              >
                {{ photoName }} · {{ (photoSize / 1024).toFixed(0) }} KB
              </p>
              <p
                v-else
                class="form-hint"
              >
                支持 JPG / PNG，单张不超过 4MB。
              </p>
              <p
                v-if="enrollErrors.faceImage"
                class="form-error"
              >
                {{ enrollErrors.faceImage }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <Button
          variant="secondary"
          :disabled="enrolling"
          @click="enrollOpen = false"
        >
          取消
        </Button>
        <Button
          :loading="enrolling"
          @click="submitEnroll"
        >
          <IdCard class="h-4 w-4" />
          提交录入
        </Button>
      </template>
    </Modal>
  </div>
</template>
