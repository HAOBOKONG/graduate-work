import http from './http'

export async function uploadImage(file: File): Promise<string> {
  const body = new FormData()
  body.append('file', file)
  return (await http.post<{ url: string }>('/uploads/images', body)).data.url
}
