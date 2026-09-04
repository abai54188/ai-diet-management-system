<template>
  <div>
    <el-card shadow="never">
      <template #header>用户管理</template>
      <!-- 搜索栏 -->
      <el-form :inline="true" class="search-bar">
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="用户名/昵称" clearable style="width: 200px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 130px">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="正常" :value="0" />
            <el-option label="已禁用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="users" v-loading="loading" size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
              {{ row.status === 1 ? '已禁用' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" min-width="200">
          <template #default="{ row }">
            <el-button size="small" :type="row.status === 1 ? 'success' : 'warning'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-button>
            <el-button size="small" :type="row.role === 'ADMIN' ? 'info' : 'danger'" @click="toggleRole(row)">
              {{ row.role === 'ADMIN' ? '降为用户' : '设为管理员' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="page.current" :page-size="page.size" :total="page.total"
        layout="total, prev, pager, next" class="table-pagination" @current-change="load" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUserListApi, adminUserStatusApi, adminUserRoleApi } from '@/api/community'

const users = ref([])
const loading = ref(false)
const query = reactive({ keyword: '', role: null, status: null })
const page = reactive({ current: 1, size: 10, total: 0 })

onMounted(load)

/**
 * 加载用户列表
 */
async function load() {
  loading.value = true
  try {
    const res = await adminUserListApi({
      current: page.current, size: page.size,
      keyword: query.keyword || undefined,
      role: query.role || undefined,
      status: query.status ?? undefined
    })
    users.value = res.data.records
    page.total = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

/**
 * 禁用/启用用户
 */
async function toggleStatus(row) {
  const action = row.status === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？`, '提示', { type: 'warning' })
  await adminUserStatusApi(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`已${action}`)
  await load()
}

/**
 * 角色分配
 */
async function toggleRole(row) {
  const action = row.role === 'ADMIN' ? '降为普通用户' : '提升为管理员'
  await ElMessageBox.confirm(`确定将「${row.username}」${action}吗？`, '权限变更', { type: 'warning' })
  await adminUserRoleApi(row.id, row.role === 'ADMIN' ? 'USER' : 'ADMIN')
  ElMessage.success('角色已变更')
  await load()
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 6px;
}

.table-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>