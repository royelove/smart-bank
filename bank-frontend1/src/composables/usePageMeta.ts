import { computed } from 'vue'
import { useRoute } from 'vue-router'

/**
 * Reads the current route's page meta.
 *
 * The router is the single source of truth for a page's title, so both the
 * topbar breadcrumb and the page's own <PageHeader> read from here instead of
 * duplicating literals in every view.
 */
export function usePageMeta() {
  const route = useRoute()

  const title = computed(() => (route.meta.title as string | undefined) ?? '')
  const subtitle = computed(() => (route.meta.subtitle as string | undefined) ?? '')
  const parent = computed(() => (route.meta.parent as string | undefined) ?? '')

  return { title, subtitle, parent }
}
