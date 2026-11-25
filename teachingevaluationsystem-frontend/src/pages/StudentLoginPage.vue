<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { studentLogin } from '@/services/auth'
import { setAuth } from '@/stores/auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  if (!form.username || !form.password) {
    showFailToast('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data = await studentLogin(form)
    setAuth(data.token, 'student', data.name)
    await router.replace('/student/tasks')
  } catch (error) {
    showFailToast(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="student-login-page">
    <div class="login-hero">
      <div class="hero-title">学生端登录</div>
      <div class="hero-sub">课程评教系统</div>
    </div>

    <div class="login-card">
      <van-form @submit="submit">
        <van-field
          v-model="form.username"
          name="username"
          label="账号"
          placeholder="请输入学号"
          clearable
          autocomplete="username"
        />
        <van-field
          v-model="form.password"
          name="password"
          label="密码"
          type="password"
          placeholder="请输入密码"
          clearable
          autocomplete="current-password"
        />
        <div class="login-btn-wrap">
          <van-button block type="primary" native-type="submit" :loading="loading">登录</van-button>
        </div>
      </van-form>
      <div class="login-tip">默认账号密码通常为学号</div>
    </div>
  </div>
</template>

<style scoped>
.student-login-page {
  min-height: 100vh;
  padding: 24px 16px calc(24px + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, #eaf3ff 0%, #f7f8fa 42%, #f7f8fa 100%);
}

.login-hero {
  padding: 34px 8px 22px;
}

.hero-title {
  font-size: 28px;
  line-height: 1.2;
  font-weight: 700;
  color: #1f2329;
}

.hero-sub {
  margin-top: 8px;
  font-size: 14px;
  color: #4e5969;
}

.login-card {
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.login-btn-wrap {
  padding: 18px 16px 8px;
}

.login-tip {
  padding: 0 16px 16px;
  color: #86909c;
  font-size: 12px;
}
</style>
