<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MarketNav from '../components/MarketNav.vue'
import { getMyOrders, orderStatusLabel, type MyOrders } from '../api/orders'
import { dateLabel } from '../api/products'
import { errorMessage } from '../api/http'
const orders = ref<MyOrders>({purchases: [], sales: []}), tab = ref('purchases'), loading = ref(false), error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { orders.value = await getMyOrders() }
  catch (failure) { error.value = errorMessage(failure) }
  finally { loading.value = false }
}
onMounted(load)
</script>
<template>
  <div class="market-page">
    <MarketNav />
    <div class="page-heading"><h2>我的订单</h2><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-tabs v-model="tab" v-loading="loading">
      <el-tab-pane v-for="kind in (['purchases', 'sales'] as const)" :key="kind" :name="kind" :label="`${kind === 'purchases' ? '购买订单' : '出售订单'}（${orders[kind].length}）`">
        <el-card v-for="order in orders[kind]" :key="order.id" class="order-card">
          <div class="page-heading"><router-link :to="`/orders/${order.id}`">{{ order.productTitleSnapshot }}</router-link><el-tag>{{ orderStatusLabel(order.status) }}</el-tag></div>
          <p class="price">¥ {{ Number(order.priceSnapshot).toFixed(2) }}</p>
          <p>订单号：{{ order.orderNo }}</p>
          <p>{{ kind === 'purchases' ? '卖家' : '买家' }}：{{ kind === 'purchases' ? order.seller.nickname : order.buyer.nickname }}</p>
          <p>创建时间：{{ dateLabel(order.createdAt) }}</p>
          <router-link :to="`/orders/${order.id}`">查看订单</router-link>
        </el-card>
        <el-empty v-if="!loading && !error && !orders[kind].length" description="暂无订单" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
