<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { startTask, submitTask, type Indicator } from '@/services/studentEvaluation'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitLoading = ref(false)
const indicators = ref<Indicator[]>([])
const scoreMap = reactive<Record<number, number>>({})

const taskId = Number(route.params.taskId)
const evaluationId = Number(route.query.evaluationId)
const courseId = Number(route.query.courseId)
const evaluationName = String(route.query.evaluationName || '')
const courseName = String(route.query.courseName || '')
const teacherName = String(route.query.teacherName || '')

const pageTitle = `${evaluationName}${courseName ? ` - ${courseName}` : ''}`

async function load() {
  if (!Number.isFinite(taskId) || taskId <= 0) {
    showFailToast('任务参数无效')
    router.replace('/student/tasks')
    return
  }
  loading.value = true
  try {
    const detail = await startTask(taskId)
    indicators.value = detail.indicators.filter((it) => it.level === 2)
    indicators.value.forEach((it) => {
      scoreMap[it.id] = 8
    })
  } catch (error) {
    showFailToast(error instanceof Error ? error.message : '加载评分项失败')
  } finally {
    loading.value = false
  }
}

async function submit() {
  submitLoading.value = true
  try {
    await submitTask(taskId, {
      scores: indicators.value.map((it) => ({ indicatorId: it.id, score: scoreMap[it.id] ?? 0 }))
    })
    showSuccessToast('提交成功')
    router.replace({
      path: '/student/tasks',
      query: {
        view: 'teacher',
        evaluationId: Number.isFinite(evaluationId) ? String(evaluationId) : '',
        courseId: Number.isFinite(courseId) ? String(courseId) : ''
      }
    })
  } catch (error) {
    showFailToast(error instanceof Error ? error.message : '提交失败')
  } finally {
    submitLoading.value = false
  }
}

function goBack() {
  router.replace({
    path: '/student/tasks',
    query: {
      view: 'teacher',
      evaluationId: Number.isFinite(evaluationId) ? String(evaluationId) : '',
      courseId: Number.isFinite(courseId) ? String(courseId) : '',
      evaluationName: evaluationName || '',
      courseName: courseName || ''
    }
  })
}

onMounted(load)
</script>

<template>
  <div class="score-page">
    <van-nav-bar :title="pageTitle || '评分'" left-arrow left-text="返回" @click-left="goBack" />
    <div class="score-content">
      <div class="teacher-name">{{ teacherName || '教师评分' }}</div>
      <van-skeleton title :row="6" :loading="loading">
        <van-empty v-if="!indicators.length" description="暂无可评分指标" />
        <div v-else class="indicator-list">
          <div v-for="it in indicators" :key="it.id" class="indicator-item">
            <div class="indicator-name">{{ it.name }}</div>
            <div class="slider-row">
              <van-slider v-model="scoreMap[it.id]" :min="0" :max="10" :step="1" bar-height="6px" />
              <span class="score-value">{{ scoreMap[it.id] ?? 0 }}</span>
            </div>
          </div>
        </div>
      </van-skeleton>
    </div>
    <div class="submit-bar">
      <van-button block type="primary" :loading="submitLoading" @click="submit">提交评分</van-button>
    </div>
  </div>
</template>

<style scoped>
.score-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.score-content {
  padding: 12px 12px 90px;
}

.teacher-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 10px;
}

.indicator-list {
  margin-top: 6px;
}

.indicator-item {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 10px 10px 12px;
  margin-bottom: 10px;
}

.indicator-name {
  font-size: 13px;
  color: #4e5969;
  line-height: 1.4;
  margin-bottom: 10px;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.score-value {
  min-width: 20px;
  text-align: right;
  color: #1677ff;
  font-size: 14px;
  font-weight: 600;
}

.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

@media (min-width: 1024px) {
  .score-content {
    width: min(1000px, calc(100vw - 56px));
    margin: 0 auto;
    padding: 18px 0 24px;
  }

  .teacher-name {
    margin-bottom: 14px;
    font-size: 20px;
  }

  .indicator-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .indicator-item {
    margin-bottom: 0;
    border-radius: 12px;
    padding: 14px;
  }

  .submit-bar {
    position: static;
    width: min(1000px, calc(100vw - 56px));
    margin: 0 auto 20px;
    background: transparent;
    border-top: 0;
    padding: 8px 0 0;
  }
}
</style>
