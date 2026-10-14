<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminProducts, deleteProduct } from '../../api/admin'
import { dateLabel, statusLabel, type Product } from '../../api/products'
import { errorMessage } from '../../api/http'
const rows=ref<Product[]>([]), page=ref(1), total=ref(0), loading=ref(false), busy=ref<number|null>(null), error=ref('')
async function load() {
  loading.value=true; error.value=''
  try { const data=await adminProducts(page.value); rows.value=data.records; total.value=data.total }
  catch(failure) { error.value=errorMessage(failure) } finally { loading.value=false }
}
async function remove(product:Product) {
  if(busy.value!==null) return
  try { await ElMessageBox.confirm(`确认删除“${product.title}”？删除后无法恢复，涉及交易的商品将保留。`,'删除违规商品',{confirmButtonText:'删除',cancelButtonText:'取消',type:'warning'}) } catch { return }
  busy.value=product.id
  try { await deleteProduct(product.id); ElMessage.success('商品已删除'); await load(); if(!rows.value.length && page.value>1) { page.value--; await load() } }
  catch(failure) { error.value=errorMessage(failure) } finally { busy.value=null }
}
onMounted(load)
</script>
<template>
  <div class="page-heading"><div><span class="eyebrow">PRODUCTS</span><h2>商品管理</h2><p class="page-subtitle">查看全部商品，维护集市内容。</p></div><span class="count-pill">共 {{ total }} 件商品</span></div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" />
  <el-card class="admin-table-card" v-loading="loading">
    <el-table :data="rows" empty-text="暂无商品">
      <el-table-column label="商品" min-width="240"><template #default="{row}"><router-link :to="`/products/${row.id}`">{{ row.title }}</router-link><p class="muted">商品 #{{ row.id }} · 发布者 #{{ row.sellerId }}</p></template></el-table-column>
      <el-table-column label="价格" width="120"><template #default="{row}"><span class="table-price">¥ {{ Number(row.price).toFixed(2) }}</span></template></el-table-column>
      <el-table-column label="状态" width="110"><template #default="{row}"><el-tag>{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="发布时间" min-width="180"><template #default="{row}">{{ dateLabel(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="100" fixed="right"><template #default="{row}"><el-button type="danger" plain size="small" :disabled="busy!==null" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
  </el-card>
  <el-pagination v-if="total" v-model:current-page="page" :page-size="12" :total="total" layout="prev, pager, next" @current-change="load" />
</template>
