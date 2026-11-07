<script setup lang="ts">
/**
 * 股票 K 线（蜡烛图）组件。
 *
 * 纯 SVG 绘制，不依赖任何图表库：新前端的设计哲学是「不引入重型库」，
 * 而 chart.js 核心并不支持蜡烛图，所以这里直接按像素画。
 *
 * 涨跌配色遵循 A 股习惯：红涨绿跌。
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { getKline } from '@/api/stock'
import { toNumber } from '@/utils/format'
import type { StockKline } from '@/types'

const props = defineProps<{
  stockCode: string
  stockName?: string
}>()

const PERIODS = [
  { value: '1min', label: '1分' },
  { value: '5min', label: '5分' },
  { value: '1day', label: '日K' },
  { value: '1week', label: '周K' },
  { value: '1month', label: '月K' },
]

const UP_COLOR = '#ef4444'
const DOWN_COLOR = '#22c55e'

/* ---------------------------------- data ---------------------------------- */

const period = ref('1day')
const bars = ref<StockKline[]>([])
const loading = ref(false)
const error = ref('')
let timer: ReturnType<typeof setInterval> | undefined

async function load() {
  if (!props.stockCode) return
  loading.value = true
  try {
    const data = await getKline(props.stockCode, period.value)
    bars.value = Array.isArray(data) ? data : []
    error.value = ''
  } catch (e) {
    error.value = (e as Error).message || 'K线数据加载失败'
    bars.value = []
  } finally {
    loading.value = false
  }
}

function setPeriod(value: string) {
  if (period.value === value) return
  period.value = value
  load()
}

/* -------------------------------- geometry -------------------------------- */

const CHART_HEIGHT = 420
const PRICE_TOP = 14
const PRICE_HEIGHT = 282
const GAP = 16
const VOLUME_HEIGHT = 74
const PAD_LEFT = 8
const PAD_RIGHT = 62

const volumeTop = PRICE_TOP + PRICE_HEIGHT + GAP

const wrapRef = ref<HTMLElement | null>(null)
const chartWidth = ref(880)
let resizeObserver: ResizeObserver | null = null

const plotLeft = PAD_LEFT
const plotRight = computed(() => Math.max(plotLeft + 40, chartWidth.value - PAD_RIGHT))
const plotWidth = computed(() => plotRight.value - plotLeft)

function validPrices(): { lows: number[]; highs: number[] } {
  const lows: number[] = []
  const highs: number[] = []
  for (const bar of bars.value) {
    const low = toNumber(bar.lowPrice)
    const high = toNumber(bar.highPrice)
    if (low > 0) lows.push(low)
    if (high > 0) highs.push(high)
  }
  return { lows, highs }
}

const priceMin = computed(() => {
  const { lows, highs } = validPrices()
  if (!lows.length) return 0
  const min = Math.min(...lows)
  const max = Math.max(...highs)
  const pad = (max - min) * 0.08 || max * 0.02 || 1
  return Math.max(0, min - pad)
})

const priceMax = computed(() => {
  const { lows, highs } = validPrices()
  if (!highs.length) return 1
  const min = Math.min(...lows)
  const max = Math.max(...highs)
  const pad = (max - min) * 0.08 || max * 0.02 || 1
  return max + pad
})

const volumeMax = computed(() => {
  const max = bars.value.reduce((acc, bar) => Math.max(acc, toNumber(bar.volume)), 0)
  return max > 0 ? max : 1
})

const step = computed(() => (bars.value.length ? plotWidth.value / bars.value.length : 0))
const candleWidth = computed(() => Math.max(1.5, Math.min(14, step.value * 0.62)))

function xOf(index: number): number {
  return plotLeft + step.value * (index + 0.5)
}

function yPrice(price: number): number {
  const range = priceMax.value - priceMin.value || 1
  return PRICE_TOP + ((priceMax.value - price) / range) * PRICE_HEIGHT
}

function yVolume(volume: number): number {
  return volumeTop + VOLUME_HEIGHT - (volume / volumeMax.value) * VOLUME_HEIGHT
}

const priceTicks = computed(() => {
  const ticks: { y: number; label: string }[] = []
  const count = 4
  for (let i = 0; i <= count; i++) {
    const price = priceMax.value - ((priceMax.value - priceMin.value) * i) / count
    ticks.push({ y: yPrice(price), label: price.toFixed(2) })
  }
  return ticks
})

interface Candle {
  index: number
  x: number
  wickTop: number
  wickBottom: number
  bodyY: number
  bodyH: number
  color: string
  volumeY: number
  volumeH: number
  up: boolean
}

const candles = computed<Candle[]>(() =>
  bars.value.map((bar, index) => {
    const open = toNumber(bar.openPrice)
    const close = toNumber(bar.closePrice)
    const up = close >= open
    const top = yPrice(Math.max(open, close))
    const bottom = yPrice(Math.min(open, close))
    const volumeY = yVolume(toNumber(bar.volume))
    return {
      index,
      x: xOf(index),
      wickTop: yPrice(toNumber(bar.highPrice)),
      wickBottom: yPrice(toNumber(bar.lowPrice)),
      bodyY: top,
      bodyH: Math.max(1, bottom - top),
      color: up ? UP_COLOR : DOWN_COLOR,
      volumeY,
      volumeH: volumeTop + VOLUME_HEIGHT - volumeY,
      up,
    }
  }),
)

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

function parseTime(raw: string): Date | null {
  const date = new Date(String(raw).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

function formatAxisTime(raw: string): string {
  const date = parseTime(raw)
  if (!date) return raw || ''
  if (period.value === '1min' || period.value === '5min') {
    return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
  }
  if (period.value === '1month') {
    return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}`
  }
  return `${pad2(date.getMonth() + 1)}/${pad2(date.getDate())}`
}

function formatFullTime(raw: string): string {
  const date = parseTime(raw)
  if (!date) return raw || ''
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

const xLabels = computed(() => {
  if (!bars.value.length) return []
  const target = Math.min(7, bars.value.length)
  const gap = Math.max(1, Math.floor(bars.value.length / target))
  const out: { x: number; label: string }[] = []
  for (let i = 0; i < bars.value.length; i += gap) {
    out.push({ x: xOf(i), label: formatAxisTime(bars.value[i].klineTime) })
  }
  const lastIndex = bars.value.length - 1
  if (!out.length || out[out.length - 1].x < xOf(lastIndex) - candleWidth.value * 2) {
    out.push({ x: xOf(lastIndex), label: formatAxisTime(bars.value[lastIndex].klineTime) })
  }
  return out
})

/* -------------------------------- hover/tip ------------------------------- */

const hoverIndex = ref(-1)

const hoveredBar = computed(() => (hoverIndex.value >= 0 ? bars.value[hoverIndex.value] ?? null : null))
const hoveredCandle = computed(() =>
  hoverIndex.value >= 0 ? candles.value[hoverIndex.value] ?? null : null,
)

function handleMouseMove(event: MouseEvent) {
  if (!bars.value.length || !wrapRef.value) return
  const rect = wrapRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  if (x < plotLeft || x > plotRight.value) {
    hoverIndex.value = -1
    return
  }
  hoverIndex.value = Math.min(
    bars.value.length - 1,
    Math.max(0, Math.floor((x - plotLeft) / step.value)),
  )
}

function handleMouseLeave() {
  hoverIndex.value = -1
}

const tooltipStyle = computed(() => {
  const candle = hoveredCandle.value
  if (!candle) return {}
  const flip = candle.x > plotRight.value - 180
  const left = flip ? candle.x - 172 : candle.x + 14
  const top = Math.max(4, Math.min(candle.wickTop - 12, CHART_HEIGHT - 150))
  return { left: `${left}px`, top: `${top}px` }
})

/* -------------------------------- summary -------------------------------- */

const lastBar = computed(() => bars.value[bars.value.length - 1] ?? null)

const lastChangePercent = computed(() => {
  if (bars.value.length < 2) return 0
  const prev = toNumber(bars.value[bars.value.length - 2].closePrice)
  const current = toNumber(bars.value[bars.value.length - 1].closePrice)
  if (!prev) return 0
  return ((current - prev) / prev) * 100
})

const hasSimulated = computed(() => bars.value.some((bar) => bar.source === 'simulated'))

function sourceLabel(source: string): string {
  switch (source) {
    case 'trade': return '真实成交'
    case 'quote': return '实时行情'
    case 'remote': return '历史行情'
    case 'simulated': return '演示数据'
    default: return source || '—'
  }
}

/* ------------------------------- lifecycle ------------------------------- */

onMounted(() => {
  load()
  timer = setInterval(load, 15000)
  resizeObserver = new ResizeObserver((entries) => {
    const width = entries[0]?.contentRect.width
    if (width) chartWidth.value = Math.max(360, width)
  })
  if (wrapRef.value) resizeObserver.observe(wrapRef.value)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  resizeObserver?.disconnect()
})

watch(() => props.stockCode, load)

defineExpose({ refresh: load })
</script>

<template>
  <div class="kline">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div class="min-w-0">
        <p class="truncate font-medium text-navy-900 dark:text-navy-50">
          {{ stockName || stockCode }}
        </p>
        <p class="font-mono text-xs text-navy-500 dark:text-navy-400">
          {{ stockCode }}
          <template v-if="lastBar">
            <span> · 最新 {{ toNumber(lastBar.closePrice).toFixed(2) }}</span>
            <span :style="{ color: lastChangePercent >= 0 ? UP_COLOR : DOWN_COLOR }">
              {{ lastChangePercent >= 0 ? '+' : '' }}{{ lastChangePercent.toFixed(2) }}%
            </span>
          </template>
        </p>
      </div>

      <div class="flex items-center gap-2">
        <div class="inline-flex rounded-lg border border-navy-200 p-0.5 dark:border-navy-700">
          <button
            v-for="item in PERIODS"
            :key="item.value"
            type="button"
            class="rounded-md px-2.5 py-1 text-xs font-medium transition-colors"
            :class="period === item.value
              ? 'bg-primary-600 text-white'
              : 'text-navy-500 hover:text-navy-900 dark:text-navy-400 dark:hover:text-navy-100'"
            @click="setPeriod(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
        <button
          type="button"
          class="btn-ghost btn-icon rounded-lg p-1.5"
          title="刷新"
          @click="load"
        >
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
        </button>
      </div>
    </div>

    <p
      v-if="hasSimulated"
      class="mt-2 text-xs text-warning-600 dark:text-warning-400"
    >
      含演示补齐的历史数据（source=simulated），真实成交与实时行情会覆盖对应的柱子。
    </p>

    <div
      v-if="error"
      class="mt-4 rounded-lg bg-danger-50 px-4 py-3 text-sm text-danger-700 dark:bg-danger-500/10 dark:text-danger-400"
      role="alert"
    >
      {{ error }}
    </div>

    <div
      v-else-if="loading && !bars.length"
      class="flex h-[420px] items-center justify-center text-sm text-navy-400 dark:text-navy-500"
    >
      正在加载 K 线…
    </div>

    <div
      v-else-if="!bars.length"
      class="flex h-[420px] items-center justify-center text-sm text-navy-400 dark:text-navy-500"
    >
      暂无 K 线数据
    </div>

    <div
      v-else
      ref="wrapRef"
      class="relative mt-3 w-full"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
    >
      <svg
        :width="chartWidth"
        :height="CHART_HEIGHT"
        :viewBox="`0 0 ${chartWidth} ${CHART_HEIGHT}`"
      >
        <!-- 价格网格 -->
        <g>
          <line
            v-for="tick in priceTicks"
            :key="`grid-${tick.y}`"
            :x1="plotLeft"
            :x2="plotRight"
            :y1="tick.y"
            :y2="tick.y"
            class="text-navy-200 dark:text-navy-700"
            stroke="currentColor"
            stroke-width="1"
            stroke-dasharray="3 4"
          />
          <text
            v-for="tick in priceTicks"
            :key="`tick-${tick.y}`"
            :x="plotRight + 6"
            :y="tick.y + 3"
            class="fill-navy-400 dark:fill-navy-500"
            style="font-size: 10px"
          >
            {{ tick.label }}
          </text>
        </g>

        <!-- 成交量基线 -->
        <line
          :x1="plotLeft"
          :x2="plotRight"
          :y1="volumeTop + VOLUME_HEIGHT"
          :y2="volumeTop + VOLUME_HEIGHT"
          class="text-navy-200 dark:text-navy-700"
          stroke="currentColor"
          stroke-width="1"
        />
        <text
          :x="plotLeft + 2"
          :y="volumeTop + 10"
          class="fill-navy-400 dark:fill-navy-500"
          style="font-size: 10px"
        >
          成交量
        </text>

        <!-- 蜡烛 -->
        <g v-for="candle in candles" :key="candle.index">
          <line
            :x1="candle.x"
            :x2="candle.x"
            :y1="candle.wickTop"
            :y2="candle.wickBottom"
            :stroke="candle.color"
            stroke-width="1"
          />
          <rect
            :x="candle.x - candleWidth / 2"
            :y="candle.bodyY"
            :width="candleWidth"
            :height="candle.bodyH"
            :fill="candle.color"
          />
        </g>

        <!-- 成交量柱 -->
        <rect
          v-for="candle in candles"
          :key="`vol-${candle.index}`"
          :x="candle.x - candleWidth / 2"
          :y="candle.volumeY"
          :width="candleWidth"
          :height="candle.volumeH"
          :fill="candle.color"
          opacity="0.45"
        />

        <!-- 十字光标 -->
        <line
          v-if="hoveredCandle"
          :x1="hoveredCandle.x"
          :x2="hoveredCandle.x"
          :y1="PRICE_TOP"
          :y2="volumeTop + VOLUME_HEIGHT"
          class="text-navy-300 dark:text-navy-600"
          stroke="currentColor"
          stroke-width="1"
          stroke-dasharray="4 3"
        />

        <!-- 时间轴 -->
        <text
          v-for="label in xLabels"
          :key="`x-${label.x}`"
          :x="label.x"
          :y="CHART_HEIGHT - 8"
          text-anchor="middle"
          class="fill-navy-400 dark:fill-navy-500"
          style="font-size: 10px"
        >
          {{ label.label }}
        </text>
      </svg>

      <!-- 悬浮提示 -->
      <div
        v-if="hoveredBar"
        class="pointer-events-none absolute w-40 rounded-lg border border-navy-200 bg-white/95 px-3 py-2 text-xs shadow-md dark:border-navy-700 dark:bg-navy-800/95"
        :style="tooltipStyle"
      >
        <div class="mb-1 flex items-center justify-between gap-2">
          <span class="font-mono text-navy-500 dark:text-navy-400">
            {{ formatFullTime(hoveredBar.klineTime) }}
          </span>
        </div>
        <div class="flex items-center justify-between gap-2 text-navy-500 dark:text-navy-400">
          <span>来源</span>
          <span>{{ sourceLabel(hoveredBar.source) }}</span>
        </div>
        <div class="mt-1 space-y-0.5 font-mono">
          <div class="flex justify-between">
            <span class="text-navy-500 dark:text-navy-400">开</span>
            <span :style="{ color: hoveredCandle?.color }">{{ toNumber(hoveredBar.openPrice).toFixed(2) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-navy-500 dark:text-navy-400">高</span>
            <span :style="{ color: hoveredCandle?.color }">{{ toNumber(hoveredBar.highPrice).toFixed(2) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-navy-500 dark:text-navy-400">低</span>
            <span :style="{ color: hoveredCandle?.color }">{{ toNumber(hoveredBar.lowPrice).toFixed(2) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-navy-500 dark:text-navy-400">收</span>
            <span :style="{ color: hoveredCandle?.color }">{{ toNumber(hoveredBar.closePrice).toFixed(2) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-navy-500 dark:text-navy-400">量</span>
            <span>{{ toNumber(hoveredBar.volume).toLocaleString() }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-navy-500 dark:text-navy-400">额</span>
            <span>{{ toNumber(hoveredBar.amount).toLocaleString() }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
