import http from './http'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string | null
  role: string
  status: string
}

export interface LoginResult { token: string; user: UserInfo }

export async function register(username: string, password: string, nickname: string) {
  return (await http.post<UserInfo>('/auth/register', { username, password, nickname })).data
}

export async function login(username: string, password: string) {
  return (await http.post<LoginResult>('/auth/login', { username, password })).data
}

export async function currentUser() {
  return (await http.get<UserInfo>('/users/me')).data
}
