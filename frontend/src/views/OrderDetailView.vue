<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarketNav from '../components/MarketNav.vue'
import { currentUser } from '../api/auth'
import { getOrder, orderAction, orderStatusLabel, type Order } from '../api/orders'
import { dateLabel } from '../api/products'
import { errorMessage } from '../api/http'
const route = useRoute(), order = ref<Order | null>(null), userId = ref<number | null>(null)
const loading = ref(false), busy = ref(false), error = ref('')
const buyer = computed(() => order.value?.buyerId === userId.value)
const seller = computed(() => order.value?.sellerId === userId.value)
async function load() {
  loading.value = true; error.value = ''
  try { const [data, user] = await Promise.all([getOrder(String(route.params.id)), currentUser()]); order.value = data; userId.value = user.id }
  catch (failure) { error.value = errorMessage(failure) }
  finally { loading.value = false }
}
async function act(action: 'pay' | 'ship' | 'complete') {
  if (!order.value || busy.value) return
  const messages = {pay: '本操作为模拟支付，不会实际扣款。确认支付？', ship: '确认商品已发货？', complete: '确认已收到商品？确认后交易完成。'}
  try { await ElMessageBox.confirm(messages[action], '确认订单操作', {confirmButtonText: '确认', cancelButtonText: '取消'}) } catch { return }
  busy.value = true; error.value = ''
  try { order.value = await orderAction(order.value.id, action); ElMessage.success('订单状态已更新') }
  catch (failure) { error.value = errorMessage(failure) }
  finally { busy.value = false }
}
onMounted(load)
</script>
<template>
  <div class="market-page">
    <MarketNav />
    <div class="page-heading"><h2>订单详情</h2><el-button :loading="loading" :disabled="busy" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-card v-loading="loading" class="order-detail">
      <template v-if="order">
        <h3>{{ order.productTitleSnapshot }}</h3>
        <el-tag>{{ orderStatusLabel(order.status) }}</el-tag>
        <p class="price">¥ {{ Number(order.priceSnapshot).toFixed(2) }}</p>
        <p>订单号：{{ order.orderNo }}</p>
        <p>买家：{{ order.buyer.nickname }}（{{ order.buyer.username }}）</p>
        <p>卖家：{{ order.seller.nickname }}（{{ order.seller.username }}）</p>
        <p>创建时间：{{ dateLabel(order.createdAt) }}</p>
        <p>更新时间：{{ dateLabel(order.updatedAt) }}</p>
        <p><router-link :to="`/products/${order.productId}`">查看商品</router-link></p>
        <p class="muted">本平台使用模拟支付，不发生实际扣款。</p>
        <div class="order-actions">
          <el-button v-if="buyer && order.status === 'WAIT_PAY'" type="primary" :loading="busy" @click="act('pay')">模拟支付</el-button>
          <el-button v-if="seller && order.status === 'PAID'" type="primary" :loading="busy" @click="act('ship')">发货</el-button>
          <el-button v-if="buyer && order.status === 'SHIPPED'" type="success" :loading="busy" @click="act('complete')">确认收货</el-button>
          <p v-if="seller && order.status === 'WAIT_PAY'">等待买家支付</p>
          <p v-if="buyer && order.status === 'PAID'">等待卖家发货</p>
          <p v-if="seller && order.status === 'SHIPPED'">等待买家确认收货</p>
          <p v-if="order.status === 'COMPLETED'">交易已完成</p>
        </div>
      </template>
    </el-card>
  </div>
</template>
