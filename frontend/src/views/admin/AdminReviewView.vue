<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminProducts, reviewProduct } from '../../api/admin'
import { dateLabel, conditionLabel, type Product } from '../../api/products'
import { errorMessage } from '../../api/http'
const rows=ref<Product[]>([]), page=ref(1), total=ref(0), loading=ref(false), busy=ref<number|null>(null), error=ref('')
async function load() {
  loading.value=true; error.value=''
  try { const data=await adminProducts(page.value,true); rows.value=data.records; total.value=data.total }
  catch(failure) { error.value=errorMessage(failure) } finally { loading.value=false }
}
async function review(id:number, approve:boolean) {
  if(busy.value!==null) return
  busy.value=id
  try { await reviewProduct(id,approve); ElMessage.success(approve?'审核通过，商品已上架':'商品已拒绝'); await load(); if(!rows.value.length && page.value>1) { page.value--; await load() } }
  catch(failure) { error.value=errorMessage(failure) } finally { busy.value=null }
}
onMounted(load)
</script>
<template>
  <div class="page-heading"><div><span class="eyebrow">CONTENT REVIEW</span><h2>商品审核</h2><p class="page-subtitle">为每一件进入校园集市的好物把关。</p></div><el-tag>{{ total }} 件待审核</el-tag></div>
  <el-alert v-if="error" :title="error" type="error" :closable="false" />
  <div class="review-list" v-loading="loading">
    <el-card v-for="product in rows" :key="product.id" class="review-card">
      <div class="review-cover"><el-image v-if="product.image" :src="product.image" fit="cover"><template #error><div class="no-image">图片无法加载</div></template></el-image><div v-else class="no-image">暂无图片</div></div>
      <div class="review-content"><div class="page-heading"><h3>{{ product.title }}</h3><strong class="price">¥ {{ Number(product.price).toFixed(2) }}</strong></div><p class="description">{{ product.description }}</p><p class="muted">{{ conditionLabel(product.conditionLevel) }} · 发布者 #{{ product.sellerId }} · {{ dateLabel(product.createdAt) }}</p><router-link :to="`/products/${product.id}`">查看详情 ↗</router-link></div>
      <div class="review-actions"><el-button type="primary" :disabled="busy!==null" @click="review(product.id,true)">通过</el-button><el-button plain type="danger" :disabled="busy!==null" @click="review(product.id,false)">拒绝</el-button></div>
    </el-card>
    <el-empty v-if="!loading && !error && !rows.length" description="审核队列已清空，暂时没有待审核商品" />
  </div>
  <el-pagination v-if="total" v-model:current-page="page" :page-size="12" :total="total" layout="prev, pager, next" @current-change="load" />
</template>
