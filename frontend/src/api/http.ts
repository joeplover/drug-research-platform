import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 180000
})

http.interceptors.request.use((config) => {
  const raw = localStorage.getItem('drug-research-user-session')
  if (raw) {
    try {
      const session = JSON.parse(raw)
      if (session.token) {
        config.headers.Authorization = `Bearer ${session.token}`
      }
    } catch {
      // ignore
    }
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message ?? error.message ?? 'Request failed'
    return Promise.reject(new Error(message))
  }
)

export default http
