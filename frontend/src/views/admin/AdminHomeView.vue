<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { currentUser, type UserInfo } from '../../api/auth'
import { errorMessage } from '../../api/http'
const user = ref<UserInfo | null>(null)
const loading = ref(true), error = ref('')
onMounted(async () => {
  try { user.value = await currentUser() }
  catch (failure) { error.value = errorMessage(failure) }
  finally { loading.value = false }
})
</script>
<template>
  <el-alert v-if="error" :title="error" type="error" :closable="false" />
  <section class="admin-welcome" v-loading="loading">
    <span class="eyebrow">让好物继续被使用</span>
    <h2>你好，{{ user?.nickname || '管理员' }}。</h2>
    <p>欢迎来到二手商品交易平台管理后台。</p>
    <div class="welcome-detail" v-if="user"><span>账号：{{ user.username }}</span><el-tag>ADMIN · 管理员</el-tag></div>
  </section>
  <div class="admin-shortcuts">
    <router-link to="/admin/review"><span class="shortcut-icon">✓</span><h3>审核校园好物</h3><p>查看新发布的商品，通过或拒绝审核。</p><span class="shortcut-arrow">进入审核 →</span></router-link>
    <router-link to="/admin/products"><span class="shortcut-icon">▦</span><h3>维护商品内容</h3><p>浏览所有商品，处理违规发布内容。</p><span class="shortcut-arrow">管理商品 →</span></router-link>
    <router-link to="/admin/users"><span class="shortcut-icon">◎</span><h3>管理用户账号</h3><p>查看用户，禁用或恢复普通用户账号。</p><span class="shortcut-arrow">管理用户 →</span></router-link>
  </div>
</template>
