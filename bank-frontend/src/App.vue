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
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: #0a1628;
  color: #1a1a2e;
}

/* Global scrollbar */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #c0c4cc; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #909399; }

/* Element Plus overrides for bank theme */
:root {
  --bank-primary: #1a3a6b;
  --bank-primary-dark: #0c1a3b;
  --bank-gold: #f5af19;
  --bank-gold-dark: #d4a017;
  --bank-danger: #f12711;
  --bank-success: #52c41a;
  --bank-bg: #f0f2f5;
  --bank-card-shadow: 0 8px 24px rgba(0,0,0,0.08);
}

.el-button--primary {
  background: linear-gradient(135deg, #1a3a6b, #2d5a8c);
  border: none;
}
.el-button--primary:hover {
  background: linear-gradient(135deg, #2d5a8c, #3a6a9c);
}
.el-button--primary:active {
  background: linear-gradient(135deg, #0c1a3b, #1a3a6b);
}

.el-input__wrapper {
  box-shadow: 0 0 0 1px #d0d5de !important;
  border-radius: 8px;
  transition: all 0.2s;
}
.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px #1a3a6b !important;
}
.el-input__inner { font-weight: 500; }

.el-tabs__item.is-active { color: #1a3a6b !important; font-weight: 600; }
.el-tabs__active-bar { background: #1a3a6b !important; }
</style>
