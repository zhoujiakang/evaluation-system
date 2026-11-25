<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminUsers, grantAdminPermission, revokeAdminPermission, type AdminUser } from '@/services/domain'

const loading = ref(false)
const rows = ref<AdminUser[]>([])
const pager = reactive({ page: 1, size: 10, total: 0 })
const query = reactive<{ keyword: string }>({ keyword: '' })

async function load() {
  loading.value = true
  try {
    const res = await getAdminUsers({ ...query, isAdmin: 1, page: pager.page, size: pager.size })
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
  query.keyword = ''
  pager.page = 1
  load()
}

async function addAdmin() {
  try {
    const { value } = await ElMessageBox.prompt('请输入要授权的用户账号（username）', '添加管理员', {
      confirmButtonText: '授权',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：zhangsan',
      inputValidator: (val) => {
        if (!String(val || '').trim()) return '账号不能为空'
        return true
      }
    })
    const username = String(value || '').trim()
    const matched = await getAdminUsers({ keyword: username, page: 1, size: 100 })
    const exact = matched.records.find((item) => item.username === username)
    if (!exact) {
      ElMessage.error('未找到该账号对应的用户')
      return
    }
    if (exact.isAdmin) {
      ElMessage.info('该用户已是管理员')
      return
    }
    await grantAdminPermission(exact.userId)
    ElMessage.success('授权成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '授权失败')
  }
}

async function grant(row: AdminUser) {
  try {
    await ElMessageBox.confirm(`确认授权「${row.name}」为管理员？`, '授权确认', { type: 'warning' })
    await grantAdminPermission(row.userId)
    ElMessage.success('授权成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '授权失败')
  }
}

async function revoke(row: AdminUser) {
  try {
    await ElMessageBox.confirm(`确认取消「${row.name}」的管理员权限？`, '取消确认', { type: 'warning' })
    await revokeAdminPermission(row.userId)
    ElMessage.success('取消成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '取消失败')
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
        <span>管理员管理</span>
        <el-button type="primary" @click="addAdmin">添加管理员</el-button>
      </div>
    </template>
    <el-form :model="query" inline class="query-bar">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="账号/姓名" @keyup.enter="search" />
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
        <el-table-column prop="role" label="角色" width="90">
          <template #default="{ row }">{{ row.role === 'teacher' ? '教师' : '学生' }}</template>
        </el-table-column>
        <el-table-column prop="isAdmin" label="管理员" width="90">
          <template #default="{ row }">{{ row.isAdmin ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="grantedAt" label="授权时间" width="180" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="!row.isAdmin" size="small" type="success" @click="grant(row)">授权</el-button>
            <el-button v-else size="small" type="danger" @click="revoke(row)">取消授权</el-button>
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
</template>

<style scoped>
.page-head { display: flex; align-items: center; justify-content: space-between; }
.query-bar { margin-bottom: 12px; }
.pager-wrap { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
