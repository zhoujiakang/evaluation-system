<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth, getDisplayName } from '@/stores/auth'

const router = useRouter()
const route = useRoute()

const menus = [
  { key: '/admin/classes', label: '班级管理' },
  { key: '/admin/students', label: '学生管理' },
  { key: '/admin/teachers', label: '教师管理' },
  { key: '/admin/admin-users', label: '管理员管理' },
  { key: '/admin/courses', label: '课程管理' },
  { key: '/admin/indicators', label: '指标管理' },
  { key: '/admin/evaluations', label: '测评管理' },
  { key: '/admin/scores', label: '分数查看' }
]

const activeMenu = computed(() => route.path)
const displayName = computed(() => getDisplayName() || '管理员')

function logout() {
  clearAuth()
  router.replace('/admin/login')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="aside">
      <div class="logo">管理端</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="item in menus" :key="item.key" :index="item.key">{{ item.label }}</el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <div class="profile">{{ displayName }}</div>
        <el-button size="small" class="logout-btn" @click="logout">退出登录</el-button>
      </div>
    </aside>

    <main class="main">
      <div class="main-scroll">
        <router-view />
      </div>
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  height: 100vh;
  display: grid;
  grid-template-columns: 220px 1fr;
  overflow: hidden;
}

.aside {
  border-right: 1px solid #e7ebf3;
  background: #fff;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.logo {
  height: 56px;
  display: grid;
  place-items: center;
  font-weight: 700;
  border-bottom: 1px solid #eef2f7;
}

.menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  border-right: none;
}

.aside-footer {
  border-top: 1px solid #eef2f7;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.logout-btn {
  flex: none;
}

.main {
  min-width: 0;
  min-height: 0;
  background: #f5f7fb;
}

.main-scroll {
  height: 100%;
  overflow: hidden;
  padding: 16px;
  --admin-sticky-top: 0px;
}

@media (max-width: 900px) {
  .admin-layout {
    grid-template-columns: 200px 1fr;
  }
}
</style>
