<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLogin } from '@/services/auth'
import { setAuth } from '@/stores/auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  loading.value = true
  try {
    const data = await adminLogin(form)
    setAuth(data.token, 'admin', data.name)
    await router.replace('/admin/classes')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell">
    <el-card class="auth-card">
      <h2>管理端登录</h2>
      <el-form :model="form" label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="submit" style="width: 100%">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.auth-shell { min-height: 100vh; display: grid; place-items: center; padding: 20px; }
.auth-card { width: 380px; max-width: 100%; }
h2 { margin: 0 0 16px; }
</style>
