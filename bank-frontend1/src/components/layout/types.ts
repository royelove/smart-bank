import type { Component } from 'vue'

export interface NavItem {
  /** Label shown in the sidebar (hidden when the rail is collapsed). */
  label: string
  /** Router target. */
  to: string
  icon: Component
  /** Match the route exactly instead of prefix-matching (needed for nested shells). */
  exact?: boolean
  /** Optional right-aligned text badge, e.g. an unread count. */
  badge?: string | number
}

export interface NavSection {
  title: string
  items: NavItem[]
}

/** One entry in a <SectionNav> rail. `id` may be a string key or a numeric
 *  enum value (e.g. a ticket status) that drives a filter. */
export interface SectionNavItem<T extends string | number = string> {
  id: T
  label: string
  icon?: Component
  /** Optional right-aligned count. */
  count?: number
}
