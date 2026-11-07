<template>
  <div class="kline-chart">
    <div class="kline-toolbar">
      <div class="kline-title">
        {{ stockName }} K线图
        <span class="kline-sub">{{ stockCode }}</span>
      </div>
      <div class="kline-periods">
        <el-radio-group v-model="period" size="small" @change="loadKline">
          <el-radio-button value="1min">1分</el-radio-button>
          <el-radio-button value="5min">5分</el-radio-button>
          <el-radio-button value="1day">日K</el-radio-button>
          <el-radio-button value="1week">周K</el-radio-button>
          <el-radio-button value="1month">月K</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div v-if="loading && klineData.length === 0" class="loading-wrap">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else-if="klineData.length === 0" class="empty-wrap">
      <el-empty description="暂无K线数据" />
    </div>

    <div v-else ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getKline } from '@/api/stock'

const props = defineProps({
  stockCode: { type: String, required: true },
  stockName: { type: String, default: '' }
})

const period = ref('1day')
const klineData = ref([])
const loading = ref(false)
let timer = null
let chartInstance = null
const chartRef = ref(null)

const loadKline = async () => {
  loading.value = true
  try {
    const res = await getKline(props.stockCode, period.value)
    klineData.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
    nextTick(() => {
      renderChart()
    })
  }
}

const renderChart = () => {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const data = klineData.value
  const ohlcData = data.map(d => [
    d.openPrice, d.closePrice, d.lowPrice, d.highPrice
  ])

  // 横轴标签：分钟级用「时:分」，日/周/月级用「年-月-日」。
  // 日/周/月线的 klineTime 都是当天 00:00，统一按分钟格式化会得到一排相同的 00:00。
  const pad2 = (n) => n.toString().padStart(2, '0')
  const isIntraday = period.value === '1min' || period.value === '5min'
  const dates = data.map(d => {
    const t = new Date(d.klineTime)
    if (Number.isNaN(t.getTime())) return d.klineTime || ''
    return isIntraday
      ? `${pad2(t.getHours())}:${pad2(t.getMinutes())}`
      : `${t.getFullYear()}-${pad2(t.getMonth() + 1)}-${pad2(t.getDate())}`
  })

  const colors = data.map(d => {
    if (!d.openPrice || !d.closePrice) return '#5470c6'
    return d.closePrice >= d.openPrice ? '#e74c3c' : '#2ecc71'
  })

  const option = {
    title: { text: '' },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params) => {
        const d = data[params[0].dataIndex]
        if (!d) return ''
        return `
          <div style="font-family: monospace; font-size: 13px;">
            <b>${d.stockName}(${d.stockCode})</b><br/>
            开盘: ¥${Number(d.openPrice).toFixed(2)}<br/>
            收盘: ¥${Number(d.closePrice).toFixed(2)}<br/>
            最高: ¥${Number(d.highPrice).toFixed(2)}<br/>
            最低: ¥${Number(d.lowPrice).toFixed(2)}<br/>
            成交量: ${Number(d.volume).toLocaleString()}手<br/>
            成交额: ¥${Number(d.amount).toFixed(2)}
          </div>
        `
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      scale: true,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      axisLabel: { color: '#999', fontSize: 10, rotate: 30 }
    },
    yAxis: {
      type: 'value',
      scale: true,
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      axisLabel: { color: '#999', fontSize: 10 },
      splitArea: { show: true, areaStyle: { color: ['rgba(0,0,0,0.02)', 'rgba(0,0,0,0.05)'] } }
    },
    dataZoom: [
      { type: 'inside', start: 0, end: 100 },
      { type: 'slider', start: 0, end: 100, height: 20, bottom: 5, borderColor: '#ccc' }
    ],
    series: [
      {
        type: 'candlestick',
        data: ohlcData,
        itemStyle: {
          color: '#e74c3c',
          color0: '#2ecc71',
          borderColor: '#e74c3c',
          borderColor0: '#2ecc71',
          borderWidth: 1
        },
        encode: {
          x: 0,
          y: [1, 0, 3, 2]
        }
      },
      {
        type: 'line',
        data: dates.map((_, i) => {
          const d = data[i]
          if (!d || !d.openPrice) return null
          return (d.openPrice + d.closePrice) / 2
        }),
        smooth: true,
        lineStyle: { color: '#5470c6', width: 1, opacity: 0.5 },
        tooltip: { show: false },
        silent: true
      }
    ]
  }

  chartInstance.setOption(option, true)
}

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  loadKline()
  timer = setInterval(loadKline, 10000)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.kline-chart {
  padding: var(--space-1) 0;
}

.kline-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.kline-title {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-foreground);
  letter-spacing: 0.06em;
  font-family: var(--font-sans);
}

.kline-sub {
  font-size: var(--font-base);
  color: var(--color-muted-foreground);
  margin-left: var(--space-2);
  font-weight: 400;
  font-family: var(--font-mono);
}

.kline-periods .el-radio-button {
  width: 70px;
}

.loading-wrap {
  padding: var(--space-8);
}

.empty-wrap {
  padding: var(--space-16) 0;
}

.chart-container {
  width: 100%;
  height: 500px;
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
}
</style>