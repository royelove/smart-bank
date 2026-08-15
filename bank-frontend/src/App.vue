<template>
  <router-view />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  min-height: 100vh;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', 'Segoe UI', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: var(--vault-bg);
  color: var(--vault-text);
}

/* ============================================================
   设计系统 · 深藏蓝 × 香槟金 · 顶级银行奢华风
   ============================================================ */
:root {
  /* —— 基底:深邃藏蓝 × 墨黑蓝 —— */
  --vault-bg: #0a101c;
  --vault-bg-2: #0d1526;
  --vault-panel: #111a2e;
  --vault-panel-2: #16213a;
  --vault-panel-3: #1c2846;
  --vault-graphite: #0c1220;

  /* —— 香槟金描边:1px 金属细线 —— */
  --vault-line: rgba(212, 176, 106, 0.13);
  --vault-line-strong: rgba(212, 176, 106, 0.26);
  --vault-line-hover: rgba(212, 176, 106, 0.42);

  /* —— 文字:暖白梯度 —— */
  --vault-text: #eef2f8;
  --vault-text-2: #b9c3d8;
  --vault-text-3: #78859e;
  --vault-text-dim: #55617a;

  /* —— 数据语义:青涨赤跌(低饱和) —— */
  --vault-up: #2fa8a0;      /* 青:上涨 */
  --vault-up-soft: #3fc9bf;
  --vault-down: #d05a5a;    /* 赤:下跌 */
  --vault-down-soft: #e07070;
  --vault-flat: #8a97ab;

  /* —— 主色调:香槟金(奢华) —— */
  --vault-accent: #d4b06a;
  --vault-accent-glow: rgba(212, 176, 106, 0.35);
  --vault-accent-deep: #b08d4a;

  /* —— 高亮金(文字强调) —— */
  --vault-gold: #e6c98a;

  /* —— 字体:几何无衬线,宽字距 —— */
  --vault-font-num: 'DIN Alternate', 'Bahnschrift', 'Segoe UI', 'Roboto', monospace;
  --vault-letter-space: 0.04em;

  --vault-radius: 10px;
  --vault-radius-lg: 14px;
}

/* —— 全局滚动条:哑光金属 —— */
::-webkit-scrollbar { width: 8px; height: 8px; }
::-webkit-scrollbar-track { background: var(--vault-graphite); }
::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #2a3650, #1c2638);
  border-radius: 4px;
  border: 1px solid var(--vault-line);
}
::-webkit-scrollbar-thumb:hover { background: #3a4a68; }

/* ============================================================
   通用面板 · 拉丝金属 + 1px 冷白描边 + 精密景深
   ============================================================ */
.vault-panel {
  position: relative;
  background: linear-gradient(180deg, var(--vault-panel-2), var(--vault-panel));
  border: 1px solid var(--vault-line);
  border-radius: var(--vault-radius-lg);
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,0.03),
    0 12px 32px rgba(0,0,0,0.35);
  overflow: hidden;
}
/* 极细腻横向拉丝纹理 */
.vault-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: repeating-linear-gradient(
    90deg,
    transparent 0,
    transparent 3px,
    rgba(212,176,106,0.012) 3px,
    rgba(212,176,106,0.012) 4px
  );
  opacity: 0.6;
}

/* —— 呼吸灯:柔和明灭 —— */
@keyframes vault-breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}
.vault-breathe { animation: vault-breathe 2.4s ease-in-out infinite; }

/* —— 服务器状态点 —— */
.vault-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--vault-up);
  box-shadow: 0 0 10px var(--vault-up-soft);
  animation: vault-breathe 2.4s ease-in-out infinite;
}

/* —— 数字字体,仪表盘读数 —— */
.vault-num {
  font-family: var(--vault-font-num);
  letter-spacing: var(--vault-letter-space);
  font-variant-numeric: tabular-nums;
}

/* —— 等高线守护纹样(资产总览用) —— */
.vault-contour {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    repeating-radial-gradient(ellipse at 50% 20%, transparent 0, transparent 40px, rgba(212,176,106,0.04) 41px, transparent 42px),
    repeating-radial-gradient(ellipse at 50% 20%, transparent 0, transparent 80px, rgba(212,176,106,0.03) 81px, transparent 82px);
  mask-image: linear-gradient(180deg, transparent 0, #000 40%);
  -webkit-mask-image: linear-gradient(180deg, transparent 0, #000 40%);
}

/* ============================================================
   Element Plus 深度定制 · 精密仪表风
   ============================================================ */

/* —— 表格:冷白描边,细网格 —— */
.el-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(212,176,106,0.04);
  --el-table-row-hover-bg-color: rgba(212,176,106,0.06);
  --el-table-border-color: var(--vault-line);
  --el-table-text-color: var(--vault-text-2);
  --el-table-header-text-color: var(--vault-text-3);
  background: transparent;
  color: var(--vault-text);
  font-size: 13px;
}
.el-table th.el-table__cell {
  background: rgba(212,176,106,0.04) !important;
  border-bottom: 1px solid var(--vault-line) !important;
  letter-spacing: 0.06em;
  font-weight: 600;
}
.el-table td.el-table__cell { border-bottom: 1px solid var(--vault-line) !important; }
.el-table .el-table__inner-wrapper::before { display: none; }
.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background: rgba(212,176,106,0.02) !important;
}

/* —— 按钮:哑光金属 + 冷白描边 —— */
.el-button {
  border-radius: 8px;
  border: 1px solid var(--vault-line-strong);
  background: var(--vault-panel-3);
  color: var(--vault-text-2);
  letter-spacing: var(--vault-letter-space);
  transition: all 0.25s ease;
}
.el-button:hover {
  border-color: var(--vault-line-hover);
  background: var(--vault-panel-2);
  color: var(--vault-text);
  box-shadow: 0 0 12px rgba(212,176,106,0.10);
}
.el-button--primary {
  background: linear-gradient(135deg, var(--vault-accent), var(--vault-accent-deep));
  border: none;
  color: #231a08;
  font-weight: 600;
}
.el-button--primary:hover {
  background: linear-gradient(135deg, #e6c98a, #c9a458);
  box-shadow: 0 0 18px var(--vault-accent-glow);
  color: #1a1306;
}
.el-button--danger {
  background: rgba(208,90,90,0.12);
  border: 1px solid rgba(208,90,90,0.35);
  color: var(--vault-down-soft);
}
.el-button--danger:hover {
  background: rgba(208,90,90,0.2);
  border-color: rgba(208,90,90,0.6);
  color: #ff8a8a;
}

/* —— 输入框 —— */
.el-input__wrapper, .el-select__wrapper {
  background: var(--vault-graphite);
  box-shadow: 0 0 0 1px var(--vault-line-strong) !important;
  border-radius: 8px;
  transition: all 0.25s;
}
.el-input__wrapper:hover, .el-select__wrapper:hover { box-shadow: 0 0 0 1px var(--vault-line-hover) !important; }
.el-input__wrapper.is-focus, .el-select__wrapper.is-focused {
  box-shadow: 0 0 0 1px var(--vault-accent), 0 0 12px var(--vault-accent-glow) !important;
}
.el-input__inner, .el-select__inner {
  color: var(--vault-text);
  font-family: var(--vault-font-num);
  letter-spacing: var(--vault-letter-space);
}
.el-input__inner::placeholder { color: var(--vault-text-dim); }
.el-input-number { --el-input-number-controls-bg: var(--vault-graphite); }

/* —— 弹窗:精密仪表浮层 —— */
.el-dialog {
  background: linear-gradient(180deg, var(--vault-panel-2), var(--vault-panel)) !important;
  border: 1px solid var(--vault-line-strong);
  border-radius: 16px !important;
  box-shadow: 0 24px 64px rgba(0,0,0,0.6), 0 0 0 1px rgba(212,176,106,0.04);
}
.el-dialog__title {
  color: var(--vault-text) !important;
  letter-spacing: 0.08em;
  font-weight: 600;
}
.el-dialog__header { border-bottom: 1px solid var(--vault-line); }
.el-overlay { background: rgba(4,7,12,0.75); backdrop-filter: blur(4px); }

/* —— 表单标签 —— */
.el-form-item__label { color: var(--vault-text-3) !important; letter-spacing: 0.05em; }

/* —— Tabs —— */
.el-tabs__item { color: var(--vault-text-3); }
.el-tabs__item.is-active { color: var(--vault-accent) !important; }
.el-tabs__active-bar { background: var(--vault-accent) !important; }
.el-tabs { --el-tabs-header-height: 44px; }

/* —— Tag —— */
.el-tag {
  background: rgba(212,176,106,0.06);
  border-color: var(--vault-line-strong);
  color: var(--vault-text-2);
}
.el-tag--success { background: rgba(47,168,160,0.14); border-color: rgba(47,168,160,0.4); color: var(--vault-up-soft); }
.el-tag--warning { background: rgba(212,176,106,0.12); border-color: rgba(212,176,106,0.4); color: var(--vault-gold); }
.el-tag--danger { background: rgba(208,90,90,0.14); border-color: rgba(208,90,90,0.4); color: var(--vault-down-soft); }
.el-tag--primary { background: rgba(212,176,106,0.12); border-color: rgba(212,176,106,0.4); color: var(--vault-accent); }

/* —— 消息/通知 —— */
.el-message {
  background: var(--vault-panel-3) !important;
  border: 1px solid var(--vault-line-strong) !important;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5) !important;
  color: var(--vault-text) !important;
}
.el-message .el-message__content { color: var(--vault-text) !important; }

/* —— 下拉面板 —— */
.el-popper.is-pure, .el-select-dropdown, .el-dropdown__popper {
  background: var(--vault-panel-3) !important;
  border: 1px solid var(--vault-line-strong) !important;
  box-shadow: 0 12px 32px rgba(0,0,0,0.55) !important;
}
.el-select-dropdown__item { color: var(--vault-text-2); }
.el-select-dropdown__item.is-selected, .el-select-dropdown__item.hover { background: rgba(212,176,106,0.1); color: var(--vault-text); }
.el-dropdown-menu__item { color: var(--vault-text-2); }
.el-dropdown-menu__item:not(.is-disabled):hover { background: rgba(212,176,106,0.1); color: var(--vault-text); }

/* —— 菜单(侧边栏) —— */
.el-menu { --el-menu-bg-color: transparent; --el-menu-text-color: var(--vault-text-3); --el-menu-hover-bg-color: rgba(212,176,106,0.08); --el-menu-active-color: var(--vault-accent); border-right: none; }
.el-menu-item:hover { background: rgba(212,176,106,0.08) !important; color: var(--vault-text) !important; }
.el-menu-item.is-active { background: rgba(212,176,106,0.12) !important; color: var(--vault-accent) !important; }

/* —— 进度条 —— */
.el-progress-bar__outer { background: var(--vault-panel-3); }
.el-progress-bar__inner { transition: width 0.6s ease; }

/* —— 空状态 —— */
.el-empty__description p { color: var(--vault-text-3); }

/* —— 骨架屏 —— */
.el-skeleton { --el-skeleton-color: var(--vault-panel-3); --el-skeleton-to-color: var(--vault-panel-2); }

/* —— 分页 —— */
.el-pagination { --el-pagination-bg-color: transparent; --el-pagination-button-bg-color: transparent; }
.el-pagination button, .el-pager li {
  background: transparent !important;
  color: var(--vault-text-3) !important;
  border: 1px solid var(--vault-line) !important;
  margin: 0 2px;
  border-radius: 6px !important;
}
.el-pagination li.is-active { background: var(--vault-accent) !important; border-color: var(--vault-accent) !important; color: #fff !important; }
</style>
