<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUsers, setUserEnabled } from '../../api/admin'
import type { UserInfo } from '../../api/auth'
import { errorMessage } from '../../api/http'
const rows=ref<UserInfo[]>([]), page=ref(1), total=ref(0), loading=ref(false), busy=ref<number|null>(null), error=ref('')
async function load() {
  loading.value=true; error.value=''
  try { const data=await adminUsers(page.value); rows.value=data.records; total.value=data.total }
  catch(failure) { error.value=errorMessage(failure) } finally { loading.value=false }
}
async function change(user:UserInfo) {
  if(busy.value!==null || user.role==='ADMIN') return
  const enabled=user.status==='DISABLED'
  try { await ElMessageBox.confirm(`确认${enabled?'恢复':'禁用'}用户“${user.nickname}”？`,'用户状态管理',{confirmButtonText:'确认',cancelButtonText:'取消'}) } catch { return }
  busy.value=user.id
  try { await setUserEnabled(user.id,enabled); ElMessage.success(enabled?'用户已恢复':'用户已禁用'); await load() }
  catch(failure) { error.value=errorMessage(failure) } finally { busy.value=null }
}
onMounted(load)
</script>
<template>
  <div class="page-heading"><div><span class="eyebrow">COMMUNITY</span><h2>用户管理</h2><p class="page-subtitle">管理用户状态，让校园交易有序进行。</p></div><span class="count-pill">共 {{ total }} 位用户</span></div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" />
  <el-card class="admin-table-card" v-loading="loading">
    <el-table :data="rows" empty-text="暂无用户">
      <el-table-column label="用户" min-width="210"><template #default="{row}"><div class="user-cell"><span class="user-avatar">{{ row.nickname.slice(0,1) }}</span><div><strong>{{ row.nickname }}</strong><p class="muted">{{ row.username }}</p></div></div></template></el-table-column>
      <el-table-column prop="id" label="ID" width="85" />
      <el-table-column label="角色" width="120"><template #default="{row}"><el-tag :type="row.role==='ADMIN'?'warning':'info'">{{ row.role==='ADMIN'?'管理员':'普通用户' }}</el-tag></template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'danger'">{{ row.status==='ACTIVE'?'正常':'已禁用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="120" fixed="right"><template #default="{row}"><span v-if="row.role==='ADMIN'" class="muted">管理员保护</span><el-button v-else :type="row.status==='ACTIVE'?'danger':'primary'" plain size="small" :disabled="busy!==null" @click="change(row)">{{ row.status==='ACTIVE'?'禁用':'恢复' }}</el-button></template></el-table-column>
    </el-table>
  </el-card>
  <el-pagination v-if="total" v-model:current-page="page" :page-size="12" :total="total" layout="prev, pager, next" @current-change="load" />
</template>
