<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarketNav from '../components/MarketNav.vue'
import ProductCard from '../components/ProductCard.vue'
import { getProducts, offShelf, statusLabel, type Product } from '../api/products'
import { errorMessage } from '../api/http'
const records = ref<Product[]>([]), page = ref(1), total = ref(0), loading = ref(false), error = ref(''), busyId = ref<number | null>(null)
async function load() {
  loading.value = true; error.value = ''
  try { const data = await getProducts(page.value, '', true); records.value = data.records; total.value = data.total }
  catch (failure) { error.value = errorMessage(failure) }
  finally { loading.value = false }
}
async function remove(product: Product) {
  try { await ElMessageBox.confirm(`确认下架“${product.title}”？`, '下架商品', { confirmButtonText: '下架', cancelButtonText: '取消' }) } catch { return }
  busyId.value = product.id
  try { await offShelf(product.id); ElMessage.success('商品已下架'); await load() }
  catch (failure) { error.value = errorMessage(failure) }
  finally { busyId.value = null }
}
onMounted(load)
</script>
<template>
  <div class="market-page">
    <MarketNav />
    <div class="page-heading"><h2>我的商品</h2><router-link to="/products/new">发布新商品</router-link></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="product-grid" v-loading="loading">
      <div v-for="product in records" :key="product.id">
        <ProductCard :product="product" />
        <div class="product-actions">
          <el-tag>{{ statusLabel(product.status) }}</el-tag>
          <router-link v-if="['ON_SALE', 'OFF_SHELF', 'PENDING_REVIEW', 'REJECTED'].includes(product.status)" :to="`/products/${product.id}/edit`">修改</router-link>
          <el-button v-if="product.status === 'ON_SALE'" size="small" type="danger" :loading="busyId === product.id" :disabled="busyId !== null" @click="remove(product)">下架</el-button>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && !error && !records.length" description="你还没有发布商品" />
    <el-pagination v-if="total" v-model:current-page="page" :page-size="12" :total="total" layout="prev, pager, next" @current-change="load" />
  </div>
</template>
