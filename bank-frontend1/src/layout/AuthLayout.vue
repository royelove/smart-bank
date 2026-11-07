<script setup lang="ts">
/**
 * AuthLayout — the shared shell for every auth route.
 *
 * Split screen on `lg+`: a brand/marketing panel on the left and a centered
 * `max-w-md` form column on the right. Below `lg` the brand panel collapses
 * into a compact header above the form, so the page stays a single column.
 *
 * Usage:
 *   <AuthLayout title="手机号登录" subtitle="...">
 *     <form>…</form>
 *     <template #footer>cross links</template>
 *   </AuthLayout>
 */
import { Clock, CreditCard, Percent, ShieldCheck } from 'lucide-vue-next'

interface AuthLayoutProps {
  /** Page heading rendered above the default slot. */
  title: string
  /** Optional supporting line under the heading. */
  subtitle?: string
  /** Marketing headline of the brand panel. */
  panelTitle?: string
  /** Marketing paragraph of the brand panel. */
  panelDescription?: string
}

withDefaults(defineProps<AuthLayoutProps>(), {
  subtitle: '',
  panelTitle: '智慧银行，让财富增值更简单',
  panelDescription: '安全、便捷、智能的数字化银行服务。随时随地管理账户、投资理财、办理业务。',
})

const highlights = [
  { icon: ShieldCheck, value: '256-bit', label: '银行级加密' },
  { icon: Clock, value: '7×24h', label: '全天候服务' },
  { icon: Percent, value: '0元', label: '转账手续费' },
]
</script>

<template>
  <div class="flex min-h-screen flex-col bg-white dark:bg-navy-950 lg:flex-row">
    <!-- Brand panel (desktop only). Kept achromatic so the form column owns the
         accent; a full-bleed blue gradient made the login form read as an
         afterthought. -->
    <aside
      class="relative hidden bg-navy-50 p-12 dark:bg-navy-900 lg:flex lg:w-1/2 lg:flex-col lg:justify-between lg:border-r lg:border-navy-200 lg:dark:border-navy-800"
    >
      <div class="flex items-center gap-3">
        <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-600 text-white">
          <CreditCard class="h-5 w-5" />
        </div>
        <div>
          <p class="text-heading-sm font-semibold text-navy-900 dark:text-navy-50">
            SmartBank
          </p>
          <p class="text-body-xs text-navy-500 dark:text-navy-400">
            智慧银行
          </p>
        </div>
      </div>

      <div class="max-w-md">
        <h2 class="text-display-md text-balance text-navy-900 dark:text-navy-50">
          {{ panelTitle }}
        </h2>
        <p class="mt-6 text-body-lg text-navy-600 dark:text-navy-300">
          {{ panelDescription }}
        </p>
        <dl class="mt-10 grid grid-cols-3 gap-3 text-center">
          <div
            v-for="item in highlights"
            :key="item.label"
            class="rounded-xl border border-navy-200 bg-white p-4 dark:border-navy-700 dark:bg-navy-800/60"
          >
            <component
              :is="item.icon"
              class="mx-auto h-5 w-5 text-primary-500"
            />
            <dt class="mt-2 font-mono text-2xl font-bold text-navy-900 dark:text-navy-50">
              {{ item.value }}
            </dt>
            <dd class="mt-1 text-body-xs text-navy-500 dark:text-navy-400">
              {{ item.label }}
            </dd>
          </div>
        </dl>
      </div>

      <div class="flex items-center gap-3 text-body-sm text-navy-500 dark:text-navy-400">
        <span class="inline-flex h-2 w-2 rounded-full bg-success-500" />
        <span>系统运行正常</span>
        <span class="text-navy-300 dark:text-navy-600">|</span>
        <span>存款保险保障</span>
      </div>
    </aside>

    <!-- Form column -->
    <main class="flex flex-1 flex-col">
      <!-- Compact brand header (mobile / tablet) -->
      <header
        class="flex items-center gap-3 border-b border-navy-200 bg-white px-5 py-4 dark:border-navy-800 dark:bg-navy-900 lg:hidden"
      >
        <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-600">
          <CreditCard class="h-5 w-5 text-white" />
        </div>
        <div class="min-w-0">
          <p class="text-heading-sm text-navy-900 dark:text-navy-50">
            SmartBank
          </p>
          <p class="text-body-xs text-navy-500 dark:text-navy-400">
            安全、便捷的智慧银行服务
          </p>
        </div>
      </header>

      <div class="flex flex-1 items-center justify-center px-4 py-10 sm:px-6 lg:px-12">
        <div class="w-full max-w-md animate-fade-in">
          <div class="mb-8">
            <h1 class="text-heading-xl text-navy-900 dark:text-navy-50">
              {{ title }}
            </h1>
            <p
              v-if="subtitle"
              class="mt-2 text-body-md text-navy-500 dark:text-navy-400"
            >
              {{ subtitle }}
            </p>
          </div>

          <slot />

          <div
            v-if="$slots.footer"
            class="mt-8 border-t border-navy-200 pt-6 dark:border-navy-800"
          >
            <slot name="footer" />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
