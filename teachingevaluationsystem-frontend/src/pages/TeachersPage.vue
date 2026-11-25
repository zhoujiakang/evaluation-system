<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createTeacher, deleteTeacher, getTeachers, updateTeacher, type AdminTeacher, type TeacherCreatePayload } from '@/services/domain'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const rows = ref<AdminTeacher[]>([])
const pager = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({ teacherType: '', name: '' })
const form = reactive<TeacherCreatePayload>({ username: '', name: '', password: '', teacherType: 'CN' })

async function load() {
  loading.value = true
  try {
    const res = await getTeachers({ ...query, page: pager.page, size: pager.size })
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
  query.teacherType = ''
  query.name = ''
  pager.page = 1
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', name: '', password: '', teacherType: 'CN' })
  dialogVisible.value = true
}

function openEdit(row: AdminTeacher) {
  editingId.value = row.id
  Object.assign(form, { username: row.username, name: row.name, password: '', teacherType: row.teacherType })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateTeacher(editingId.value, { username: form.username, name: form.name, teacherType: form.teacherType })
      ElMessage.success('更新成功')
    } else {
      await createTeacher(form)
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

async function remove(row: AdminTeacher) {
  try {
    await ElMessageBox.confirm(`确认删除教师「${row.name}」？`, '删除确认', { type: 'warning' })
    await deleteTeacher(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
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

function search() {
  pager.page = 1
  load()
}

watch(
  () => query.teacherType,
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
        <span>教师管理</span>
        <el-button type="primary" @click="openCreate">新增教师</el-button>
      </div>
    </template>
    <el-form :model="query" inline class="query-bar">
      <el-form-item label="姓名">
        <el-input v-model="query.name" clearable placeholder="教师姓名" @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="教师类型">
        <el-select v-model="query.teacherType" clearable placeholder="全部" style="width: 160px">
          <el-option label="中方教师" value="CN" />
          <el-option label="外方教师" value="INTL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <div class="admin-data-panel">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="teacherType" label="类型" />
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

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑教师' : '新增教师'" width="460px">
    <el-form :model="form" label-position="top">
      <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
      <el-form-item v-if="!editingId" label="初始密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      <el-form-item label="教师类型">
        <el-select v-model="form.teacherType" style="width: 100%">
          <el-option label="中方教师" value="CN" />
          <el-option label="外方教师" value="INTL" />
        </el-select>
      </el-form-item>
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
