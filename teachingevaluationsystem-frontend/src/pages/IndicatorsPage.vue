<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createIndicator,
  deleteIndicator,
  getIndicators,
  updateIndicator,
  type AdminIndicator,
  type IndicatorPayload
} from '@/services/domain'

type TeacherType = 'CN' | 'INTL'
type ViewMode = 'graph' | 'table'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<string | number | null>(null)
const teacherType = ref<TeacherType>('CN')
const viewMode = ref<ViewMode>('graph')
const rows = ref<AdminIndicator[]>([])
const selectedId = ref<string | null>(null)
const expandedParentIds = ref<string[]>([])

const graphContainer = ref<HTMLElement | null>(null)
let mindMap: any = null
let mindMapLib: any = null
let resizeObserver: ResizeObserver | null = null
let fitTimer: number | null = null

const form = reactive<IndicatorPayload>({
  teacherType: 'CN',
  level: 1,
  parentId: null,
  name: '',
  nameEn: '',
  weight: 0.1,
  sortOrder: 1,
  isActive: 1
})

const parentOptions = computed(() => rows.value.filter((item) => item.level === 1))
const selected = computed(() => rows.value.find((item) => idKey(item.id) === selectedId.value) || null)
const level1Count = computed(() => rows.value.filter((item) => item.level === 1).length)
const level2Count = computed(() => rows.value.filter((item) => item.level === 2).length)
const activeCount = computed(() => rows.value.filter((item) => item.isActive === 1).length)
const tableRef = ref()
function idKey(value: string | number | null | undefined) {
  if (value === null || value === undefined) return ''
  return String(value)
}

type IndicatorTreeRow = AdminIndicator & { treeKey: string; children?: IndicatorTreeRow[] }
const treeRows = computed<IndicatorTreeRow[]>(() => {
  const parents = rows.value.filter((item) => item.level === 1).sort((a, b) => a.sortOrder - b.sortOrder)
  const children = rows.value.filter((item) => item.level === 2).sort((a, b) => a.sortOrder - b.sortOrder)
  return parents.map((parent, pIdx) => ({
    ...parent,
    treeKey: `p_${idKey(parent.id)}_${pIdx}`,
    children: children.filter((child) => idKey(child.parentId) === idKey(parent.id)).map((child, cIdx) => ({
      ...child,
      treeKey: `c_${idKey(child.id)}_${idKey(parent.id)}_${cIdx}`
    }))
  }))
})

function labelOf(item: AdminIndicator) {
  const suffix = item.level === 1 && item.weight !== null ? ` (${item.weight})` : ''
  return `${item.name}${suffix}`
}

function ellipsis(text: string, max = 48) {
  if (!text) return ''
  return text.length > max ? `${text.slice(0, max - 1)}...` : text
}

function displayLabel(item: AdminIndicator) {
  return ellipsis(labelOf(item), 48)
}

function wrapTwoLines(text: string, maxPerLine = 18) {
  if (!text) return ''
  const clean = text.trim()
  if (clean.length <= maxPerLine) return clean
  const first = clean.slice(0, maxPerLine)
  const secondRaw = clean.slice(maxPerLine)
  const second = secondRaw.length > maxPerLine ? `${secondRaw.slice(0, maxPerLine - 1)}...` : secondRaw
  return `${first}\n${second}`
}

function createMindMapData() {
  const level1 = rows.value.filter((item) => item.level === 1).sort((a, b) => a.sortOrder - b.sortOrder)
  const level2 = rows.value.filter((item) => item.level === 2).sort((a, b) => a.sortOrder - b.sortOrder)

  const childrenMap = new Map<string, AdminIndicator[]>()
  for (const node of level2) {
    const pid = idKey(node.parentId)
    if (!pid) continue
    const arr = childrenMap.get(pid) || []
    arr.push(node)
    childrenMap.set(pid, arr)
  }

  const rootChildren = level1.map((p) => {
    const parentKey = idKey(p.id)
    const expanded = expandedParentIds.value.includes(parentKey)
    const children = (childrenMap.get(parentKey) || []).map((c) => ({
      data: {
        uid: `L2_${idKey(c.id)}_P_${idKey(c.parentId)}`,
        bizId: idKey(c.id),
        level: 2,
        text: wrapTwoLines(displayLabel(c)),
        fullText: labelOf(c),
        style: {
          fillColor: '#eef2ff',
          borderColor: '#4c6ef5',
          borderWidth: 2,
          color: '#1f2937'
        }
      },
      children: []
    }))
    return {
      data: {
        uid: `L1_${idKey(p.id)}`,
        bizId: idKey(p.id),
        level: 1,
        text: wrapTwoLines(displayLabel(p)),
        fullText: labelOf(p),
        style: {
          fillColor: '#eaf7ee',
          borderColor: '#2f9e44',
          borderWidth: 2,
          color: '#1f2937'
        },
        expand: expanded
      },
      children
    }
  })

  return {
    data: {
      uid: `ROOT_${teacherType.value}`,
      level: 0,
      text: teacherType.value === 'CN' ? '中方指标体系' : '外方指标体系',
      style: {
        fillColor: '#f4f4f5',
        borderColor: '#7a7a7a',
        borderWidth: 2,
        color: '#374151'
      },
      expand: true
    },
    children: rootChildren
  }
}

async function load() {
  loading.value = true
  try {
    rows.value = (await getIndicators({ teacherType: teacherType.value })).records
    expandedParentIds.value = []
    if (selectedId.value && !rows.value.some((item) => idKey(item.id) === selectedId.value)) selectedId.value = null
    if (viewMode.value === 'graph') await renderChart()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    teacherType: teacherType.value,
    level: 1,
    parentId: null,
    name: '',
    nameEn: '',
    weight: 0.1,
    sortOrder: 1,
    isActive: 1
  })
  dialogVisible.value = true
}

function openEdit(row: AdminIndicator) {
  editingId.value = row.id
  Object.assign(form, {
    teacherType: row.teacherType,
    level: row.level,
    parentId: row.parentId,
    name: row.name,
    nameEn: row.nameEn || '',
    weight: row.weight,
    sortOrder: row.sortOrder,
    isActive: row.isActive
  })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload: IndicatorPayload = {
      teacherType: form.teacherType,
      level: form.level,
      parentId: form.level === 1 ? null : form.parentId,
      name: form.name,
      nameEn: form.nameEn || null,
      weight: form.level === 1 ? form.weight : null,
      sortOrder: form.sortOrder,
      isActive: form.isActive
    }
    if (editingId.value) {
      await updateIndicator(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createIndicator(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: AdminIndicator) {
  try {
    await ElMessageBox.confirm(`确认删除指标「${row.name}」？`, '删除确认', { type: 'warning' })
    await deleteIndicator(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

function onTableRowClick(row: IndicatorTreeRow) {
  selectedId.value = idKey(row.id)
  if (row.level === 1 && tableRef.value) {
    tableRef.value.toggleRowExpansion(row)
  }
}

async function ensureEcharts() {
  if (mindMapLib) return mindMapLib
  const mod = await import('simple-mind-map')
  mindMapLib = mod.default || mod
  return mindMapLib
}

function scheduleFit(delay = 20) {
  if (!mindMap) return
  if (fitTimer) window.clearTimeout(fitTimer)
  fitTimer = window.setTimeout(() => {
    if (!mindMap) return
    try {
      mindMap.view.fit(undefined, false, 20)
    } catch (error) {
      // ignore fit transient errors during rapid updates
    }
  }, delay)
}

function onGraphResize() {
  if (!mindMap) return
  mindMap.resize()
  scheduleFit(0)
}

function disposeMindMap() {
  if (fitTimer) {
    window.clearTimeout(fitTimer)
    fitTimer = null
  }
  resizeObserver?.disconnect()
  resizeObserver = null
  if (mindMap) {
    mindMap.destroy()
    mindMap = null
  }
}

async function renderChart() {
  await nextTick()
  if (!graphContainer.value) return
  const MindMap = await ensureEcharts()
  const graphData = createMindMapData()

  if (!mindMap) {
    mindMap = new MindMap({
      el: graphContainer.value,
      data: graphData,
      layout: 'logicalStructure',
      readonly: true,
      mousewheelAction: 'zoom',
      disableMouseWheelZoom: true,
      disableTouchZoom: true,
      isDisableDrag: true,
      fit: false,
      fitPadding: 20,
      textAutoWrapWidth: 220,
      minZoomRatio: 30,
      maxZoomRatio: 300,
      expandBtnSize: 18,
      customCheckEnableShortcut: () => false,
      mousedownEventPreventDefault: true
    })

    mindMap.on('node_click', (node: any) => {
      const data = node?.getData?.() || {}
      const level = Number(data?.level ?? -1)
      const bizId = idKey(data?.bizId)
      const fullText = data?.fullText
      if (fullText) {
        node.group?.node?.setAttribute?.('title', fullText)
      }
      if (!bizId || level < 1) return
      selectedId.value = bizId
      if (level === 1) {
        const exists = expandedParentIds.value.includes(bizId)
        expandedParentIds.value = exists
          ? expandedParentIds.value.filter((id) => id !== bizId)
          : [...expandedParentIds.value, bizId]
      }
    })

    mindMap.on('node_tree_render_end', () => {
      scheduleFit(0)
    })

    resizeObserver = new ResizeObserver(onGraphResize)
    resizeObserver.observe(graphContainer.value)
  } else {
    mindMap.setData(graphData)
  }

  scheduleFit(0)
}

watch(teacherType, () => {
  form.teacherType = teacherType.value
  selectedId.value = null
  if (viewMode.value === 'graph') {
    disposeMindMap()
  }
  load()
})

watch(viewMode, async (mode) => {
  if (mode === 'graph') {
    await renderChart()
    return
  }
  disposeMindMap()
})

onMounted(load)

onBeforeUnmount(() => {
  disposeMindMap()
})
</script>

<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="page-head">
        <div class="title-row">
          <span>指标管理</span>
          <el-tag>一级 {{ level1Count }}</el-tag>
          <el-tag type="info">二级 {{ level2Count }}</el-tag>
          <el-tag type="success">启用 {{ activeCount }}</el-tag>
        </div>
        <div class="actions">
          <el-segmented v-model="teacherType" :options="[{ label: '中方', value: 'CN' }, { label: '外方', value: 'INTL' }]" />
          <el-segmented v-model="viewMode" :options="[{ label: '图结构', value: 'graph' }, { label: '表结构', value: 'table' }]" />
          <el-button type="primary" @click="openCreate">新增指标</el-button>
        </div>
      </div>
    </template>

    <div v-if="viewMode === 'graph'" ref="graphContainer" class="graph-area"></div>

    <el-table
      v-else
      ref="tableRef"
      :data="treeRows"
      row-key="treeKey"
      size="small"
      :tree-props="{ children: 'children' }"
      @row-click="onTableRowClick"
    >
      <el-table-column prop="name" label="中文名" min-width="260" show-overflow-tooltip />
      <el-table-column prop="nameEn" label="英文名" min-width="280" show-overflow-tooltip />
      <el-table-column prop="level" label="级别" width="70" />
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column prop="weight" label="权重" width="90" />
      <el-table-column prop="isActive" label="启用" width="80">
        <template #default="{ row }">{{ row.isActive === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="selected" class="selected-info">
      <span>当前选中：{{ selected.name }}</span>
      <span>（{{ selected.level === 1 ? '一级' : '二级' }}）</span>
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑指标' : '新增指标'" width="560px">
    <el-form :model="form" label-position="top">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="体系">
            <el-select v-model="form.teacherType" style="width: 100%">
              <el-option label="中方" value="CN" />
              <el-option label="外方" value="INTL" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="级别">
            <el-select v-model="form.level" style="width: 100%">
              <el-option label="一级指标" :value="1" />
              <el-option label="二级指标" :value="2" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item v-if="form.level === 2" label="父级指标">
            <el-select v-model="form.parentId" clearable placeholder="请选择父级指标" style="width: 100%">
              <el-option v-for="item in parentOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24"><el-form-item label="中文名"><el-input v-model="form.name" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="英文名"><el-input v-model="form.nameEn" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="1" style="width: 100%" /></el-form-item>
        </el-col>
        <el-col :span="12" v-if="form.level === 1">
          <el-form-item label="权重"><el-input-number v-model="form.weight" :min="0" :max="1" :step="0.01" style="width: 100%" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="启用">
            <el-select v-model="form.isActive" style="width: 100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
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
.page-head { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; }
.title-row { display: flex; align-items: center; gap: 10px; font-size: 15px; font-weight: 600; }
.actions { display: flex; align-items: center; gap: 10px; }
.graph-area {
  width: 100%;
  height: clamp(420px, calc(100vh - 300px), 760px);
  max-height: calc(100vh - 300px);
  min-height: 420px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  overflow: hidden;
  background:
    linear-gradient(90deg, #f1f5f9 1px, transparent 1px) 0 0 / 20px 20px,
    linear-gradient(#f1f5f9 1px, transparent 1px) 0 0 / 20px 20px,
    #fcfdff;
}

@media (max-height: 820px) {
  .graph-area {
    height: clamp(360px, calc(100vh - 270px), 620px);
    max-height: calc(100vh - 270px);
    min-height: 360px;
  }
}
.selected-info { margin-top: 12px; color: #475569; display: flex; gap: 6px; }
</style>
