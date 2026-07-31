<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="login-wrapper">
      <!-- 左侧品牌区 -->
      <div class="brand-panel">
        <div class="brand-content">
          <div class="brand-icon">
            <el-icon :size="56"><Ship /></el-icon>
          </div>
          <h1>货代管理系统</h1>
          <p>Freight Management System</p>
          <div class="brand-divider"></div>
          <p class="brand-desc">高效 · 专业 · 可靠</p>
        </div>
        <div class="brand-footer">
          <span>崴航（广州）国际货运代理有限公司</span>
        </div>
      </div>

      <!-- 右侧登录区 -->
      <div class="login-panel">
        <div class="login-inner">
          <div class="login-header">
            <h2>欢迎回来</h2>
            <p>请登录您的账户</p>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            class="login-form"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                size="large"
                class="custom-input"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                size="large"
                class="custom-input"
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="bottom-link">
            还没有账号？<router-link to="/register">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
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
/* ============ 整体布局 ============ */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0c1e3d 0%, #102a4c 30%, #0f3b6e 60%, #0a2a5e 100%);
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', sans-serif;
}

/* ============ 背景装饰 ============ */
.bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.06;
  background: #fff;
}
.shape-1 {
  width: 600px;
  height: 600px;
  top: -200px;
  right: -100px;
  animation: float 20s ease-in-out infinite;
}
.shape-2 {
  width: 400px;
  height: 400px;
  bottom: -120px;
  left: -80px;
  animation: float 16s ease-in-out infinite reverse;
}
.shape-3 {
  width: 200px;
  height: 200px;
  top: 40%;
  left: 45%;
  animation: float 12s ease-in-out infinite;
}
@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

/* ============ 登录卡片容器 ============ */
.login-wrapper {
  display: flex;
  width: 900px;
  max-width: 94vw;
  min-height: 520px;
  background: rgba(255, 255, 255, 0.97);
  border-radius: 20px;
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.3),
    0 8px 30px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
}

/* ============ 左侧品牌区 ============ */
.brand-panel {
  width: 48%;
  background: linear-gradient(160deg, #1a56db 0%, #1e40af 40%, #0f2b6b 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 40px;
  position: relative;
  overflow: hidden;
}
.brand-panel::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 40%, rgba(255,255,255,0.08) 0%, transparent 50%);
}
.brand-content {
  text-align: center;
  color: #fff;
  position: relative;
  z-index: 1;
}
.brand-icon {
  width: 90px;
  height: 90px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 28px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: transform 0.3s ease;
}
.brand-icon:hover {
  transform: scale(1.05) rotate(-3deg);
}
.brand-icon :deep(.el-icon) {
  color: #fff;
}
.brand-content h1 {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 3px;
  margin: 0 0 12px;
}
.brand-content > p {
  font-size: 13px;
  opacity: 0.7;
  letter-spacing: 2px;
  margin: 0;
  text-transform: uppercase;
}
.brand-divider {
  width: 50px;
  height: 3px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 2px;
  margin: 24px auto;
}
.brand-desc {
  font-size: 15px !important;
  opacity: 0.9 !important;
  letter-spacing: 4px !important;
  text-transform: none !important;
}
.brand-footer {
  position: absolute;
  bottom: 24px;
  left: 0;
  right: 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  letter-spacing: 1px;
}

/* ============ 右侧登录区 ============ */
.login-panel {
  width: 52%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 50px 48px;
}
.login-inner {
  width: 100%;
  max-width: 340px;
}
.login-header {
  margin-bottom: 36px;
}
.login-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}
.login-header p {
  font-size: 14px;
  color: #80889e;
  margin: 0;
}

/* ============ 表单样式 ============ */
.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}
.login-form :deep(.el-form-item__error) {
  font-size: 12px;
  padding-top: 4px;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e4e7ed inset;
  transition: all 0.3s ease;
  padding: 1px 15px;
  background: #f7f8fc;
}
.custom-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c6cde0 inset;
}
.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #1a56db40 inset, 0 0 0 1px #1a56db inset;
  background: #fff;
}
.custom-input :deep(.el-input__inner) {
  height: 46px;
  font-size: 15px;
}
.custom-input :deep(.el-input__prefix) {
  color: #a0aec0;
  margin-right: 4px;
}

/* ============ 登录按钮 ============ */
.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 3px;
  background: linear-gradient(135deg, #1a56db 0%, #1e40af 100%);
  border: none;
  transition: all 0.3s ease;
  margin-top: 8px;
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(26, 86, 219, 0.4);
}
.login-btn:active {
  transform: translateY(0);
}
.login-btn :deep(.el-icon) {
  margin-right: 6px;
}

/* ============ 底部链接 ============ */
.bottom-link {
  text-align: center;
  color: #80889e;
  font-size: 14px;
  margin-top: 16px;
}
.bottom-link a {
  color: #1a56db;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}
.bottom-link a:hover {
  color: #1e40af;
}

/* ============ 移动端适配 ============ */
@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
    width: 92vw;
    min-height: auto;
    border-radius: 16px;
  }
  .brand-panel {
    width: 100%;
    padding: 32px 24px;
  }
  .brand-content h1 {
    font-size: 22px;
    letter-spacing: 2px;
  }
  .brand-icon {
    width: 64px;
    height: 64px;
    border-radius: 18px;
    margin-bottom: 20px;
  }
  .brand-icon :deep(.el-icon) {
    font-size: 36px !important;
  }
  .brand-divider {
    margin: 16px auto;
  }
  .brand-footer {
    display: none;
  }
  .login-panel {
    width: 100%;
    padding: 32px 28px 40px;
  }
  .login-inner {
    max-width: 100%;
  }
  .login-header h2 {
    font-size: 22px;
  }
  .login-btn {
    height: 46px;
    font-size: 15px;
  }
}
</style>
