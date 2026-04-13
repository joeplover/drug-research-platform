<template>
  <div class="page-grid">
    <div class="split-grid">
      <div class="panel">
        <div class="section-title">
          <div>
            <h2>新增用户</h2>
          </div>
        </div>
        <el-form :model="createForm" label-position="top" class="form-grid">
          <el-form-item label="用户名">
            <el-input v-model="createForm.username" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="createForm.email" />
          </el-form-item>
          <el-form-item label="初始密码">
            <el-input v-model="createForm.password" type="password" show-password />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="createForm.role" style="width: 100%">
              <el-option label="ADMIN" value="ADMIN" />
              <el-option label="RESEARCHER" value="RESEARCHER" />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="action-row">
          <el-button type="primary" :loading="creating" @click="handleCreate">创建用户</el-button>
        </div>
      </div>

      <div class="panel">
        <div class="section-title">
          <div>
            <h2>当前会话</h2>
          </div>
        </div>
        <JsonPanel :value="userStore.session" />
      </div>
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>用户列表</h2>
        </div>
        <el-button type="primary" @click="loadUsers">刷新用户</el-button>
      </div>
      <el-table :data="users" stripe>
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column label="角色" min-width="140">
          <template #default="{ row }">
            <el-select v-model="row.role" style="width: 120px">
              <el-option label="ADMIN" value="ADMIN" />
              <el-option label="RESEARCHER" value="RESEARCHER" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="140">
          <template #default="{ row }">
            <el-select v-model="row.status" style="width: 120px">
              <el-option label="ACTIVE" value="ACTIVE" />
              <el-option label="DISABLED" value="DISABLED" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleUpdate(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import JsonPanel from '@/components/JsonPanel.vue'
import { createUser, listUsers, updateUserRole } from '@/api/modules/users'
import { useUserStore } from '@/stores/user'
import type { UserView } from '@/types/user'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()
const users = ref<UserView[]>([])
const creating = ref(false)

const createForm = reactive({
  username: '',
  email: '',
  password: '123456',
  role: 'RESEARCHER'
})

async function loadUsers() {
  try {
    users.value = await listUsers()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function handleCreate() {
  if (!createForm.username || !createForm.email || !createForm.password) {
    ElMessage.warning('请填写用户名、邮箱和初始密码')
    return
  }
  if (createForm.password.length < 6) {
    ElMessage.warning('初始密码长度不能少于 6 位')
    return
  }
  creating.value = true
  try {
    await createUser({ ...createForm })
    ElMessage.success('用户创建成功')
    Object.assign(createForm, {
      username: '',
      email: '',
      password: '123456',
      role: 'RESEARCHER'
    })
    await loadUsers()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    creating.value = false
  }
}

async function handleUpdate(row: UserView) {
  try {
    await updateUserRole(row.id, {
      role: row.role,
      status: row.status
    })
    ElMessage.success('用户信息已更新')
    await loadUsers()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

onMounted(loadUsers)
</script>
