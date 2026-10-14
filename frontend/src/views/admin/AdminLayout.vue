<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { currentUser, type UserInfo } from '../../api/auth'
import { clearToken } from '../../auth/token'
const user = ref<UserInfo | null>(null), router = useRouter()
onMounted(async () => { user.value = await currentUser() })
function logout() { clearToken(); void router.replace('/login') }
</script>
<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand"><span class="brand-mark">闲</span><div><strong>闲集 · 管理后台</strong><small>让校园好物有序流转</small></div></div>
      <span class="sidebar-caption">工作空间</span>
      <nav class="admin-menu">
        <router-link to="/admin"><span>⌂</span>后台首页</router-link>
        <router-link to="/admin/review"><span>✓</span>商品审核</router-link>
        <router-link to="/admin/products"><span>▦</span>商品管理</router-link>
        <router-link to="/admin/users"><span>◎</span>用户管理</router-link>
      </nav>
      <div class="sidebar-bottom"><router-link to="/">← 返回交易平台</router-link><p>每一件闲置，都有新的可能。</p></div>
    </aside>
    <section class="admin-main">
      <header class="admin-topbar"><span class="eyebrow">CAMPUS MARKET / ADMIN</span><div v-if="user"><span class="admin-avatar">{{ user.nickname.slice(0,1) }}</span><span>{{ user.nickname }}</span><el-button text @click="logout">退出</el-button></div></header>
      <router-view :key="$route.path" />
    </section>
  </div>
</template>
