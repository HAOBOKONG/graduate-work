<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { currentUser } from '../api/auth'
import { getToken } from '../auth/token'
import { createOrder } from '../api/orders'
import { addFavorite, getMyFavorites, removeFavorite } from '../api/favorites'
import MarketNav from '../components/MarketNav.vue'
import { getProduct, conditionLabel, dateLabel, statusLabel, type ProductDetail } from '../api/products'
import { errorMessage } from '../api/http'
const router = useRouter(), userId = ref<number | null>(null), buying = ref(false), favoriteBusy = ref(false), favorited = ref(false)
const route = useRoute(), detail = ref<ProductDetail | null>(null), loading = ref(false), error = ref('')
watch(() => route.params.id, async (id) => {
  loading.value = true; error.value = ''; detail.value = null; userId.value = null; favorited.value = false
  try {
    detail.value = await getProduct(String(id))
    if (getToken()) {
      const [user, favorites] = await Promise.all([currentUser(), getMyFavorites()])
      userId.value = user.id
      favorited.value = favorites.some(product => product.id === detail.value?.product.id)
    }
  } catch (failure) { error.value = errorMessage(failure) }
  finally { loading.value = false }
}, { immediate: true })
async function toggleFavorite() {
  if (!getToken()) { await router.push('/login'); return }
  if (!detail.value || favoriteBusy.value) return
  favoriteBusy.value = true; error.value = ''
  try {
    if (favorited.value) {
      await removeFavorite(detail.value.product.id)
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(detail.value.product.id)
      favorited.value = true
      ElMessage.success('收藏成功')
    }
  } catch (failure) { error.value = errorMessage(failure) }
  finally { favoriteBusy.value = false }
}
async function buy() {
  if (!getToken()) { await router.push('/login'); return }
  if (!detail.value || buying.value) return
  buying.value = true; error.value = ''
  try {
    const order = await createOrder(detail.value.product.id)
    detail.value.product.status = 'RESERVED'
    ElMessage.success('订单已创建，请进行模拟支付')
    await router.push(`/orders/${order.id}`)
  } catch (failure) { error.value = errorMessage(failure) }
  finally { buying.value = false }
}
</script>
<template>
  <div class="market-page">
    <MarketNav />
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <router-link class="back-link" to="/">← 返回好物列表</router-link>
    <el-card v-loading="loading" class="detail-card">
      <div v-if="detail" class="detail-layout">
        <div class="detail-gallery">
          <div class="detail-images" v-if="detail.product.images.length">
            <el-image v-for="(image, index) in detail.product.images" :key="index" :src="image" fit="contain" :preview-src-list="detail.product.images" :initial-index="index">
              <template #error><div class="no-image">图片无法加载</div></template>
            </el-image>
          </div>
          <div class="no-image" v-else>暂无图片</div>
          <small>点击图片可放大查看</small>
        </div>
        <div class="detail-info">
          <div class="detail-tags"><span>{{ detail.category.name }}</span><span>{{ conditionLabel(detail.product.conditionLevel) }}</span><span>{{ statusLabel(detail.product.status) }}</span></div>
          <h2>{{ detail.product.title }}</h2>
          <p class="detail-price price">¥ {{ Number(detail.product.price).toFixed(2) }}</p>
          <p v-if="detail.product.originalPrice !== null" class="muted">原价 ¥ {{ Number(detail.product.originalPrice).toFixed(2) }}</p>
          <div class="detail-facts"><div><span>发布者</span><strong>{{ detail.seller.nickname }}</strong></div><div><span>发布时间</span><strong>{{ dateLabel(detail.product.createdAt) }}</strong></div></div>
          <div class="detail-actions"><el-button :type="favorited ? 'warning' : 'default'" :loading="favoriteBusy" :disabled="loading" @click="toggleFavorite">{{ favorited ? '♥ 已收藏' : '♡ 收藏' }}</el-button>
          <el-button v-if="detail.product.status === 'ON_SALE' && detail.product.sellerId !== userId" type="primary" :loading="buying" :disabled="loading" @click="buy">立即购买</el-button></div>
          <p v-if="detail.product.sellerId === userId" class="muted">这是你发布的商品</p>
          <p v-else-if="detail.product.status !== 'ON_SALE'" class="muted">商品当前无法购买（{{ statusLabel(detail.product.status) }}）</p>
        </div>
        <section class="detail-description"><span class="eyebrow">ABOUT THIS FIND</span><h3>关于这件好物</h3><p class="description">{{ detail.product.description }}</p></section>
      </div>
    </el-card>
  </div>
</template>
