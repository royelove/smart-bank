<template>
  <router-view />
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Noto+Sans+SC:wght@300;400;500;600;700;900&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  --color-background: var(--color-background);
  --color-foreground: var(--color-foreground);
  background:
    radial-gradient(ellipse 80% 50% at 20% 20%, rgba(245, 158, 11, 0.02) 0%, transparent 50%),
    radial-gradient(ellipse 60% 40% at 80% 20%, rgba(139, 92, 246, 0.02) 0%, transparent 50%),
    linear-gradient(180deg, var(--color-background) 0%, #F8FAFC 100%);
}

body::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.5' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.003'/%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 9999;
}

:root {
  --color-primary: #C084D8;
  --color-on-primary: #1E293B;
  --color-secondary: #A855F7;
  --color-on-secondary: #1E293B;

  --color-accent: #F472B6;
  --color-on-accent: #1E293B;

  --color-background: #F8FAFC;
  --color-foreground: #1E293B;
  --color-card: #FFFFFF;
  --color-card-hover: #F1F5F9;
  --color-card-foreground: #1E293B;
  --color-muted: #64748B;
  --color-muted-foreground: #98A2B3;

  --color-border: #94A3B8;
  --color-border-hover: #CBD5E1;

  --color-destructive: #F87171;
  --color-on-destructive: #FFFFFF;
  --color-success: #34D399;
  --color-warning: #FBBF24;
  --color-info: #60A5FA;
  --color-success-soft: #10B981;
  --color-warning-soft: #F8EA3F;

  --color-up: #34D399;
  --color-up-soft: #A3E4D7;
  --color-down: #F87171;
  --color-down-soft: #FEC89A;
  --color-flat: #98A2B3;

  --color-ring: #A855F7;
  --color-ring-subtle: rgba(192, 132, 216, 0.1);

  --font-sans: 'Inter', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', monospace;

  --space-1: 6px;
  --space-2: 12px;
  --space-3: 16px;
  --space-4: 24px;
  --space-5: 32px;
  --space-6: 40px;
  --space-7: 48px;
  --space-8: 56px;
  --space-9: 64px;
  --space-10: 72px;
  --space-11: 80px;
  --space-12: 96px;
  --space-14: 112px;
  --space-16: 128px;

  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 20px;
  --radius-xl: 28px;
  --radius-2xl: 36px;
  --radius-full: 9999px;

  --shadow-sm: 0 1px 3px 0 rgba(0, 0, 0, 0.05), 0 1px 2px -1px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 12px -2px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 12px 28px -4px rgba(0, 0, 0, 0.15), 0 4px 12px -4px rgba(0, 0, 0, 0.15);
  --shadow-xl: 0 20px 40px -6px rgba(0, 0, 0, 0.2), 0 8px 16px -4px rgba(0, 0, 0, 0.2);
  --shadow-2xl: 0 32px 64px -8px rgba(0, 0, 0, 0.25);

  --glass-bg: rgba(255, 255, 255, 0.7);
  --glass-border: rgba(148, 163, 184, 0.5);
  --glass-blur: 36px;

  --transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-normal: 280ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: 380ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-spring: 450ms cubic-bezier(0.34, 1.56, 0.64, 1);
}

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: var(--color-muted); border-radius: var(--radius-full); }
::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: var(--radius-full);
  transition: background var(--transition-fast);
}
::-webkit-scrollbar-thumb:hover { background: var(--color-border-hover); }
::selection {
  background: rgba(245, 158, 11, 0.25);
  color: var(--color-foreground);
}

/* ============================================================
   全局组件样式
   ============================================================ */

/* ── Card Component ── */
.card {
  position: relative;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  transition: all var(--transition-normal);
}

.card:hover {
  border-color: var(--color-border-hover);
  box-shadow: var(--shadow-lg), var(--shadow-glow-primary);
  transform: translateY(-2px);
}

/* ── Glass Card ── */
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
}
/* ── Utility Classes ── */
.text-primary { color: var(--color-primary); }
.text-on-primary { color: var(--color-on-primary); }
.text-accent { color: var(--color-accent); }
.text-on-accent { color: var(--color-on-accent); }
.text-muted { color: var(--color-muted-foreground); }
.text-success { color: var(--color-success); }
.text-danger { color: var(--color-destructive); }
.text-up { color: var(--color-up); }
.text-down { color: var(--color-down); }

.bg-card { background: var(--color-card); }
.bg-muted { background: var(--color-muted); }
.bg-surface { background: var(--color-card); }

.font-mono { font-family: var(--font-mono); }
.font-sans { font-family: var(--font-sans); }

/* ── Button Base ── */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  font-weight: 600;
  font-size: 13px;
  line-height: 1.5;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all var(--transition-fast);
  outline: none;
  letter-spacing: 0.02em;
}

.btn:focus-visible {
  outline: 2px solid var(--color-ring);
  outline-offset: 2px;
}

.btn-primary {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  color: var(--color-on-primary);
  border-color: transparent;
  box-shadow: var(--shadow-glow-primary);
}

.btn-primary:hover {
  box-shadow: 0 0 30px rgba(192, 132, 216, 0.35);
  transform: translateY(-1px);
}

.btn-accent {
  background: linear-gradient(135deg, var(--color-accent) 0%, #F472B6 100%);
  color: var(--color-on-accent);
  border-color: transparent;
}

.btn-outline {
  background: transparent;
  color: var(--color-foreground);
  border-color: var(--color-border);
}

.btn-outline:hover {
  background: var(--color-muted);
  border-color: var(--color-border-hover);
}

.btn-ghost {
  background: transparent;
  color: var(--color-muted-foreground);
}

.btn-ghost:hover {
  background: var(--color-muted);
  color: var(--color-foreground);
}

/* ── Input Base ── */
.input {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  background: var(--color-muted);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-foreground);
  font-size: 14px;
  font-family: var(--font-sans);
  transition: all var(--transition-fast);
  outline: none;
}

.input:focus {
  border-color: var(--color-ring);
  box-shadow: 0 0 0 3px rgba(192, 132, 212, 0.12), 0 0 0 1px var(--color-ring);
}

.input::placeholder {
  color: var(--color-muted-foreground);
}

/* ── Badge ── */
.badge {
  display: inline-flex;
  align-items: center;
  padding: var(--space-1) var(--space-3);
  font-size: 11px;
  font-weight: 600;
  border-radius: var(--radius-full);
  letter-spacing: 0.03em;
}

.badge-primary {
  background: rgba(192, 132, 216, 0.12);
  color: var(--color-primary);
  border: 1px solid rgba(192, 132, 216, 0.2);
}

.badge-accent {
  background: rgba(244, 114, 182, 0.12);
  color: var(--color-accent);
  border: 1px solid rgba(244, 114, 182, 0.2);
}

.badge-success {
  background: rgba(52, 211, 153, 0.12);
  color: var(--color-success);
  border: 1px solid rgba(52, 211, 153, 0.2);
}

.badge-danger {
  background: rgba(248, 113, 113, 0.12);
  color: var(--color-destructive);
  border: 1px solid rgba(248, 113, 113, 0.2);
}

/* ── Animations ── */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-24px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

@keyframes glow {
  0%, 100% { box-shadow: var(--shadow-glow-primary); }
  50% { box-shadow: 0 0 48px rgba(192, 132, 216, 0.25); }
}

.animate-fadeIn { animation: fadeIn var(--transition-normal); }
.animate-slideUp { animation: slideUp var(--transition-slow); }
.animate-slideDown { animation: slideDown var(--transition-slow); }
.animate-pulse { animation: pulse 3s infinite; }
.animate-glow { animation: glow 3s infinite; }

/* ── Responsive Container ── */
.container {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--space-6);
}

@media (min-width: 640px) {
  .container { padding: 0 var(--space-8); }
}

@media (min-width: 1024px) {
  .container { padding: 0 var(--space-10); }
}

/* ============================================================
    Element Plus 深度定制 · Minimalism Style
    ============================================================ */

/* ── Table ── */
.el-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: var(--color-muted);
  --el-table-row-hover-bg-color: rgba(192, 132, 216, 0.04);
  --el-table-border-color: var(--color-border);
  --el-table-text-color: var(--color-foreground);
  --el-table-header-text-color: var(--color-muted-foreground);
  background: transparent;
  font-size: 14px;
}

.el-table th.el-table__cell {
  background: var(--color-muted) !important;
  border-bottom: 1px solid var(--color-border) !important;
  font-weight: 700;
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.el-table td.el-table__cell {
  border-bottom: 1px solid var(--color-border) !important;
}

.el-table .el-table__inner-wrapper::before { display: none; }

.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background: rgba(30, 41, 59, 0.5) !important;
}

/* ── Button ── */
.el-button {
  border-radius: var(--radius-md);
  font-weight: 600;
  letter-spacing: 0.03em;
  transition: all var(--transition-fast);
  font-family: var(--font-sans);
}

.el-button--primary {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  border-color: transparent;
  color: var(--color-on-primary);
  box-shadow: var(--shadow-glow-primary);
}

.el-button--primary:hover {
  box-shadow: 0 0 32px rgba(192, 132, 216, 0.35);
  transform: translateY(-1px);
}

.el-button--danger {
  background: rgba(248, 113, 113, 0.08);
  border: 1px solid rgba(248, 113, 113, 0.25);
  color: var(--color-destructive);
}

.el-button--danger:hover {
  background: rgba(248, 113, 113, 0.15);
  border-color: rgba(248, 113, 113, 0.4);
}

.el-button--default {
  background: var(--color-muted);
  border-color: var(--color-border);
  color: var(--color-foreground);
}

.el-button--default:hover {
  background: var(--color-border);
  border-color: var(--color-border-hover);
}

/* ── Input ── */
.el-input__wrapper, .el-select__wrapper {
  background: var(--color-muted);
  box-shadow: 0 0 0 1px var(--color-border) !important;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.el-input__wrapper:hover, .el-select__wrapper:hover {
  box-shadow: 0 0 0 1px var(--color-border-hover) !important;
}

.el-input__wrapper.is-focus, .el-select__wrapper.is-focused {
  box-shadow: 0 0 0 1px var(--color-ring), 0 0 0 4px rgba(192, 132, 212, 0.1) !important;
}

.el-input__inner, .el-select__inner {
  color: var(--color-foreground);
  font-family: var(--font-sans);
}

.el-input__inner::placeholder {
  color: var(--color-muted-foreground);
}

/* ── Dialog ── */
.el-dialog {
  background: var(--color-card) !important;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl) !important;
  box-shadow: var(--shadow-xl);
  backdrop-filter: blur(var(--glass-blur));
}

.el-dialog__title {
  color: var(--color-foreground) !important;
  font-weight: 700;
  font-size: 18px;
}

.el-dialog__header {
  border-bottom: 1px solid var(--color-border);
  padding: var(--space-4) var(--space-6);
}

.el-dialog__body {
  padding: var(--space-5);
}

.el-dialog__footer {
  border-top: 1px solid var(--color-border);
  padding: var(--space-3) var(--space-6);
}

.el-overlay {
  background: rgba(13, 15, 26, 0.85);
  backdrop-filter: blur(8px);
}

/* ── Form Label ── */
.el-form-item__label {
  color: var(--color-muted-foreground) !important;
  font-weight: 500;
}

/* ── Tabs ── */
.el-tabs__item {
  color: var(--color-muted-foreground);
  font-weight: 500;
}

.el-tabs__item.is-active {
  color: var(--color-primary) !important;
}

.el-tabs__active-bar {
  background: var(--color-primary) !important;
}

.el-tabs {
  --el-tabs-header-height: 48px;
}

/* ── Tag ── */
.el-tag {
  background: rgba(192, 132, 216, 0.08);
  border-color: rgba(192, 132, 216, 0.2);
  color: var(--color-primary);
  border-radius: var(--radius-full);
  font-weight: 500;
}

.el-tag--success {
  background: rgba(52, 211, 153, 0.08);
  border-color: rgba(52, 211, 153, 0.2);
  color: var(--color-success);
}

.el-tag--warning {
  background: rgba(251, 191, 36, 0.08);
  border-color: rgba(251, 191, 36, 0.2);
  color: var(--color-warning);
}

.el-tag--danger {
  background: rgba(248, 113, 113, 0.08);
  border-color: rgba(248, 113, 113, 0.2);
  color: var(--color-destructive);
}

.el-tag--primary {
  background: rgba(192, 132, 216, 0.08);
  border-color: rgba(192, 132, 216, 0.2);
  color: var(--color-accent);
}

.el-tag--info {
  background: rgba(96, 165, 250, 0.08);
  border-color: rgba(96, 165, 250, 0.2);
  color: var(--color-flat);
}

/* ── Message ── */
.el-message {
  background: var(--color-card) !important;
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-xl) !important;
  color: var(--color-foreground) !important;
  border-radius: var(--radius-lg);
}

/* ── Dropdown ── */
.el-popper.is-pure, .el-select-dropdown, .el-dropdown__popper {
  background: var(--color-card) !important;
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-xl) !important;
  border-radius: var(--radius-lg);
}

.el-select-dropdown__item {
  color: var(--color-foreground);
}

.el-select-dropdown__item.is-selected,
.el-select-dropdown__item.hover {
  background: rgba(192, 132, 216, 0.08);
  color: var(--color-primary);
}

.el-dropdown-menu__item {
  color: var(--color-foreground);
}

.el-dropdown-menu__item:not(.is-disabled):hover {
  background: var(--color-muted);
  color: var(--color-primary);
}

/* ── Menu (Sidebar) ── */
.el-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--color-muted-foreground);
  --el-menu-hover-bg-color: var(--color-muted);
  --el-menu-active-color: var(--color-primary);
  border-right: none;
}

.el-menu-item:hover {
  background: var(--color-muted) !important;
  color: var(--color-foreground) !important;
}

.el-menu-item.is-active {
  background: rgba(192, 132, 216, 0.08) !important;
  color: var(--color-primary) !important;
}

/* ── Progress ── */
.el-progress-bar__outer {
  background: var(--color-muted);
  border-radius: var(--radius-full);
  height: 6px;
}

.el-progress-bar__inner {
  background: linear-gradient(90deg, var(--color-primary) 0%, var(--color-secondary) 100%);
  border-radius: var(--radius-full);
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ── Empty State ── */
.el-empty__description p {
  color: var(--color-muted-foreground);
}

/* ── Skeleton ── */
.el-skeleton {
  --el-skeleton-color: var(--color-muted);
  --el-skeleton-to-color: var(--color-card);
}

/* ── Pagination ── */
.el-pagination {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: transparent;
}

.el-pagination button, .el-pager li {
  background: transparent !important;
  color: var(--color-muted-foreground) !important;
  border: 1px solid var(--color-border) !important;
  margin: 0 2px;
  border-radius: var(--radius-md) !important;
}

.el-pagination li.is-active {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-secondary) 100%) !important;
  border-color: transparent !important;
  color: var(--color-on-primary) !important;
}

/* ── Step ── */
.el-step__title {
  color: var(--color-muted-foreground);
}

.el-step__title.is-process {
  color: var(--color-primary);
  font-weight: 700;
}

.el-step__title.is-finish {
  color: var(--color-success);
}

.el-step__head.is-process {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.el-step__head.is-finish {
  color: var(--color-success);
  border-color: var(--color-success);
}

/* ── Switch ── */
.el-switch.is-checked .el-switch__core {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

/* ── Radio ── */
.el-radio__input.is-checked .el-radio__inner {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.el-radio__input.is-checked + .el-radio__label {
  color: var(--color-primary);
}

/* ── Checkbox ── */
.el-checkbox__input.is-checked .el-checkbox__inner {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

/* ── Select Option ── */
.el-select-dropdown__item.selected {
  color: var(--color-primary) !important;
  font-weight: 600;
}

/* ── Card Component ── */
.el-card {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  transition: all var(--transition-normal);
}

.el-card:hover {
  box-shadow: var(--shadow-lg);
}

.el-card__header {
  border-bottom: 1px solid var(--color-border);
}

/* ── Divider ── */
.el-divider {
  border-color: var(--color-border);
}

/* ── Loading ── */
.el-loading-mask {
  background: rgba(10, 14, 26, 0.8);
}

.el-loading-spinner .circular {
  stroke: var(--color-primary);
}

/* ── Tooltip ── */
.el-tooltip__popper.is-dark {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  color: var(--color-foreground);
}

/* ── Popover ── */
.el-popover.el-popper {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  color: var(--color-foreground);
}

/* ── Upload ── */
.el-upload-dragger {
  background: var(--color-muted);
  border-color: var(--color-border);
}

.el-upload-dragger:hover {
  border-color: var(--color-primary);
}

/* ── DatePicker ── */
.el-date-editor .el-input__wrapper {
  background: var(--color-muted);
}

.el-picker-panel {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  color: var(--color-foreground);
  border-radius: var(--radius-lg);
}

/* ── TimePicker ── */
.el-time-panel {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  color: var(--color-foreground);
}

/* ── Cascader ── */
.el-cascader__dropdown {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  color: var(--color-foreground);
}

/* ── Transfer ── */
.el-transfer-panel {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  color: var(--color-foreground);
}

/* ── Tree ── */
.el-tree {
  background: transparent;
  color: var(--color-foreground);
}

.el-tree-node__content:hover {
  background: var(--color-muted);
}

/* ── Timeline ── */
.el-timeline-item__content {
  color: var(--color-foreground);
}

/* ── Backtop ── */
.el-backtop {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
  color: var(--color-foreground);
}

.el-backtop:hover {
  background: var(--color-muted);
  color: var(--color-primary);
}

/* ── Drawer ── */
.el-drawer {
  background: var(--color-card);
}

.el-drawer__header {
  border-bottom: 1px solid var(--color-border);
}

/* ── Alert ── */
.el-alert {
  border-radius: var(--radius-lg);
  border: 1px solid transparent;
}

.el-alert--info {
  background: rgba(59, 130, 246, 0.06);
  border-color: rgba(59, 130, 246, 0.2);
}

.el-alert--success {
  background: rgba(16, 185, 129, 0.06);
  border-color: rgba(16, 185, 129, 0.2);
}

.el-alert--warning {
  background: rgba(245, 158, 11, 0.06);
  border-color: rgba(245, 158, 11, 0.2);
}

.el-alert--error {
  background: rgba(239, 68, 68, 0.06);
  border-color: rgba(239, 68, 68, 0.2);
}

/* ── Notification ── */
.el-notification {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
}

/* ── MessageBox ── */
.el-message-box {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
}

/* ── ColorPicker ── */
.el-color-picker__panel {
  background: var(--color-card);
  border: 1px solid var(--glass-border);
}

/* ── Responsive Utilities ── */
@media (max-width: 640px) {
  .hidden-mobile { display: none !important; }
}

@media (min-width: 641px) {
  .hidden-desktop { display: none !important; }
}

@media (max-width: 1024px) {
  .hidden-tablet { display: none !important; }
}

/* ── Reduced Motion ── */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* ── Focus Visible ── */
:focus-visible {
  outline: 2px solid var(--color-ring);
  outline-offset: 2px;
}
</style>