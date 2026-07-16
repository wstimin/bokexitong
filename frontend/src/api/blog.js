import http from './http'

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data),
  sendEmailCode: (data) => http.post('/auth/email-code', data),
  resetPassword: (data) => http.post('/auth/reset-password', data)
}

export const userApi = {
  me: () => http.get('/users/me'),
  updateProfile: (data) => http.put('/users/me', data),
  changePassword: (data) => http.put('/users/me/password', data),
  favorites: (params) => http.get('/users/me/favorites', { params }),
  comments: (params) => http.get('/users/me/comments', { params })
}

export const portalApi = {
  home: () => http.get('/portal/home'),
  articles: (params) => http.get('/portal/articles', { params }),
  detail: (id) => http.get(`/portal/articles/${id}`)
}

export const installApi = {
  status: () => http.get('/install/status'),
  install: (data) => http.post('/install', data)
}

export const articleApi = {
  page: (params) => http.get('/articles', { params }),
  adminDetail: (id) => http.get(`/articles/${id}`),
  adminSave: (data) => http.post('/articles/admin', data),
  adminUpdate: (id, data) => http.put(`/articles/admin/${id}`, data),
  updateRecommendation: (id, recommended, recommendSort) => http.put(`/articles/${id}/recommendation`, null, { params: { recommended, recommendSort } }),
  mine: (params) => http.get('/articles/mine', { params }),
  save: (data) => http.post('/articles', data),
  update: (id, data) => http.put(`/articles/${id}`, data),
  removeMine: (id) => http.delete(`/articles/${id}`),
  remove: (ids) => http.delete('/articles', { data: ids }),
  updateStatus: (id, status, reason) => http.put(`/articles/${id}/status`, null, { params: { status, reason } }),
  like: (id) => http.post(`/articles/${id}/like`),
  favorite: (id) => http.post(`/articles/${id}/favorite`)
}

export const adminApi = {
  dashboard: () => http.get('/admin/dashboard'),
  settings: () => http.get('/admin/settings'),
  saveSettings: (data) => http.put('/admin/settings', data),
  siteSettings: () => http.get('/admin/site-settings'),
  saveSiteSettings: (data) => http.put('/admin/site-settings', data),
  mailSettings: () => http.get('/admin/mail-settings'),
  saveMailSettings: (data) => http.put('/admin/mail-settings', data),
  testMail: (data) => http.post('/admin/mail-settings/test-mail', data),
  operationLogs: (params) => http.get('/admin/operation-logs', { params }),
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
  createUser: (data) => http.post('/admin/users', data),
  userStatus: (id, status) => http.put(`/admin/users/${id}/status`, null, { params: { status } }),
  updateUser: (id, data) => http.put(`/admin/users/${id}`, data),
  resetUserPassword: (id, data) => http.put(`/admin/users/${id}/password`, data),
  deleteUser: (id) => http.delete(`/admin/users/${id}`),
  comments: (params) => http.get('/admin/comments', { params }),
  auditComment: (id, status, reason) => http.put(`/admin/comments/${id}/status`, null, { params: { status, reason } }),
  deleteComment: (id) => http.delete(`/admin/comments/${id}`)
}

export const commentApi = {
  page: (params) => http.get('/comments', { params }),
  save: (data) => http.post('/comments', data)
}

export const uploadApi = {
  file: (input) => {
    const file = input?.file || input?.target?.files?.[0] || input
    const form = new FormData()
    if (!file) throw new Error('未选择上传文件')
    form.append('file', file, file.name || 'file')
    return http.post('/uploads', form)
  }
}
