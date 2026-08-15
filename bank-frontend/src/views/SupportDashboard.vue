<template>
  <div class="support-layout">
    <aside class="support-aside">
      <div class="aside-brand">
        <div class="brand-icon">钱</div>
        <div class="brand-text">
          <div class="brand-title">钱钱银行</div>
          <div class="brand-sub">客服管理后台</div>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="aside-menu"
        background-color="transparent"
        text-color="rgba(212,176,106,0.72)"
        active-text-color="#e6c98a"
        @select="onMenuSelect"
      >
        <el-menu-item index="overview">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="tickets">
          <el-icon><Tickets /></el-icon>
          <span>工单管理</span>
        </el-menu-item>
        <el-menu-item index="idcard">
          <el-icon><Postcard /></el-icon>
          <span>身份库管理</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">客服人员专用通道</div>
    </aside>

    <main class="support-main">
      <header class="support-header">
        <div class="header-title">{{ headerTitle }}</div>
        <div class="header-right">
          <div class="server-status">
            <span class="vault-status-dot"></span>
            <span class="server-text">服务在线</span>
          </div>
          <el-tag v-if="activeMenu === 'tickets'" size="small" type="warning" effect="plain">当前视图：{{ statusText(activeStatus) }}</el-tag>
          <div class="staff-info">
            <el-icon><UserFilled /></el-icon>
            <span>{{ staffName }}</span>
          </div>
          <el-button size="small" type="danger" plain @click="logout">退出登录</el-button>
        </div>
      </header>

      <section class="support-content">
        <!-- 数据概览 -->
        <template v-if="activeMenu === 'overview'">
          <div class="stat-row">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-num vault-num" style="color:var(--vault-accent)">{{ stats.total }}</div>
              <div class="stat-label">工单总数</div>
            </el-card>
            <el-card shadow="hover" class="stat-card">
              <div class="stat-num vault-num" style="color:var(--vault-down)">{{ stats.pending }}</div>
              <div class="stat-label">待处理</div>
            </el-card>
            <el-card shadow="hover" class="stat-card">
              <div class="stat-num vault-num" style="color:var(--vault-gold)">{{ stats.processing }}</div>
              <div class="stat-label">处理中</div>
            </el-card>
            <el-card shadow="hover" class="stat-card">
              <div class="stat-num vault-num" style="color:var(--vault-up)">{{ stats.closed }}</div>
              <div class="stat-label">已关闭</div>
            </el-card>
            <el-card shadow="hover" class="stat-card invalid-card">
              <div class="stat-num vault-num" style="color:var(--vault-text-3)">{{ stats.invalid }}</div>
              <div class="stat-label">无效/恶意</div>
            </el-card>
          </div>

          <el-card class="todo-card">
            <template #header>
              <div class="card-header">
                <span>最新待处理工单</span>
                <el-button size="small" @click="goTickets">查看全部</el-button>
              </div>
            </template>
            <el-table :data="pendingList" v-loading="loading" stripe style="width:100%">
              <el-table-column prop="ticketNo" label="工单号" width="150" />
              <el-table-column prop="categoryText" label="类型" width="90" />
              <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
              <el-table-column prop="userName" label="用户名" width="100" />
              <el-table-column label="情绪" width="80">
                <template #default="{ row }">
                  <el-tag :type="sentimentType(row.sentiment)" size="small">{{ row.sentiment || '--' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="AI评分" width="90">
                <template #default="{ row }">
                  <span :style="{ color: scoreColor(row.aiScore) }">{{ row.aiScore ?? '--' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="创建时间" width="160">
                <template #default="{ row }">{{ formatTime(row.creationDate) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button size="small" type="primary" @click="goTickets">去处理</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!loading && pendingList.length === 0" description="暂无待处理工单" :image-size="70" />
          </el-card>
        </template>

        <!-- 工单管理 -->
        <template v-else-if="activeMenu === 'tickets'">
          <el-card class="ticket-card">
            <template #header>
              <div class="card-header">
                <span>客户服务工单</span>
                <el-button size="small" @click="loadAll()">刷新</el-button>
              </div>
            </template>

            <el-tabs v-model="activeStatus">
              <el-tab-pane label="全部" name="all" />
              <el-tab-pane label="待处理" name="0" />
              <el-tab-pane label="处理中" name="1" />
              <el-tab-pane label="已回复" name="2" />
              <el-tab-pane label="已关闭" name="3" />
              <el-tab-pane label="无效/恶意" name="invalid" />
            </el-tabs>

            <el-table
              :data="filteredTickets"
              v-loading="loading"
              stripe
              style="width:100%"
              :row-class-name="rowClass"
            >
              <el-table-column prop="ticketNo" label="工单号" width="150" />
              <el-table-column prop="categoryText" label="类型" width="80" />
              <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
              <el-table-column prop="accountNumber" label="账户" width="130" />
              <el-table-column prop="userName" label="用户名" width="85" />
              <el-table-column prop="statusText" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="情绪" width="80">
                <template #default="{ row }">
                  <el-tag :type="sentimentType(row.sentiment)" size="small">{{ row.sentiment || '--' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="有效" width="70">
                <template #default="{ row }">
                  <el-tag :type="row.aiValid === 0 ? 'info' : 'success'" size="small" effect="plain">
                    {{ row.aiValid === 0 ? '无效' : '有效' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="AI评估" width="180">
                <template #default="{ row }">
                  <el-tooltip :content="row.aiEval || '--'" placement="top">
                    <span class="ai-eval">
                      <b :style="{ color: scoreColor(row.aiScore) }">{{ row.aiScore ?? '--' }}分</b>
                      {{ (row.aiEval || '--') }}
                    </span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="handlerName" label="处理人" width="85" />
              <el-table-column label="创建时间" width="150">
                <template #default="{ row }">{{ formatTime(row.creationDate) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <template v-if="row.aiValid !== 0">
                    <el-button v-if="row.status === 0" size="small" type="primary" @click="handleAccept(row)">受理</el-button>
                    <el-button v-if="row.status !== 3" size="small" type="success" @click="openReply(row)">回复</el-button>
                  </template>
                  <el-button v-if="row.status !== 3" size="small" @click="handleClose(row)">关闭</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-if="!loading && filteredTickets.length === 0" description="暂无工单" />
          </el-card>
        </template>

        <!-- 身份库管理 -->
        <template v-else-if="activeMenu === 'idcard'">
          <el-card class="idcard-card">
            <template #header>
              <div class="card-header">
                <span>身份库录入</span>
                <span class="idcard-tip">身份证号 + 姓名 + 人脸照片建档，用户实名认证时将与此库比对</span>
              </div>
            </template>
            <el-form :model="idCardForm" label-width="90px" class="idcard-form">
              <el-form-item label="身份证号">
                <el-input v-model="idCardForm.idCard" placeholder="18位身份证号" maxlength="18" clearable style="width:260px" />
              </el-form-item>
              <el-form-item label="姓名">
                <el-input v-model="idCardForm.name" placeholder="真实姓名" maxlength="20" clearable style="width:260px" />
              </el-form-item>
              <el-form-item label="人脸照片">
                <div class="idcard-upload-row">
                  <el-upload
                    accept="image/*"
                    :auto-upload="false"
                    :show-file-list="false"
                    :on-change="onIdCardImage"
                  >
                    <el-button :type="idCardImage ? 'success' : 'primary'">
                      {{ idCardImage ? '已选择照片' : '选择照片' }}
                    </el-button>
                  </el-upload>
                  <el-image v-if="idCardPreview" :src="idCardPreview" fit="cover" class="idcard-preview" />
                </div>
              </el-form-item>
              <el-form-item>
                <el-button type="warning" :loading="enrolling" @click="handleEnroll">录入身份库</el-button>
                <el-button @click="resetIdCardForm">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card class="idcard-card">
            <template #header>
              <div class="card-header">
                <span>身份库记录（{{ idCards.length }}）</span>
                <el-button size="small" @click="loadIdCards">刷新</el-button>
              </div>
            </template>
            <el-table :data="idCards" v-loading="idLoading" stripe style="width:100%">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column label="身份证号" width="240">
                <template #default="{ row }">{{ maskId(row.idCard) }}</template>
              </el-table-column>
              <el-table-column prop="name" label="姓名" width="140" />
              <el-table-column label="录入时间" min-width="180">
                <template #default="{ row }">{{ formatTime(row.creationDate) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!idLoading && idCards.length === 0" description="身份库暂无记录" />
          </el-card>
        </template>
      </section>
    </main>

    <el-dialog v-model="detailVisible" title="工单详情" width="520px" destroy-on-close>
      <div v-if="current" class="ticket-detail">
        <div class="detail-item"><label>标题：</label>{{ current.title }}</div>
        <div class="detail-item"><label>类型：</label>{{ current.categoryText }}</div>
        <div class="detail-item"><label>账户：</label>{{ current.accountNumber }}</div>
        <div class="detail-item"><label>用户名：</label>{{ current.userName }}（{{ current.userPhone }}）</div>
        <div class="detail-item"><label>描述：</label>{{ current.description }}</div>
        <div v-if="current.aiEval" class="detail-item reply">
          <label>AI评估：</label>【{{ current.sentiment || '--' }} · {{ current.aiScore ?? '--' }}分 · {{ current.aiValid === 0 ? '无效' : '有效' }}】{{ current.aiEval }}
        </div>
        <div v-if="current.reply" class="detail-item reply"><label>客服回复：</label>{{ current.reply }}</div>
      </div>
    </el-dialog>

    <el-dialog v-model="replyVisible" title="回复工单" width="480px" destroy-on-close>
      <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="请输入回复内容" maxlength="1000" show-word-limit />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="handleReply">确认回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllTickets, acceptTicket, replyTicket, closeTicket, enrollIdCard, getIdCardList } from '@/api/support'

const router = useRouter()
const staffName = ref(localStorage.getItem('staffName') || '客服')
const activeMenu = ref('overview')
const activeStatus = ref('all')
const tickets = ref([])
const loading = ref(false)
const current = ref(null)
const detailVisible = ref(false)
const replyVisible = ref(false)
const replying = ref(false)
const replyContent = ref('')
const idCardForm = ref({ idCard: '', name: '' })
const idCardImage = ref('')
const idCardPreview = ref('')
const idCards = ref([])
const idLoading = ref(false)
const enrolling = ref(false)

const headerTitle = computed(() => {
  if (activeMenu.value === 'overview') return '数据概览'
  if (activeMenu.value === 'idcard') return '身份库管理'
  return '工单管理'
})

const statusTextMap = { all: '全部', 0: '待处理', 1: '处理中', 2: '已回复', 3: '已关闭', invalid: '无效/恶意' }
const statusText = (s) => statusTextMap[s] || s

const filteredTickets = computed(() => {
  if (activeStatus.value === 'invalid') return tickets.value.filter((t) => Number(t.aiValid) === 0)
  if (activeStatus.value === 'all') return tickets.value
  const st = Number(activeStatus.value)
  return tickets.value.filter((t) => Number(t.status) === st && Number(t.aiValid) === 1)
})

const stats = computed(() => {
  const s = { total: tickets.value.length, pending: 0, processing: 0, replied: 0, closed: 0, invalid: 0 }
  tickets.value.forEach((t) => {
    const st = Number(t.status)
    if (Number(t.aiValid) === 0) s.invalid++
    else if (st === 0) s.pending++
    else if (st === 1) s.processing++
    else if (st === 2) s.replied++
    else if (st === 3) s.closed++
  })
  return s
})

const pendingList = computed(() =>
  tickets.value.filter((t) => Number(t.status) === 0 && Number(t.aiValid) === 1)
)

const statusType = (s) => {
  if (s === 0) return 'danger'
  if (s === 1) return 'warning'
  if (s === 2) return 'success'
  return 'info'
}

const sentimentType = (s) => {
  if (s === '积极') return 'success'
  if (s === '消极') return 'warning'
  if (s === '恶意') return 'danger'
  return 'info'
}

const scoreColor = (score) => {
  if (score == null) return 'var(--vault-text-3)'
  if (score >= 60) return 'var(--vault-up)'
  if (score >= 40) return 'var(--vault-gold)'
  return 'var(--vault-down)'
}

const rowClass = ({ row }) => (Number(row.aiValid) === 0 ? 'invalid-row' : '')

const formatTime = (t) => {
  if (!t) return '--'
  return String(t).replace('T', ' ').substring(0, 16)
}

const onMenuSelect = (index) => {
  activeMenu.value = index
  if (index === 'idcard') loadIdCards()
}

const goTickets = () => {
  activeMenu.value = 'tickets'
  activeStatus.value = '0'
}

const loadAll = async () => {
  loading.value = true
  try {
    const res = await getAllTickets()
    tickets.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleAccept = async (row) => {
  try {
    await acceptTicket(row.id)
    ElMessage.success('已受理')
    loadAll()
  } catch (e) {
    console.error(e)
  }
}

const openReply = (row) => {
  current.value = row
  replyContent.value = row.reply || ''
  replyVisible.value = true
}

const handleReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyTicket({ ticketId: current.value.id, reply: replyContent.value })
    ElMessage.success('回复成功')
    replyVisible.value = false
    loadAll()
  } catch (e) {
    console.error(e)
  } finally {
    replying.value = false
  }
}

const handleClose = async (row) => {
  try {
    await ElMessageBox.confirm('确认关闭该工单？', '提示', { type: 'warning' })
    await closeTicket(row.id)
    ElMessage.success('工单已关闭')
    loadAll()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') console.error(e)
  }
}

const logout = async () => {
  try {
    await ElMessageBox.confirm('确认退出客服后台？', '提示', { type: 'warning' })
    localStorage.removeItem('staff_token')
    localStorage.removeItem('staffName')
    router.push('/support/login')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') console.error(e)
  }
}

const maskId = (id) => {
  if (!id || id.length < 10) return id || '--'
  return id.substring(0, 6) + '********' + id.substring(id.length - 4)
}

const onIdCardImage = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    idCardImage.value = e.target.result.split(',')[1]
    idCardPreview.value = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

const resetIdCardForm = () => {
  idCardForm.value = { idCard: '', name: '' }
  idCardImage.value = ''
  idCardPreview.value = ''
}

const handleEnroll = async () => {
  const f = idCardForm.value
  if (!/^\d{17}[\dXx]$/.test(f.idCard)) {
    ElMessage.warning('请输入正确的18位身份证号')
    return
  }
  if (!f.name.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (!idCardImage.value) {
    ElMessage.warning('请选择人脸照片')
    return
  }
  enrolling.value = true
  try {
    await enrollIdCard({ idCard: f.idCard, name: f.name, image: idCardImage.value })
    ElMessage.success('身份库录入成功')
    resetIdCardForm()
    loadIdCards()
  } catch (e) {
    console.error(e)
  } finally {
    enrolling.value = false
  }
}

const loadIdCards = async () => {
  idLoading.value = true
  try {
    const res = await getIdCardList()
    idCards.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    idLoading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.support-layout { display: flex; min-height: 100vh; background: var(--vault-bg); }
.support-aside {
  width: 220px; flex-shrink: 0;
  background: var(--vault-graphite);
  border-right: 1px solid var(--vault-line);
  display: flex; flex-direction: column;
}
.aside-brand {
  display: flex; align-items: center; gap: 12px;
  padding: 22px 20px 18px;
  border-bottom: 1px solid var(--vault-line);
}
.brand-icon {
  width: 42px; height: 42px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 800;
  color: #231a08;
  background: linear-gradient(135deg, #e6c98a, #b08d4a);
  box-shadow: 0 0 18px var(--vault-accent-glow);
}
.brand-title { font-size: 17px; font-weight: 700; color: var(--vault-text); letter-spacing: 1px; }
.brand-sub { font-size: 11px; color: var(--vault-text-3); letter-spacing: 3px; margin-top: 2px; }
.aside-menu { border-right: none; flex: 1; padding-top: 10px; background: transparent; }
.aside-menu :deep(.el-menu-item) { height: 50px; }
.aside-menu :deep(.el-menu-item.is-active) { background: rgba(212,176,106,0.1); }
.aside-menu :deep(.el-menu-item:hover) { background: rgba(212,176,106,0.06); }
.aside-footer { padding: 16px 20px; font-size: 11px; color: var(--vault-text-dim); letter-spacing: 1px; }
.support-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.support-header {
  height: 60px; flex-shrink: 0;
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 24px;
  background: rgba(13,20,32,0.9);
  border-bottom: 1px solid var(--vault-line);
  backdrop-filter: blur(8px);
}
.header-title { font-size: 17px; font-weight: 700; color: var(--vault-text); letter-spacing: 0.08em; }
.header-right { display: flex; align-items: center; gap: 16px; }
.server-status { display: flex; align-items: center; gap: 8px; }
.server-text { font-size: 12px; color: var(--vault-text-3); letter-spacing: 0.05em; }
.staff-info { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; color: var(--vault-text-2); }
.support-content { flex: 1; padding: 20px 24px; overflow: auto; }
.stat-row { display: flex; gap: 16px; margin-bottom: 16px; }
.stat-card { flex: 1; text-align: center; padding: 8px 0; border: 1px solid var(--vault-line); background: linear-gradient(180deg, var(--vault-panel-2), var(--vault-panel)); box-shadow: inset 0 1px 0 rgba(255,255,255,0.03), 0 12px 32px rgba(0,0,0,0.25); }
.invalid-card { opacity: 0.85; }
.stat-num { font-size: 34px; font-weight: 700; }
.stat-label { font-size: 13px; color: var(--vault-text-3); margin-top: 6px; }
.card-header { display: flex; justify-content: space-between; align-items: center; color: var(--vault-text); }
.todo-card { margin-bottom: 16px; border-color: var(--vault-line); }
.ticket-card :deep(.el-tabs__item) { font-weight: 500; }
.ticket-card :deep(.el-table .invalid-row) { color: var(--vault-text-3); }
.ticket-card :deep(.el-table .invalid-row:hover > td) { background: var(--vault-panel); }
.ai-eval {
  display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  font-size: 12px; color: var(--vault-text-2);
}
.ticket-detail { display: flex; flex-direction: column; gap: 10px; font-size: 14px; color: var(--vault-text); }
.detail-item label { color: var(--vault-text-3); margin-right: 6px; }
.detail-item.reply { background: rgba(212,176,106,0.06); border: 1px solid var(--vault-line); padding: 10px 12px; border-radius: 8px; }
.idcard-card { margin-bottom: 16px; border-color: var(--vault-line); }
.idcard-tip { font-size: 12px; color: var(--vault-text-3); }
.idcard-form { max-width: 520px; padding-top: 6px; }
.idcard-upload-row { display: flex; align-items: center; gap: 12px; }
.idcard-preview { width: 64px; height: 64px; border-radius: 8px; border: 1px solid var(--vault-line); }
</style>
