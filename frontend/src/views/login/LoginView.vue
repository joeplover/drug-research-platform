<template>
  <div class="login-page">
    <div class="login-card panel auth-card">
      <div class="section-title auth-card__header">
        <div>
          <h2>药物研发辅助平台</h2>
        </div>
      </div>

      <el-alert
        title="演示默认账号：admin@aiforaso.local / 123456"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-position="top" @submit.prevent="handleLogin">
            <el-form-item label="邮箱">
              <el-input v-model="loginForm.email" placeholder="请输入登录邮箱" size="large" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" size="large" />
            </el-form-item>
            <div class="tag-list" style="margin-bottom: 12px">
              <el-tag
                v-for="item in quickAccounts"
                :key="item.email"
                class="clickable-tag"
                effect="plain"
                @click="fillQuickAccount(item.email, item.password)"
              >
                {{ item.email }} / {{ item.password }}
              </el-tag>
            </div>
            <div class="action-row">
              <el-button type="primary" size="large" :loading="submitting" @click="handleLogin">登录进入平台</el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" label-position="top" @submit.prevent="handleRegister">
            <el-form-item label="用户名">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" size="large" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="registerForm.email" placeholder="请输入注册邮箱" size="large" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="至少 6 位" size="large" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" size="large" />
            </el-form-item>
            <div class="action-row">
              <el-button type="primary" size="large" :loading="submitting" @click="handleRegister">注册并进入平台</el-button>
            </div>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const submitting = ref(false)

const loginForm = reactive({
  email: 'admin@aiforaso.local',
  password: '123456'
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const quickAccounts = [
  { email: 'admin@aiforaso.local', password: '123456' },
  { email: 'reviewer@aiforaso.local', password: '123456' }
]

function fillQuickAccount(email: string, password: string) {
  loginForm.email = email
  loginForm.password = password
}

async function handleLogin() {
  if (!loginForm.email || !loginForm.password) {
    ElMessage.warning('请输入邮箱和密码')
    return
  }
  submitting.value = true
  try {
    await userStore.login(loginForm.email, loginForm.password)
    ElMessage.success('登录成功')
    router.push('/literatures')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    submitting.value = false
  }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.email || !registerForm.password) {
    ElMessage.warning('请完整填写注册信息')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码长度不能少于 6 位')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    await userStore.register(registerForm.username, registerForm.email, registerForm.password)
    ElMessage.success('注册成功，已自动登录')
    router.push('/literatures')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    submitting.value = false
  }
}
</script>
