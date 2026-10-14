import http from './http'
export interface Category { id: number; name: string; parentId: number | null }
export interface Product {
  id: number; sellerId: number; categoryId: number; title: string; description: string
  price: number; originalPrice: number | null; conditionLevel: number; status: string
  createdAt: string; updatedAt: string; image: string | null; images: string[]
}
export interface ProductDetail {
  product: Product
  seller: { id: number; username: string; nickname: string; avatar: string | null }
  category: Category
}
export interface ProductInput {
  title: string; description: string; categoryId: number | null; price: string
  originalPrice: string | null; conditionLevel: number; imageUrls: string[]
}
export interface ProductPage { records: Product[]; total: number; page: number; size: number }
export async function getCategories() { return (await http.get<Category[]>('/categories')).data }
export async function getProducts(page: number, keyword = '', mine = false, categoryId?: number | null) {
  return (await http.get<ProductPage>(mine ? '/products/my' : '/products', { params: { page, size: 12, keyword, categoryId: mine ? undefined : categoryId ?? undefined } })).data
}
export async function getProduct(id: string | number) { return (await http.get<ProductDetail>(`/products/${id}`)).data }
export async function saveProduct(data: ProductInput, id?: string) {
  return (id ? await http.put<ProductDetail>(`/products/${id}`, data) : await http.post<ProductDetail>('/products', data)).data
}
export async function offShelf(id: number) { return (await http.patch<ProductDetail>(`/products/${id}/off-shelf`)).data }
export const conditions = ['磨损明显', '一般', '良好', '几乎全新', '全新']
export function conditionLabel(level: number) { return conditions[level - 1] ?? '未知成色' }
export function statusLabel(status: string) {
  return ({ ON_SALE: '在售', OFF_SHELF: '已下架', RESERVED: '已预订', SOLD: '已售出', PENDING_REVIEW: '待审核', REJECTED: '未通过' } as Record<string, string>)[status] ?? status
}
export function dateLabel(value: string) { return new Date(value).toLocaleString('zh-CN') }
