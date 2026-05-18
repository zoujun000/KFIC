<template>
  <div class="register-container">
    <el-card class="register-card">
      <div class="register-header">
        <el-icon size="40" color="#67C23A"><Ship /></el-icon>
        <h2>注册账号</h2>
        <p>货代管理系统 · 用户注册</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="姓名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号" :prefix-icon="Avatar" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码"
            :prefix-icon="Lock" show-password @keyup.enter="handleRegister" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码"
            :prefix-icon="Lock" show-password @keyup.enter="handleRegister" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" style="width:100%" :loading="loading" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="bottom-link">
        已有账号？<router-link to="/login">返回登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Avatar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  realName: '',
  username: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await authApi.register({
      realName: form.realName,
      username: form.username,
      password: form.password
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #2d8f5e 0%, #1a6b40 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.register-card {
  width: 420px;
  max-width: 92vw;
  border-radius: 12px;
  padding: 20px;
}
.register-header {
  text-align: center;
  margin-bottom: 30px;
}
.register-header h2 {
  margin: 12px 0 4px;
  font-size: 22px;
  color: #303133;
}
.register-header p {
  color: #909399;
  font-size: 13px;
  margin: 0;
}
.bottom-link {
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.bottom-link a {
  color: #67C23A;
  text-decoration: none;
}

@media (max-width: 768px) {
  .register-card {
    width: 94vw;
    padding: 16px;
    border-radius: 10px;
  }
  .register-header h2 { font-size: 20px; }
  .register-header { margin-bottom: 20px; }
}
</style>
