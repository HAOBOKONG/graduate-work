import http from './http'
import type { ProductPage } from './products'
import type { UserInfo } from './auth'
export interface UserPage { records: UserInfo[]; total: number; page: number; size: number }
export async function adminProducts(page: number, pending = false) {
  return (await http.get<ProductPage>(pending ? '/admin/products/pending' : '/admin/products', {params:{page,size:12}})).data
}
export async function reviewProduct(id: number, approve: boolean) {
  await http.patch(`/admin/products/${id}/${approve ? 'approve' : 'reject'}`)
}
export async function deleteProduct(id: number) { await http.delete(`/admin/products/${id}`) }
export async function adminUsers(page: number) { return (await http.get<UserPage>('/admin/users', {params:{page,size:12}})).data }
export async function setUserEnabled(id: number, enabled: boolean) { await http.patch(`/admin/users/${id}/${enabled ? 'enable' : 'disable'}`) }
