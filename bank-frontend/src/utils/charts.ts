/**
 * Single place where Chart.js is configured.
 *
 * Registering the controllers here (instead of per page) keeps the bundle
 * tree-shakeable and means a page only imports the chart types it renders.
 */
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  DoughnutController,
  Filler,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js'

ChartJS.register(
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  DoughnutController,
  Filler,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
)

ChartJS.defaults.font.family =
  "'IBM Plex Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
ChartJS.defaults.color = '#7b8794'

/**
 * Categorical palette. `CHART_COLORS[0]` is the brand colour, so it must stay in
 * sync with `tailwind.config.js` -> `primary[600]`; `CHART_PRIMARY_FILL` is the
 * same colour at 12% alpha for `fill: true` line series.
 */
export const CHART_COLORS = [
  '#2563eb',
  '#0ea5e9',
  '#10b981',
  '#8b5cf6',
  '#f43f5e',
  '#64748b',
]

export const CHART_PRIMARY_FILL = 'rgba(37, 99, 235, 0.12)'

export { Bar as BarChart, Doughnut as DoughnutChart, Line as LineChart } from 'vue-chartjs'
export { Chart as ChartJS }
