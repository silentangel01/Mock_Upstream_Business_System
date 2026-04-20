import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'vant/es/toast/style'
import 'vant/es/dialog/style'
import 'vant/es/notify/style'
import 'vant/es/image-preview/style'
import AppH5 from './AppH5.vue'
import router from './h5/router'

const app = createApp(AppH5)
app.use(createPinia())
app.use(router)
app.mount('#app')
