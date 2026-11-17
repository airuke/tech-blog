import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElButton,
  ElCheckbox,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElInput,
  ElInputNumber,
  ElOption,
  ElSelect,
} from 'element-plus'
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-button.css'
import 'element-plus/theme-chalk/el-checkbox.css'
import 'element-plus/theme-chalk/el-dialog.css'
import 'element-plus/theme-chalk/el-dropdown.css'
import 'element-plus/theme-chalk/el-dropdown-menu.css'
import 'element-plus/theme-chalk/el-dropdown-item.css'
import 'element-plus/theme-chalk/el-input.css'
import 'element-plus/theme-chalk/el-input-number.css'
import 'element-plus/theme-chalk/el-option.css'
import 'element-plus/theme-chalk/el-select.css'
import 'element-plus/theme-chalk/el-tooltip.css'
import 'element-plus/theme-chalk/el-popper.css'
import 'element-plus/theme-chalk/el-loading.css'
import 'element-plus/theme-chalk/el-icon.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'element-plus/theme-chalk/el-overlay.css'
import 'element-plus/theme-chalk/el-scrollbar.css'
import 'element-plus/theme-chalk/el-tag.css'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import './assets/design-system.css'
const app = createApp(App).use(createPinia()).use(router)
for (const component of [
  ElButton,
  ElCheckbox,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElInput,
  ElInputNumber,
  ElOption,
  ElSelect,
]) {
  app.component(component.name, component)
}
app.mount('#app')
