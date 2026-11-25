import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'vant/lib/index.css'
import '@/styles/global.css'
import App from './App.vue'
import router from './router'
import { Button, Cell, CellGroup, Empty, Field, Form, NavBar, Popup, Progress, Skeleton, Slider, Space, Sticky, Tabbar, TabbarItem, Tag } from 'vant'

const app = createApp(App)

app
  .use(ElementPlus)
  .use(Button)
  .use(Cell)
  .use(CellGroup)
  .use(Empty)
  .use(Field)
  .use(Form)
  .use(NavBar)
  .use(Popup)
  .use(Progress)
  .use(Skeleton)
  .use(Slider)
  .use(Space)
  .use(Sticky)
  .use(Tabbar)
  .use(TabbarItem)
  .use(Tag)
  .use(router)
  .mount('#app')
