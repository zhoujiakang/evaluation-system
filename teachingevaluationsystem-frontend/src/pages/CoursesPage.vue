<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCourse,
  deleteCourse,
  getClasses,
  getCourses,
  getEvaluations,
  getTeachers,
  updateCourse,
  type AdminClass,
  type AdminCourse,
  type AdminEvaluation,
  type AdminTeacher,
  type CoursePayload
} from '@/services/domain'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const rows = ref<AdminCourse[]>([])
const pager = reactive({ page: 1, size: 10, total: 0 })
const classOptions = ref<AdminClass[]>([])
const teacherOptions = ref<AdminTeacher[]>([])
const evaluationOptions = ref<AdminEvaluation[]>([])
const query = reactive<{ evaluationId: number | null; grade: string }>({ evaluationId: null, grade: '' })
const form = reactive<CoursePayload>({
  evaluationId: null,
  grade: '',
  major: '',
  courseNameCn: '',
  courseNameEn: '',
  classIds: [],
  teacherIds: []
})

async function load() {
  loading.value = true
  try {
    const res = await getCourses({ ...query, page: pager.page, size: pager.size })
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
  Object.assign(query, { evaluationId: null, grade: '' })
  pager.page = 1
  load()
}

async function loadOptions() {
  const [classes, teachers, evaluations] = await Promise.all([
    getClasses({ page: 1, size: 1000 }),
    getTeachers({ page: 1, size: 1000 }),
    getEvaluations({ page: 1, size: 1000 })
  ])
  classOptions.value = classes.records
  teacherOptions.value = teachers.records
  evaluationOptions.value = evaluations.records
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { evaluationId: null, grade: '', major: '', courseNameCn: '', courseNameEn: '', classIds: [], teacherIds: [] })
  dialogVisible.value = true
}

function openEdit(row: AdminCourse) {
  editingId.value = row.id
  Object.assign(form, {
    evaluationId: null,
    grade: row.grade,
    major: '',
    courseNameCn: row.courseNameCn,
    courseNameEn: row.courseNameEn,
    classIds: [...row.classIds],
    teacherIds: [...row.teacherIds]
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateCourse(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createCourse(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: AdminCourse) {
  try {
    await ElMessageBox.confirm(`确认删除课程「${row.courseNameCn}」？`, '删除确认', { type: 'warning' })
    await deleteCourse(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(async () => {
  await loadOptions()
  await load()
})

function onPageChange(page: number) {
  pager.page = page
  load()
}

function onSizeChange(size: number) {
  pager.size = size
  pager.page = 1
  load()
}

function search() {
  pager.page = 1
  load()
}

watch(
  () => query.evaluationId,
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
        <span>课程管理</span>
        <el-button type="primary" @click="openCreate">新增课程</el-button>
      </div>
    </template>
    <el-form :model="query" inline class="query-bar">
      <el-form-item label="测评">
        <el-select v-model="query.evaluationId" clearable placeholder="全部" style="width: 220px">
          <el-option v-for="item in evaluationOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="年级"><el-input v-model="query.grade" clearable placeholder="例如：2025级" @keyup.enter="search" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <div class="admin-data-panel">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="courseNameCn" label="中文名" />
        <el-table-column prop="courseNameEn" label="英文名" />
        <el-table-column prop="grade" label="年级" />
        <el-table-column label="班级数" width="100">
          <template #default="{ row }">{{ row.classIds.length }}</template>
        </el-table-column>
        <el-table-column label="教师数" width="100">
          <template #default="{ row }">{{ row.teacherIds.length }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
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

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑课程' : '新增课程'" width="620px">
    <el-form :model="form" label-position="top">
      <el-row :gutter="12">
        <el-col :span="12" v-if="!editingId">
          <el-form-item label="所属测评">
            <el-select v-model="form.evaluationId" clearable placeholder="可选" style="width: 100%">
              <el-option v-for="item in evaluationOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12"><el-form-item label="年级"><el-input v-model="form.grade" placeholder="例如：2025级" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="课程中文名"><el-input v-model="form.courseNameCn" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="课程英文名"><el-input v-model="form.courseNameEn" /></el-form-item></el-col>
        <el-col :span="24">
          <el-form-item label="授课班级">
            <el-select v-model="form.classIds" multiple filterable style="width: 100%">
              <el-option v-for="item in classOptions" :key="item.id" :label="`${item.grade} ${item.className}`" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="授课教师">
            <el-select v-model="form.teacherIds" multiple filterable style="width: 100%">
              <el-option v-for="item in teacherOptions" :key="item.id" :label="`${item.name}（${item.teacherType}）`" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.page-head { display: flex; align-items: center; justify-content: space-between; }
.query-bar { margin-bottom: 12px; }
.pager-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
