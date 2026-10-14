import http from './http'

export interface FavoriteProduct {
  id: number
  title: string
  price: number
  image: string | null
  createdAt: string
}

export async function getMyFavorites() {
  return (await http.get<FavoriteProduct[]>('/favorites/my')).data
}

export async function addFavorite(productId: number) {
  await http.post(`/favorites/${productId}`)
}

export async function removeFavorite(productId: number) {
  await http.delete(`/favorites/${productId}`)
}
