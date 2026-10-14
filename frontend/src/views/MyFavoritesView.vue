<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import MarketNav from '../components/MarketNav.vue'
import { getMyFavorites, removeFavorite, type FavoriteProduct } from '../api/favorites'
import { dateLabel } from '../api/products'
import { errorMessage } from '../api/http'

const records = ref<FavoriteProduct[]>([])
const loading = ref(false), busyId = ref<number | null>(null), error = ref('')

async function load() {
  loading.value = true; error.value = ''
  try { records.value = await getMyFavorites() }
  catch (failure) { error.value = errorMessage(failure) }
  finally { loading.value = false }
}

async function remove(productId: number) {
  if (busyId.value !== null) return
  busyId.value = productId; error.value = ''
  try {
    await removeFavorite(productId)
    records.value = records.value.filter(product => product.id !== productId)
    ElMessage.success('已取消收藏')
  } catch (failure) { error.value = errorMessage(failure) }
  finally { busyId.value = null }
}

onMounted(load)
</script>

<template>
  <div class="market-page">
    <MarketNav />
    <div class="page-heading"><h2>我的收藏</h2><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div v-loading="loading" class="product-grid">
      <el-card v-for="product in records" :key="product.id" class="favorite-card">
        <router-link :to="`/products/${product.id}`">
          <el-image v-if="product.image" :src="product.image" fit="cover" class="favorite-image">
            <template #error><div class="no-image">图片无法加载</div></template>
          </el-image>
          <div v-else class="no-image favorite-image">暂无图片</div>
          <h3>{{ product.title }}</h3>
        </router-link>
        <p class="price">¥ {{ Number(product.price).toFixed(2) }}</p>
        <p class="muted">发布于 {{ dateLabel(product.createdAt) }}</p>
        <el-button :loading="busyId === product.id" :disabled="busyId !== null" @click="remove(product.id)">取消收藏</el-button>
      </el-card>
    </div>
    <el-empty v-if="!loading && !error && !records.length" description="还没有收藏商品，去逛逛吧" />
  </div>
</template>

<style scoped>
.favorite-card h3 { margin: 12px 0 6px; }
.favorite-image { width: 100%; height: 180px; }
</style>
