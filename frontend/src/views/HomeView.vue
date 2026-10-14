<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MarketNav from '../components/MarketNav.vue'
import ProductCard from '../components/ProductCard.vue'
import { getCategories, getProducts, type Category, type Product } from '../api/products'
import { errorMessage } from '../api/http'
const records = ref<Product[]>([]), categories = ref<Category[]>([]), categoryId = ref<number | null>(null), page = ref(1), total = ref(0), keyword = ref('')
const loading = ref(false), error = ref('')
let requestVersion = 0
async function load() {
  const version = ++requestVersion
  loading.value = true; error.value = ''
  try { const data = await getProducts(page.value, keyword.value, false, categoryId.value); if (version === requestVersion) { records.value = data.records; total.value = data.total } }
  catch (failure) { if (version === requestVersion) error.value = errorMessage(failure) }
  finally { if (version === requestVersion) loading.value = false }
}
function search() { page.value = 1; void load() }
onMounted(() => { void load(); void getCategories().then(data => { categories.value = data }).catch(failure => { error.value = errorMessage(failure) }) })
</script>
<template>
  <div class="market-page">
    <MarketNav />
    <section class="campus-hero"><div><span class="eyebrow">CAMPUS FINDS / 校园好物</span><h2>让闲置，遇见<br /><em>下一位主人。</em></h2><p>课本、数码、生活小物……把用不到的，交给用得上的人。</p><router-link to="/products/new" class="hero-link">发布我的闲置 <span>↗</span></router-link><div class="hero-small-note">随手发布 · 审核上架 · 安心交易</div></div><div class="hero-art" aria-hidden="true"><span class="hero-orbit"></span><span class="hero-book">好物<br />新生</span><span class="hero-sticker">REUSE<br />& RELIVE</span><span class="hero-spark">✦</span></div></section>
    <section class="market-highlights" aria-label="交易流程"><div><span>01</span><strong>发现好物</strong><small>从同学的闲置里寻找心动之选</small></div><div><span>02</span><strong>轻松发布</strong><small>上传照片，写下商品的真实状态</small></div><div><span>03</span><strong>安心流转</strong><small>审核上架，订单状态清晰可见</small></div></section>
    <div class="page-heading"><div><span class="eyebrow">FRESH ON CAMPUS</span><h2>发现闲置好物</h2></div><span class="count-pill">{{ total }} 件好物正在流转</span></div>
    <form class="search-bar" @submit.prevent="search">
      <span class="search-mark" aria-hidden="true">⌕</span>
      <el-input v-model="keyword" placeholder="搜索商品标题或描述" maxlength="200" clearable aria-label="关键词" />
      <el-select v-model="categoryId" placeholder="全部分类" clearable aria-label="商品分类" @change="search"><el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" /></el-select>
      <el-button type="primary" native-type="submit">搜索</el-button>
    </form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div v-loading="loading" class="product-grid"><ProductCard v-for="product in records" :key="product.id" :product="product" /></div>
    <el-empty v-if="!loading && !error && !records.length" description="暂无商品，快来发布第一件闲置吧" />
    <el-pagination v-if="total" v-model:current-page="page" :page-size="12" :total="total" layout="prev, pager, next" @current-change="load" />
  </div>
</template>
