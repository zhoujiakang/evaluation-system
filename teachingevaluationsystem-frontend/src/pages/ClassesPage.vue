<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createClass, deleteClass, getClasses, updateClass, type AdminClass, type ClassPayload } from '@/services/domain'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const rows = ref<AdminClass[]>([])
const pager = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({ className: '', major: '', grade: '' })
const form = reactive<ClassPayload>({ className: '', major: '', grade: '' })

async function load() {
  loading.value = true
  try {
    const res = await getClasses({ ...query, page: pager.page, size: pager.size })
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
  Object.assign(query, { className: '', major: '', grade: '' })
  pager.page = 1
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { className: '', major: '', grade: '' })
  dialogVisible.value = true
}

function openEdit(row: AdminClass) {
  editingId.value = row.id
  Object.assign(form, { className: row.className, major: row.major, grade: row.grade })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingId.value) {
      await updateClass(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createClass(form)
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

async function remove(row: AdminClass) {
  try {
    await ElMessageBox.confirm(`确认删除班级「${row.className}」？`, '删除确认', { type: 'warning' })
    await deleteClass(row.id)
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
</script>

<template>
  <el-card>
    <template #header>
      <div class="page-head">
        <span>班级管理</span>
        <el-button type="primary" @click="openCreate">新增班级</el-button>
      </div>
    </template>
    <el-form :model="query" inline class="query-bar">
      <el-form-item label="班级"><el-input v-model="query.className" clearable placeholder="班级名称" @keyup.enter="search" /></el-form-item>
      <el-form-item label="专业"><el-input v-model="query.major" clearable placeholder="专业" @keyup.enter="search" /></el-form-item>
      <el-form-item label="年级"><el-input v-model="query.grade" clearable placeholder="例如：2025级" @keyup.enter="search" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <div class="admin-data-panel">
      <el-table :data="rows" v-loading="loading">
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

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑班级' : '新增班级'" width="460px">
    <el-form :model="form" label-position="top">
      <el-form-item label="班级名称"><el-input v-model="form.className" /></el-form-item>
      <el-form-item label="专业"><el-input v-model="form.major" /></el-form-item>
      <el-form-item label="年级"><el-input v-model="form.grade" placeholder="例如：2025级" /></el-form-item>
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
