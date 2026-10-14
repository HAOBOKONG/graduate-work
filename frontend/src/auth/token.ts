const TOKEN_KEY = 'secondhand_market_token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const saveToken = (token: string) => localStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)
