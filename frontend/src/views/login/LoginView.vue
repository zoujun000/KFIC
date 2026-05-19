<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-header">
        <el-icon size="40" color="#409EFF"><Ship /></el-icon>
        <h2>货代管理系统</h2>
        <p>Freight Management System</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码"
            :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="bottom-link">
        没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a6cf5 0%, #0a4bbd 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-card {
  width: 420px;
  max-width: 92vw;
  border-radius: 12px;
  padding: 20px;
}
.login-header {
  text-align: center;
  margin-bottom: 30px;
}
.login-header h2 {
  margin: 12px 0 4px;
  font-size: 22px;
  color: #303133;
}
.login-header p {
  color: #909399;
  font-size: 13px;
  margin: 0;
}
.bottom-link {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin-top: 8px;
}
.bottom-link a {
  color: #409EFF;
  text-decoration: none;
}

@media (max-width: 768px) {
  .login-card {
    width: 94vw;
    padding: 16px;
    border-radius: 10px;
  }
  .login-header h2 { font-size: 20px; }
  .login-header { margin-bottom: 20px; }
}
</style>
