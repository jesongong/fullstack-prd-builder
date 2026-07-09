import axios from 'axios'
import { message } from 'ant-design-vue'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 30000
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      message.error(res.message || 'Request failed')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data
  },
  error => {
    message.error(error.message || 'Network error')
    return Promise.reject(error)
  }
)

export default service
