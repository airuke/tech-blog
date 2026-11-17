import axios from 'axios'
export const tokenKey = 'airuike.blog.accessToken'
export const getToken = () => localStorage.getItem(tokenKey) || ''
const http = axios.create({ baseURL: '/api', timeout: 15000 })
http.interceptors.request.use((config) => {
  const token = config.public ? '' : getToken()
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})
http.interceptors.response.use(
  (response) => {
    if (response.data?.code === 0) return response.data.data
    const error = new Error(response.data?.message || '请求失败')
    error.status = response.status
    return Promise.reject(error)
  },
  (original) => {
    const status = original.response?.status || 0
    const error = new Error(
      original.response?.data?.message ||
        (status >= 500
          ? '服务暂时不可用，请稍后重试'
          : status
            ? '请求未能完成'
            : '无法连接服务器，请确认后端已启动'),
    )
    error.status = status
    return Promise.reject(error)
  },
)
export const publicGet = (url, params) =>
  http.get(url, { params, public: true })
export const api = {
  status: () => publicGet('/auth/status'),
  setup: (body) => http.post('/auth/setup', body, { public: true }),
  login: (body) => http.post('/auth/login', body, { public: true }),
  me: () => http.get('/auth/me'),
  password: (body) => http.put('/auth/password', body),
  site: () => publicGet('/site'),
  publicArticles: (params) => publicGet('/articles', params),
  publicArticle: (id) => publicGet(`/articles/${id}`),
  publicComments: (id, params) => publicGet(`/articles/${id}/comments`, params),
  submitComment: (id, body) =>
    http.post(`/articles/${id}/comments`, body, { public: true }),
  comments: (params) => http.get('/admin/comments', { params }),
  reviewComment: (id, status, version) =>
    http.put(`/admin/comments/${id}/review`, { status, version }),
  replyComment: (id, content, version) =>
    http.put(`/admin/comments/${id}/reply`, { content, version }),
  deleteComment: (id, version) =>
    http.delete(`/admin/comments/${id}`, { params: { version } }),
  taxonomy: (kind, admin = false) =>
    admin ? http.get(`/admin/${kind}`) : publicGet(`/${kind}`),
  archives: () => publicGet('/archives'),
  stats: () => http.get('/admin/stats'),
  articles: (params) => http.get('/admin/articles', { params }),
  article: (id) => http.get(`/admin/articles/${id}`),
  saveArticle: (id, body) =>
    id
      ? http.put(`/admin/articles/${id}`, body)
      : http.post('/admin/articles', body),
  publication: (id, body) =>
    http.put(`/admin/articles/${id}/publication`, body),
  favorite: (id, body) => http.put(`/admin/articles/${id}/favorite`, body),
  deleteArticle: (id, version) =>
    http.delete(`/admin/articles/${id}`, { params: { version } }),
  saveTaxonomy: (kind, id, body) =>
    id
      ? http.put(`/admin/${kind}/${id}`, body)
      : http.post(`/admin/${kind}`, body),
  deleteTaxonomy: (kind, id) => http.delete(`/admin/${kind}/${id}`),
  files: (params) => http.get('/admin/files', { params }),
  file: (id) => http.get(`/admin/files/${id}`),
  upload: (file) => {
    const form = new FormData()
    form.append('file', file)
    return http.post('/admin/files/upload', form, { timeout: 120000 })
  },
  fileVisibility: (id, isPublic) =>
    http.put(`/admin/files/${id}/visibility`, { isPublic }),
  deleteFile: (id) => http.delete(`/admin/files/${id}`),
  saveSite: (body) => http.put('/admin/site', body),
}
