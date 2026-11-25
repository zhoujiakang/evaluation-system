<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElLoading, ElMessage, ElMessageBox } from 'element-plus'
import type { UploadProps } from 'element-plus'
import {
  createStudent,
  deleteStudent,
  getStudents,
  importStudents,
  updateStudent,
  type AdminStudent,
  type StudentCreatePayload
} from '@/services/domain'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const importing = ref(false)
const rows = ref<AdminStudent[]>([])
const pager = reactive({ page: 1, size: 10, total: 0 })
const query = reactive<{ className: string; enrollYear: number | null; studentNo: string; name: string }>({
  className: '',
  enrollYear: null,
  studentNo: '',
  name: ''
})
const form = reactive<StudentCreatePayload>({
  username: '',
  name: '',
  password: '',
  studentNo: '',
  className: '',
  major: '',
  grade: ''
})

async function load() {
  loading.value = true
  try {
    const res = await getStudents({ ...query, page: pager.page, size: pager.size })
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
  Object.assign(query, { className: '', enrollYear: null, studentNo: '', name: '' })
  pager.page = 1
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', name: '', password: '', studentNo: '', className: '', major: '', grade: '' })
  dialogVisible.value = true
}

function openEdit(row: AdminStudent) {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    name: row.name,
    password: '',
    studentNo: row.studentNo,
    className: row.className,
    major: row.major,
    grade: row.grade
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateStudent(editingId.value, {
        username: form.username,
        name: form.name,
        studentNo: form.studentNo,
        className: form.className,
        major: form.major,
        grade: form.grade
      })
      ElMessage.success('更新成功')
    } else {
      await createStudent(form)
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

async function remove(row: AdminStudent) {
  try {
    await ElMessageBox.confirm(`确认删除学生「${row.name}」？`, '删除确认', { type: 'warning' })
    await deleteStudent(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || (ext !== 'xlsx' && ext !== 'xls')) {
    ElMessage.error('仅支持 .xlsx 或 .xls 文件')
    return false
  }
  if (importing.value) return false
  importing.value = true
  const loading = ElLoading.service({
    lock: true,
    text: '正在导入学生数据...',
    background: 'rgba(255, 255, 255, 0.65)'
  })
  try {
    const result = await importStudents(file)
    ElMessage.success(
      `导入完成：成功 ${result.successRows}，跳过 ${result.skippedRows}，新增学生 ${result.createdStudents}，更新学生 ${result.updatedStudents}`
    )
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败')
  } finally {
    importing.value = false
    loading.close()
  }
  return false
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

function search() {
  pager.page = 1
  load()
}
</script>

<template>
  <el-card>
    <template #header>
      <div class="page-head">
        <span>学生管理</span>
        <div class="head-actions">
          <el-upload :show-file-list="false" :before-upload="beforeUpload" accept=".xlsx,.xls" :disabled="importing">
            <el-button :loading="importing">{{ importing ? '导入中...' : 'Excel导入' }}</el-button>
          </el-upload>
          <el-button type="primary" @click="openCreate">新增学生</el-button>
        </div>
      </div>
    </template>
    <el-form :model="query" inline class="query-bar">
      <el-form-item label="学号"><el-input v-model="query.studentNo" clearable placeholder="学号" @keyup.enter="search" /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="query.name" clearable placeholder="姓名" @keyup.enter="search" /></el-form-item>
      <el-form-item label="班级"><el-input v-model="query.className" clearable placeholder="班级名称" @keyup.enter="search" /></el-form-item>
      <el-form-item label="入学年"><el-input-number v-model="query.enrollYear" :min="2000" :max="2100" controls-position="right" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <div class="admin-data-panel">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="studentNo" label="学号" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="major" label="专业" />
        <el-table-column prop="grade" label="年级" />
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

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑学生' : '新增学生'" width="560px">
    <el-form :model="form" label-position="top">
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="账号"><el-input v-model="form.username" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item></el-col>
        <el-col :span="12" v-if="!editingId"><el-form-item label="初始密码"><el-input v-model="form.password" type="password" show-password /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="学号"><el-input v-model="form.studentNo" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="班级"><el-input v-model="form.className" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="专业"><el-input v-model="form.major" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="年级"><el-input v-model="form.grade" placeholder="例如：2025级" /></el-form-item></el-col>
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
.head-actions { display: flex; align-items: center; gap: 8px; }
.pager-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
