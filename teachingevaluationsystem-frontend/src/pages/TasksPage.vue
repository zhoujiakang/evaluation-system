<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { clearAuth, getDisplayName } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'
import {
  getCourseTasks,
  getEvaluationOptions,
  getEvaluationProgress,
  startTask,
  submitTask,
  type StudentCourseTeacherTask,
  type StudentEvaluationOption,
  type StudentEvaluationProgress
} from '@/services/studentEvaluation'

type HomeView = 'course' | 'teacher'
type CourseGroup = { courseId: number; courseNameCn: string; courseNameEn: string; total: number; completed: number }
type TeacherTaskVM = StudentCourseTeacherTask & { courseId: number; courseNameCn: string; evaluationId: number }

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const courseRows = ref<CourseGroup[]>([])
const teacherRows = ref<TeacherTaskVM[]>([])

const tab = ref<'home' | 'me'>('home')
const homeView = ref<HomeView>('course')

const evaluationOptions = ref<StudentEvaluationOption[]>([])
const evaluationProgressMap = ref<Record<number, StudentEvaluationProgress>>({})
const selectedEvaluationId = ref<number | null>(null)
const selectedEvaluationName = ref<string>('')
const selectedCourseId = ref<number | null>(null)
const evalPickerVisible = ref(false)
const evalPickerValue = ref<number | null>(null)
const evalEntryClickCount = ref(0)
const oneClickScoring = ref(false)

const courseOptions = computed<CourseGroup[]>(() => {
  return courseRows.value
})

const selectedCourse = computed(() => courseOptions.value.find((it) => it.courseId === selectedCourseId.value) || null)

const teacherTasks = computed(() => {
  if (!selectedEvaluationId.value || !selectedCourseId.value) return []
  return teacherRows.value.filter((task) => task.evaluationId === selectedEvaluationId.value && task.courseId === selectedCourseId.value)
})

const meStats = computed(() => {
  const progress = selectedEvaluationId.value ? evaluationProgressMap.value[selectedEvaluationId.value] : null
  const total = progress?.total ?? 0
  const completed = progress?.completed ?? 0
  return { total, completed, pending: total - completed }
})

const pageTitle = computed(() => {
  if (tab.value === 'me') return selectedEvaluationName.value ? `我的 - ${selectedEvaluationName.value}` : '我的'
  if (homeView.value === 'course') return selectedEvaluationName.value || '课程列表'
  return `${selectedEvaluationName.value || ''}${selectedCourse.value ? ` - ${selectedCourse.value.courseNameCn}` : ''}` || '教师列表'
})

const showBack = computed(() => tab.value === 'home' && homeView.value === 'teacher')
const showEvaluationPickerEntry = computed(() => evaluationOptions.value.length > 0)
const showOneClickFullScore = computed(() => evalEntryClickCount.value >= 10)

async function load() {
  loading.value = true
  try {
    evaluationOptions.value = await getEvaluationOptions()
    if (!evaluationOptions.value.length) {
      selectedEvaluationId.value = null
      selectedEvaluationName.value = ''
      evaluationProgressMap.value = {}
      courseRows.value = []
      teacherRows.value = []
      return
    }
    const queryEvaluationId = Number(route.query.evaluationId)
    const targetEvaluationId = Number.isFinite(queryEvaluationId) && evaluationOptions.value.some((it) => it.evaluationId === queryEvaluationId)
      ? queryEvaluationId
      : evaluationOptions.value[0].evaluationId
    await chooseEvaluation(targetEvaluationId)
    if (selectedCourseId.value && !courseOptions.value.some((it) => it.courseId === selectedCourseId.value)) {
      selectedCourseId.value = null
      if (homeView.value === 'teacher') homeView.value = 'course'
    }
  } catch (error) {
    showFailToast(error instanceof Error ? error.message : '加载失败')
  } finally {
    const queryView = String(route.query.view || '')
    const queryEvaluationId = Number(route.query.evaluationId)
    const queryCourseId = Number(route.query.courseId)
    if (queryView === 'teacher' && Number.isFinite(queryEvaluationId) && Number.isFinite(queryCourseId)) {
      selectedEvaluationId.value = queryEvaluationId
      selectedCourseId.value = queryCourseId
      homeView.value = 'teacher'
    }
    loading.value = false
  }
}

async function loadCourseTree(evaluationId: number) {
  const data = await getCourseTasks(evaluationId)
  const courses: CourseGroup[] = []
  const teachers: TeacherTaskVM[] = []
  data.forEach((course) => {
    const total = (course.teachers || []).length
    const completed = (course.teachers || []).filter((it) => it.status === 'completed').length
    courses.push({
      courseId: course.courseId,
      courseNameCn: course.courseNameCn,
      courseNameEn: course.courseNameEn,
      total,
      completed
    })
    ;(course.teachers || []).forEach((teacher) => {
      teachers.push({
        ...teacher,
        courseId: course.courseId,
        courseNameCn: course.courseNameCn,
        evaluationId
      })
    })
  })
  courseRows.value = courses
  teacherRows.value = teachers
}

async function chooseEvaluation(evaluationId: number) {
  const option = evaluationOptions.value.find((it) => it.evaluationId === evaluationId)
  selectedEvaluationId.value = evaluationId
  selectedEvaluationName.value = option?.evaluationName || ''
  selectedCourseId.value = null
  homeView.value = 'course'
  const [progress] = await Promise.all([
    getEvaluationProgress(evaluationId),
    loadCourseTree(evaluationId)
  ])
  evaluationProgressMap.value = {
    ...evaluationProgressMap.value,
    [evaluationId]: progress
  }
  selectedEvaluationName.value = progress.evaluationName || selectedEvaluationName.value
}

function progressLabel(evaluationId: number) {
  const progress = evaluationProgressMap.value[evaluationId]
  return progress ? `完成 ${progress.completed}/${progress.total}` : ''
}

function goBack() {
  if (tab.value !== 'home') {
    tab.value = 'home'
    return
  }
  if (homeView.value === 'teacher') {
    homeView.value = 'course'
    return
  }
  if (homeView.value === 'course') {
    selectedCourseId.value = null
    return
  }
}

function chooseCourse(courseId: number) {
  selectedCourseId.value = courseId
  homeView.value = 'teacher'
}

function openEvaluationPicker() {
  if (!showEvaluationPickerEntry.value) return
  evalEntryClickCount.value += 1
  evalPickerValue.value = selectedEvaluationId.value
  evalPickerVisible.value = true
}

async function confirmPickEvaluation() {
  if (!evalPickerValue.value) return
  try {
    loading.value = true
    await chooseEvaluation(evalPickerValue.value)
    evalPickerVisible.value = false
  } catch (error) {
    showFailToast(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function oneClickFullScore() {
  if (!selectedEvaluationId.value) {
    showFailToast('请先选择测评')
    return
  }
  const pendingTasks = teacherRows.value.filter((task) => task.evaluationId === selectedEvaluationId.value && task.status !== 'completed')
  if (!pendingTasks.length) {
    showFailToast('当前测评没有待完成任务')
    return
  }
  try {
    await showConfirmDialog({
      title: '一键满分',
      message: `将为当前测评的 ${pendingTasks.length} 个教师任务提交满分，是否继续？`
    })
  } catch {
    return
  }

  oneClickScoring.value = true
  try {
    let successCount = 0
    for (const task of pendingTasks) {
      const detail = await startTask(task.taskId)
      const level2Indicators = (detail.indicators || []).filter((it) => it.level === 2)
      if (!level2Indicators.length) continue
      await submitTask(task.taskId, {
        scores: level2Indicators.map((it) => ({ indicatorId: it.id, score: 10 }))
      })
      successCount += 1
    }
    await chooseEvaluation(selectedEvaluationId.value)
    evalPickerVisible.value = false
    showSuccessToast(`已完成 ${successCount} 个教师任务满分提交`)
  } catch (error) {
    showFailToast(error instanceof Error ? error.message : '一键满分失败')
  } finally {
    oneClickScoring.value = false
  }
}

function openTask(task: TeacherTaskVM) {
  router.push({
    path: `/student/tasks/score/${task.taskId}`,
    query: {
      evaluationId: String(task.evaluationId),
      courseId: String(task.courseId),
      evaluationName: selectedEvaluationName.value,
      courseName: task.courseNameCn,
      teacherName: task.teacherName
    }
  })
}

function logout() {
  clearAuth()
  router.replace('/student/login')
}

onMounted(load)
</script>

<template>
  <div class="student-mobile-page">
    <van-sticky>
      <van-nav-bar
        :title="pageTitle"
        :left-arrow="showBack"
        :left-text="showBack ? '返回' : ''"
        :right-text="showEvaluationPickerEntry ? '测评' : ''"
        @click-left="goBack"
        @click-right="openEvaluationPicker"
      />
    </van-sticky>

    <div class="content-wrap">
      <van-skeleton title :row="6" :loading="loading">
        <template v-if="tab === 'home'">
          <van-empty v-if="!evaluationOptions.length" description="暂无可评测任务" />
          <van-empty v-else-if="!selectedEvaluationId" description="请选择测评" />

          <template v-else-if="homeView === 'course'">
            <van-empty v-if="!courseOptions.length" description="该测评暂无课程" />
            <van-cell-group v-else inset>
              <van-cell
                v-for="item in courseOptions"
                :key="item.courseId"
                clickable
                :title="item.courseNameCn"
                :label="`${item.courseNameEn || '-'}｜完成 ${item.completed}/${item.total}`"
                @click="chooseCourse(item.courseId)"
              />
            </van-cell-group>
          </template>

          <template v-else>
            <van-empty v-if="!teacherTasks.length" description="该课程暂无教师任务" />
            <van-cell-group v-else inset>
              <van-cell
                v-for="task in teacherTasks"
                :key="task.taskId"
                :title="task.teacherName"
                :label="task.teacherType === 'CN' ? '中方教师' : '外方教师'"
              >
                <template #value>
                  <van-space>
                    <van-tag :type="task.status === 'completed' ? 'success' : 'warning'">
                      {{ task.status === 'completed' ? '已完成' : '待评价' }}
                    </van-tag>
                    <van-button
                      size="small"
                      type="primary"
                      :disabled="task.status === 'completed'"
                      @click="openTask(task)"
                    >
                      评分
                    </van-button>
                  </van-space>
                </template>
              </van-cell>
            </van-cell-group>
          </template>
        </template>

        <template v-else>
          <div class="me-panel">
            <div class="me-hero">
              <div class="avatar">{{ (getDisplayName() || '同学').slice(0, 1) }}</div>
              <div>
                <div class="me-name">{{ getDisplayName() || '同学' }}</div>
                <div class="me-sub">欢迎使用学评教移动端</div>
              </div>
            </div>
            <div class="me-stats">
              <div class="stat-item"><span class="num">{{ meStats.total }}</span><span class="label">总任务</span></div>
              <div class="stat-item"><span class="num">{{ meStats.completed }}</span><span class="label">已完成</span></div>
              <div class="stat-item"><span class="num">{{ meStats.pending }}</span><span class="label">待完成</span></div>
            </div>
            <div class="progress-box">
              <div class="progress-title">完成进度</div>
              <van-progress
                :percentage="meStats.total ? Number(((meStats.completed / meStats.total) * 100).toFixed(0)) : 0"
                stroke-width="10"
                color="#1677ff"
                track-color="#eef3ff"
              />
            </div>
            <van-button block type="danger" plain @click="logout">退出登录</van-button>
          </div>
        </template>
      </van-skeleton>
    </div>

    <van-popup v-model:show="evalPickerVisible" round position="bottom" :style="{ padding: '12px 12px calc(12px + env(safe-area-inset-bottom))' }">
      <div class="picker-title">选择测评</div>
      <van-cell-group inset>
        <van-cell
          v-for="item in evaluationOptions"
          :key="item.evaluationId"
          clickable
          :title="item.evaluationName"
          :label="progressLabel(item.evaluationId)"
          @click="evalPickerValue = item.evaluationId"
        >
          <template #right-icon>
            <van-tag :type="evalPickerValue === item.evaluationId ? 'primary' : 'success'" :plain="evalPickerValue !== item.evaluationId">
              {{ evalPickerValue === item.evaluationId ? '已选' : '选择' }}
            </van-tag>
          </template>
        </van-cell>
      </van-cell-group>
      <div class="picker-actions">
        <van-button block type="primary" :disabled="!evalPickerValue" @click="confirmPickEvaluation">确认</van-button>
        <van-button
          v-if="showOneClickFullScore"
          block
          plain
          type="danger"
          :loading="oneClickScoring"
          style="margin-top: 8px"
          @click="oneClickFullScore"
        >
          一键满分
        </van-button>
      </div>
    </van-popup>

    <van-tabbar v-model="tab" fixed>
      <van-tabbar-item name="home" icon="home-o" />
      <van-tabbar-item name="me" icon="user-o" />
    </van-tabbar>
  </div>
</template>

<style scoped>
.student-mobile-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.content-wrap {
  padding: 12px 0 70px;
}

.picker-title {
  font-size: 15px;
  font-weight: 600;
  margin: 2px 4px 10px;
}

.picker-actions {
  margin-top: 12px;
}

.me-panel {
  background: #fff;
  margin: 12px;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
}

.me-hero {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1677ff, #69b1ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
}

.me-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
}

.me-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #86909c;
}

.me-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 18px;
}

.stat-item {
  text-align: center;
  flex: 1;
}

.num {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #1677ff;
}

.label {
  font-size: 12px;
  color: #86909c;
}

.progress-box {
  margin: 4px 0 18px;
  padding: 10px 12px 12px;
  border-radius: 10px;
  background: #f8fbff;
}

.progress-title {
  margin-bottom: 8px;
  color: #4e5969;
  font-size: 12px;
}

:deep(.van-tabbar-item__text) {
  display: none;
}

@media (min-width: 1024px) {
  .student-mobile-page {
    background: #f3f5f9;
  }

  .content-wrap {
    width: min(1200px, calc(100vw - 48px));
    margin: 0 auto;
    padding: 16px 0 24px;
  }

  .me-panel {
    margin: 0;
    border-radius: 14px;
  }

  :deep(.van-cell-group--inset) {
    margin: 0 !important;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
  }

  :deep(.van-cell) {
    padding: 16px 18px;
  }

  :deep(.van-cell__title) {
    font-size: 16px;
  }

}
</style>
