import http from './http'
export interface OrderUser { id: number; username: string; nickname: string }
export interface Order {
  id: number; orderNo: string; buyerId: number; sellerId: number; productId: number
  productTitleSnapshot: string; priceSnapshot: number; status: string
  createdAt: string; updatedAt: string; buyer: OrderUser; seller: OrderUser
}
export interface MyOrders { purchases: Order[]; sales: Order[] }
export async function createOrder(productId: number) { return (await http.post<Order>('/orders', {productId})).data }
export async function getMyOrders() { return (await http.get<MyOrders>('/orders/my')).data }
export async function getOrder(id: string) { return (await http.get<Order>(`/orders/${id}`)).data }
export async function orderAction(id: number, action: 'pay' | 'ship' | 'complete') {
  return (await http.patch<Order>(`/orders/${id}/${action}`)).data
}
export function orderStatusLabel(status: string) {
  return ({ WAIT_PAY: '待支付', PAID: '已支付，待发货', SHIPPED: '已发货，待收货', COMPLETED: '交易完成', CANCELLED: '已取消' } as Record<string,string>)[status] ?? status
}
