<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadProps } from 'element-plus'
import {
  closeEvaluation,
  createEvaluation,
  createEvaluationWithCourses,
  deleteEvaluation,
  getClasses,
  getCourses,
  getEvaluations,
  getTeachers,
  publishEvaluation,
  updateEvaluation,
  type AdminClass,
  type AdminCourse,
  type AdminEvaluation,
  type AdminTeacher,
  type EvaluationPayload
} from '@/services/domain'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const importingCourses = ref(false)
const editingId = ref<number | null>(null)
const importFile = ref<File | null>(null)
const rows = ref<AdminEvaluation[]>([])
const pager = reactive({ page: 1, size: 10, total: 0 })
const detailEvaluation = ref<AdminEvaluation | null>(null)
const detailCourses = ref<Array<AdminCourse & { classNames: string[]; teacherNames: string[] }>>([])
const query = reactive({ status: '' })
const form = reactive<EvaluationPayload>({ name: '', academicYear: '', term: '', status: 'draft' })

async function load() {
  loading.value = true
  try {
    const res = await getEvaluations({ ...query, page: pager.page, size: pager.size })
    rows.value = res.records
    pager.total = res.total
    pager.page = res.current || pager.page
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.status = ''
  pager.page = 1
  load()
}

function openCreate() {
  editingId.value = null
  importFile.value = null
  Object.assign(form, { name: '', academicYear: '', term: '', status: 'draft' })
  dialogVisible.value = true
}

function openEdit(row: AdminEvaluation) {
  editingId.value = row.id
  importFile.value = null
  Object.assign(form, { name: row.name, academicYear: row.academicYear, term: row.term, status: row.status })
  dialogVisible.value = true
}

const beforeImportUpload: UploadProps['beforeUpload'] = (file) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || (ext !== 'xlsx' && ext !== 'xls')) {
    ElMessage.error('仅支持 .xlsx 或 .xls 文件')
    return false
  }
  importFile.value = file
  return false
}

async function save() {
  saving.value = true
  importingCourses.value = false
  try {
    if (editingId.value) {
      await updateEvaluation(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      if (importFile.value) {
        importingCourses.value = true
        const result = await createEvaluationWithCourses(form, importFile.value)
        ElMessage.success(
          `测评已创建并导入课程：成功 ${result.importResult.successRows}，跳过 ${result.importResult.skippedRows}，新增课程 ${result.importResult.createdCourses}`
        )
      } else {
        await createEvaluation(form)
        ElMessage.success('添加成功')
      }
    }
    dialogVisible.value = false
    importFile.value = null
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    importingCourses.value = false
    saving.value = false
  }
}

async function remove(row: AdminEvaluation) {
  try {
    await ElMessageBox.confirm(`确认删除测评「${row.name}」？`, '删除确认', { type: 'warning' })
    await deleteEvaluation(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function publish(row: AdminEvaluation) {
  try {
    await ElMessageBox.confirm(`确认发布测评「${row.name}」并生成任务？`, '发布确认', { type: 'warning' })
    await publishEvaluation(row.id)
    ElMessage.success('发布成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '发布失败')
  }
}

async function close(row: AdminEvaluation) {
  try {
    await ElMessageBox.confirm(`确认关闭测评「${row.name}」？`, '关闭确认', { type: 'warning' })
    await closeEvaluation(row.id)
    ElMessage.success('关闭成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '关闭失败')
  }
}

async function openDetail(row: AdminEvaluation) {
  detailVisible.value = true
  detailLoading.value = true
  detailEvaluation.value = row
  try {
    const [coursePage, classPage, teacherPage] = await Promise.all([
      getCourses({ evaluationId: row.id, page: 1, size: 1000 }),
      getClasses({ page: 1, size: 1000 }),
      getTeachers({ page: 1, size: 1000 })
    ])
    const classMap = new Map<number, AdminClass>(classPage.records.map((it) => [it.id, it]))
    const teacherMap = new Map<number, AdminTeacher>(teacherPage.records.map((it) => [it.id, it]))
    detailCourses.value = coursePage.records.map((course) => ({
      ...course,
      classNames: (course.classIds || []).map((id) => {
        const c = classMap.get(id)
        return c ? `${c.grade} ${c.className}` : `班级#${id}`
      }),
      teacherNames: (course.teacherIds || []).map((id) => {
        const t = teacherMap.get(id)
        return t ? `${t.name}（${t.teacherType}）` : `教师#${id}`
      })
    }))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(load)

function onPageChange(page: number) {
  pager.page = page
  load()
}

function onSizeChange(size: number) {
  pager.size = size
  pager.page = 1
  load()
}

watch(
  () => query.status,
  (value, oldValue) => {
    if (value === oldValue) return
    pager.page = 1
    load()
  }
)
</script>

<template>
  <el-card>
    <template #header>
      <div class="page-head">
        <span>测评管理</span>
        <el-button type="primary" @click="openCreate">新增测评</el-button>
      </div>
    </template>
    <el-form :model="query" inline class="query-bar">
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 160px">
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
          <el-option label="已关闭" value="closed" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <div class="admin-data-panel">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="academicYear" label="学年" />
        <el-table-column prop="term" label="学期" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="340">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="success" :disabled="row.status !== 'draft'" @click="publish(row)">发布</el-button>
            <el-button size="small" type="warning" :disabled="row.status !== 'published'" @click="close(row)">关闭</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager-wrap">
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

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑测评' : '新增测评'" width="520px">
    <div v-loading="importingCourses" element-loading-text="正在解析课程 Excel，请稍候...">
      <el-form :model="form" label-position="top">
      <el-form-item label="测评名称"><el-input v-model="form.name" /></el-form-item>
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="学年"><el-input v-model="form.academicYear" placeholder="例如：2025-2026" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="学期"><el-input v-model="form.term" placeholder="例如：2" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 100%">
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
          <el-option label="已关闭" value="closed" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!editingId" label="课程Excel导入（可选）">
        <el-upload :show-file-list="false" :before-upload="beforeImportUpload" accept=".xlsx,.xls" :disabled="saving">
          <el-button :loading="importingCourses">选择课程Excel</el-button>
        </el-upload>
        <div v-if="importFile" class="import-file-name">{{ importFile.name }}</div>
      </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">{{ importingCourses ? '导入中...' : '保存' }}</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="detailVisible" width="900px" :title="detailEvaluation ? `测评详情：${detailEvaluation.name}` : '测评详情'">
    <div v-loading="detailLoading">
      <el-empty v-if="!detailCourses.length" description="暂无课程数据" />
      <el-table v-else :data="detailCourses" size="small">
        <el-table-column prop="courseNameCn" label="课程中文名" min-width="180" />
        <el-table-column prop="courseNameEn" label="课程英文名" min-width="220" show-overflow-tooltip />
        <el-table-column prop="grade" label="年级" width="100" />
        <el-table-column label="关联班级" min-width="220">
          <template #default="{ row }">
            <span v-if="row.classNames.length">{{ row.classNames.join('，') }}</span>
            <span v-else class="empty-text">未关联</span>
          </template>
        </el-table-column>
        <el-table-column label="授课教师" min-width="220">
          <template #default="{ row }">
            <span v-if="row.teacherNames.length">{{ row.teacherNames.join('，') }}</span>
            <span v-else class="empty-text">未关联</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.page-head { display: flex; align-items: center; justify-content: space-between; }
.query-bar { margin-bottom: 12px; }
.import-file-name { margin-top: 8px; color: #6b7280; font-size: 12px; }
.empty-text { color: #9ca3af; }
.pager-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
