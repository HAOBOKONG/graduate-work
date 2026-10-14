<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { currentUser } from '../api/auth'
import { clearToken, getToken } from '../auth/token'
const router = useRouter()
const nickname = ref('')
const isAdmin = ref(false)
onMounted(async () => { if (getToken()) { try { const user=await currentUser(); nickname.value=user.nickname; isAdmin.value=user.role==='ADMIN' } catch { /* Axios处理401 */ } } })
function logout() { clearToken(); void router.replace('/login') }
</script>
<template>
  <nav class="market-nav">
    <router-link to="/">商品首页</router-link>
    <router-link to="/products/new">发布商品</router-link>
    <router-link to="/products/my">我的商品</router-link>
    <router-link to="/favorites">我的收藏</router-link>
    <router-link to="/orders/my">我的订单</router-link>
    <router-link v-if="isAdmin" to="/admin">管理后台 ↗</router-link>
    <span class="nav-account" v-if="nickname">{{ nickname }} <el-button size="small" @click="logout">退出登录</el-button></span>
    <router-link v-else to="/login" class="nav-account">登录 / 注册</router-link>
  </nav>
</template>
