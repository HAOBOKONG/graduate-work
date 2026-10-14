<script setup lang="ts">
import { conditionLabel, dateLabel, type Product } from '../api/products'
defineProps<{ product: Product }>()
</script>
<template>
  <router-link :to="`/products/${product.id}`" class="product-card">
    <div class="product-media"><el-image v-if="product.image" :src="product.image" fit="cover" class="product-image">
      <template #error><div class="no-image">图片无法加载</div></template>
    </el-image>
    <div v-else class="no-image">暂无图片</div><span class="media-pill">{{ conditionLabel(product.conditionLevel) }}</span></div>
    <div class="product-body">
      <h3>{{ product.title }}</h3>
      <div class="product-card-meta"><strong class="price">¥ {{ Number(product.price).toFixed(2) }}</strong><span class="card-arrow">↗</span></div>
      <p class="muted">{{ dateLabel(product.createdAt) }} 发布</p>
    </div>
  </router-link>
</template>
