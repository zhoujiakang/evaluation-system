<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getEvaluations,
  getTeacherScoreStats,
  getStudentEvaluationStatus,
  exportIncompleteStudents,
  exportTeacherScoreStats,
  type AdminEvaluation,
  type TeacherScoreStat,
  type StudentEvaluationStatus
} from '@/services/domain'

const statLoading = ref(false)
const stats = ref<TeacherScoreStat[]>([])
const studentStatusRows = ref<StudentEvaluationStatus[]>([])
const evaluationOptions = ref<AdminEvaluation[]>([])
const viewMode = ref<'teacher' | 'student'>('teacher')
const pager = reactive({ page: 1, size: 10, total: 0 })
const query = reactive<{ evaluationId: number | null; teacherType: string; teacherName: string; studentNo: string; studentName: string; className: string }>({
  evaluationId: null,
  teacherType: '',
  teacherName: '',
  studentNo: '',
  studentName: '',
  className: ''
})

function resetQuery() {
  Object.assign(query, { evaluationId: null, teacherType: '', teacherName: '', studentNo: '', studentName: '', className: '' })
  stats.value = []
  studentStatusRows.value = []
  pager.page = 1
  pager.total = 0
}

async function loadOptions() {
  evaluationOptions.value = (await getEvaluations({ page: 1, size: 1000 })).records
}

async function loadStats() {
  if (!query.evaluationId) {
    ElMessage.warning('请先选择测评')
    return
  }
  statLoading.value = true
  try {
    const statsPage = await getTeacherScoreStats(query.evaluationId, {
      page: pager.page,
      size: pager.size,
      teacherType: query.teacherType || undefined,
      teacherName: query.teacherName || undefined
    })
    stats.value = statsPage.records
    pager.total = statsPage.total
    pager.page = statsPage.current || pager.page
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '统计加载失败')
  } finally {
    statLoading.value = false
  }
}

async function loadStudentStatus() {
  if (!query.evaluationId) {
    ElMessage.warning('请先选择测评')
    return
  }
  statLoading.value = true
  try {
    const statusPage = await getStudentEvaluationStatus(query.evaluationId, {
      page: pager.page,
      size: pager.size,
      studentNo: query.studentNo,
      studentName: query.studentName,
      className: query.className
    })
    studentStatusRows.value = statusPage.records
    pager.total = statusPage.total
    pager.page = statusPage.current || pager.page
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '学生评价情况加载失败')
  } finally {
    statLoading.value = false
  }
}

async function exportTeacherStatsFile() {
  if (!query.evaluationId) {
    ElMessage.warning('请先选择测评')
    return
  }
  try {
    await exportTeacherScoreStats(query.evaluationId)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  }
}

async function exportIncompleteStudentsFile() {
  if (!query.evaluationId) {
    ElMessage.warning('请先选择测评')
    return
  }
  try {
    await exportIncompleteStudents(query.evaluationId)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  }
}

onMounted(async () => {
  await loadOptions()
})

function onPageChange(page: number) {
  pager.page = page
  if (viewMode.value === 'teacher') loadStats()
  else loadStudentStatus()
}

function onSizeChange(size: number) {
  pager.size = size
  pager.page = 1
  if (viewMode.value === 'teacher') loadStats()
  else loadStudentStatus()
}

function searchTeacherStats() {
  pager.page = 1
  loadStats()
}

function searchStudentStatus() {
  pager.page = 1
  loadStudentStatus()
}

function onQueryEnter() {
  if (viewMode.value === 'teacher') {
    searchTeacherStats()
    return
  }
  searchStudentStatus()
}

watch(viewMode, (mode) => {
  pager.page = 1
  if (mode === 'teacher') {
    query.studentNo = ''
    query.studentName = ''
    query.className = ''
    studentStatusRows.value = []
    if (!query.evaluationId) {
      pager.total = 0
      return
    }
    loadStats()
    return
  }
  query.teacherType = ''
  query.teacherName = ''
  stats.value = []
  if (!query.evaluationId) {
    pager.total = 0
    return
  }
  loadStudentStatus()
})

watch(
  () => query.evaluationId,
  (id, oldId) => {
    if (id === oldId) return
    pager.page = 1
    if (!id) {
      stats.value = []
      studentStatusRows.value = []
      pager.total = 0
      return
    }
    if (viewMode.value === 'teacher') {
      loadStats()
      return
    }
    loadStudentStatus()
  }
)

watch(
  () => query.teacherType,
  (type, oldType) => {
    if (type === oldType || viewMode.value !== 'teacher' || !query.evaluationId) return
    pager.page = 1
    loadStats()
  }
)
</script>

<template>
  <el-card>
    <template #header>
      <div class="page-head">
        <span>分数查看</span>
      </div>
    </template>
    <div class="query-bar score-query-bar">
      <div class="mode-row">
        <el-segmented v-model="viewMode" :options="[{ label: '老师分数详情', value: 'teacher' }, { label: '学生评价情况', value: 'student' }]" />
      </div>
      <el-form :model="query" inline class="filter-row">
        <el-form-item label="测评">
          <el-select v-model="query.evaluationId" clearable placeholder="请选择测评" style="width: 260px">
            <el-option v-for="item in evaluationOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="viewMode === 'teacher'" label="教师类型">
          <el-select v-model="query.teacherType" clearable placeholder="全部" style="width: 160px">
            <el-option label="中方" value="CN" />
            <el-option label="外方" value="INTL" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="viewMode === 'teacher'" label="老师姓名">
          <el-input v-model="query.teacherName" clearable placeholder="请输入老师姓名" style="width: 180px" @keyup.enter="onQueryEnter" />
        </el-form-item>
        <el-form-item v-else label="学号"><el-input v-model="query.studentNo" clearable placeholder="学号" style="width: 160px" @keyup.enter="onQueryEnter" /></el-form-item>
        <el-form-item v-if="viewMode === 'student'" label="姓名"><el-input v-model="query.studentName" clearable placeholder="姓名" style="width: 140px" @keyup.enter="onQueryEnter" /></el-form-item>
        <el-form-item v-if="viewMode === 'student'" label="班级"><el-input v-model="query.className" clearable placeholder="班级名称" style="width: 180px" @keyup.enter="onQueryEnter" /></el-form-item>
        <el-form-item class="query-actions">
          <el-button v-if="viewMode === 'teacher'" type="primary" @click="searchTeacherStats">查询</el-button>
          <el-button v-else type="primary" @click="searchStudentStatus">查询</el-button>
          <el-button v-if="viewMode === 'teacher'" @click="exportTeacherStatsFile">导出老师汇总</el-button>
          <el-button v-else @click="exportIncompleteStudentsFile">导出未完成名单</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="admin-data-panel">
      <el-table v-if="viewMode === 'teacher'" :data="stats" v-loading="statLoading">
        <el-table-column prop="teacherName" label="教师" min-width="160" />
        <el-table-column prop="teacherType" label="教师类型" width="100" />
        <el-table-column prop="submissionCount" label="有效提交数" width="120" />
        <el-table-column prop="averageScore" label="加权平均分" width="140" />
      </el-table>
      <div v-if="viewMode === 'teacher'" class="pager-wrap">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          :total="pager.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>

      <el-table v-else :data="studentStatusRows" v-loading="statLoading">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="grade" label="年级" width="100" />
        <el-table-column prop="className" label="班级" min-width="140" />
        <el-table-column prop="major" label="专业" min-width="140" />
        <el-table-column prop="totalTasks" label="总任务数" width="100" />
        <el-table-column prop="completedTasks" label="已完成" width="90" />
        <el-table-column prop="pendingTasks" label="待完成" width="90" />
        <el-table-column label="完成率" width="100">
          <template #default="{ row }">{{ row.completionRate }}%</template>
        </el-table-column>
      </el-table>
      <div v-if="viewMode === 'student'" class="pager-wrap">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          :total="pager.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.page-head { display: flex; align-items: center; justify-content: space-between; }
.query-bar { margin-bottom: 12px; }
.pager-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
.score-query-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.mode-row {
  display: flex;
  align-items: center;
}
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 12px;
}
</style>
