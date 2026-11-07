import { computed, ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark'

const STORAGE_KEY = 'smartbank-theme'

/**
 * Resolve the initial theme.
 * Priority: persisted user choice -> OS preference -> light.
 */
function resolveInitialTheme(): ThemeMode {
  if (typeof window === 'undefined') return 'light'

  const saved = window.localStorage.getItem(STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') return saved

  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

// Module-level singleton so every component shares one source of truth.
const theme = ref<ThemeMode>(resolveInitialTheme())

function applyTheme(value: ThemeMode) {
  if (typeof document === 'undefined') return
  document.documentElement.classList.toggle('dark', value === 'dark')
  document.documentElement.style.colorScheme = value
}

applyTheme(theme.value)

watch(theme, (value) => {
  applyTheme(value)
  try {
    window.localStorage.setItem(STORAGE_KEY, value)
  } catch {
    /* storage disabled (private mode) - theme still applies for this session */
  }
})

export function useTheme() {
  const isDark = computed(() => theme.value === 'dark')

  function setTheme(value: ThemeMode) {
    theme.value = value
  }

  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  return { theme, isDark, setTheme, toggleTheme }
}
