import http from './http'

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data)
}

export const portalApi = {
  home: () => http.get('/portal/home'),
  articles: (params) => http.get('/portal/articles', { params }),
  detail: (id) => http.get(`/portal/articles/${id}`)
}

export const articleApi = {
  page: (params) => http.get('/articles', { params }),
  save: (data) => http.post('/articles', data),
  update: (id, data) => http.put(`/articles/${id}`, data),
  remove: (ids) => http.delete('/articles', { data: ids }),
  like: (id) => http.post(`/articles/${id}/like`),
  favorite: (id) => http.post(`/articles/${id}/favorite`)
}

export const adminApi = {
  dashboard: () => http.get('/admin/dashboard'),
  categories: (params) => http.get('/admin/categories', { params }),
  saveCategory: (data) => http.post('/admin/categories', data),
  deleteCategory: (id) => http.delete(`/admin/categories/${id}`),
  tags: (params) => http.get('/admin/tags', { params }),
  saveTag: (data) => http.post('/admin/tags', data),
  deleteTag: (id) => http.delete(`/admin/tags/${id}`),
  images: (params) => http.get('/admin/images', { params }),
  saveImage: (data) => http.post('/admin/images', data),
  deleteImage: (id) => http.delete(`/admin/images/${id}`),
  users: (params) => http.get('/admin/users', { params }),
  userStatus: (id, status) => http.put(`/admin/users/${id}/status`, null, { params: { status } }),
  deleteUser: (id) => http.delete(`/admin/users/${id}`),
  comments: (params) => http.get('/admin/comments', { params }),
  auditComment: (id, status) => http.put(`/admin/comments/${id}/status`, null, { params: { status } }),
  deleteComment: (id) => http.delete(`/admin/comments/${id}`)
}

export const commentApi = {
  page: (params) => http.get('/comments', { params }),
  save: (data) => http.post('/comments', data)
}
