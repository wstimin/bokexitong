<template>
  <div class="editor-grid" style="padding: 0">
    <section class="admin-card">
      <div class="toolbar">
        <h2>分类管理</h2>
        <div class="hero-actions">
          <button v-if="category.id" class="btn-ghost" type="button" @click="resetCategory">取消编辑</button>
          <button class="btn-primary" type="button" @click="saveCategory">{{ category.id ? '保存分类' : '新增分类' }}</button>
        </div>
      </div>
      <el-form label-position="top">
        <el-form-item label="分类名称"><el-input v-model="category.name" placeholder="例如：技术笔记" /></el-form-item>
        <el-form-item label="分类描述"><el-input v-model="category.description" placeholder="这个分类主要收录什么内容" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="category.sort" :min="0" /></el-form-item>
      </el-form>
      <el-table :data="categories" border>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="操作" width="170">
          <template #default="{ row }"><div class="action-row"><el-button size="small" @click="editCategory(row)">编辑</el-button><el-button size="small" type="danger" @click="removeCategory(row.id)">删除</el-button></div></template>
        </el-table-column>
      </el-table>
    </section>

    <section class="admin-card">
      <div class="toolbar">
        <h2>标签管理</h2>
        <div class="hero-actions">
          <button v-if="tag.id" class="btn-ghost" type="button" @click="resetTag">取消编辑</button>
          <button class="btn-primary" type="button" @click="saveTag">{{ tag.id ? '保存标签' : '新增标签' }}</button>
        </div>
      </div>
      <el-form label-position="top">
        <el-form-item label="标签名称"><el-input v-model="tag.name" placeholder="例如：Vue" /></el-form-item>
        <el-form-item label="标签颜色"><el-color-picker v-model="tag.color" /></el-form-item>
      </el-form>
      <el-table :data="tags" border>
        <el-table-column prop="name" label="名称" />
        <el-table-column label="颜色" width="130"><template #default="{ row }"><span class="anime-tag" :style="{ color: row.color }">{{ row.color }}</span></template></el-table-column>
        <el-table-column label="操作" width="170"><template #default="{ row }"><div class="action-row"><el-button size="small" @click="editTag(row)">编辑</el-button><el-button size="small" type="danger" @click="removeTag(row.id)">删除</el-button></div></template></el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api/blog'

const categories = ref([])
const tags = ref([])
const category = reactive({ id: null, name: '', description: '', sort: 0 })
const tag = reactive({ id: null, name: '', color: '#3677e6' })
const load = async () => {
  try { const [categoryRes, tagRes] = await Promise.all([adminApi.categories({ current: 1, size: 50 }), adminApi.tags({ current: 1, size: 50 })]); categories.value = categoryRes.data.records || []; tags.value = tagRes.data.records || [] } catch (error) { console.error(error); ElMessage.error('分类和标签加载失败') }
}
const saveCategory = async () => { if (!category.name.trim()) return ElMessage.warning('请填写分类名称'); try { await adminApi.saveCategory({ ...category }); resetCategory(); ElMessage.success('分类已保存'); await load() } catch (error) { console.error(error) } }
const saveTag = async () => { if (!tag.name.trim()) return ElMessage.warning('请填写标签名称'); try { await adminApi.saveTag({ ...tag }); resetTag(); ElMessage.success('标签已保存'); await load() } catch (error) { console.error(error) } }
const editCategory = (row) => Object.assign(category, { id: row.id, name: row.name || '', description: row.description || '', sort: row.sort || 0 })
const editTag = (row) => Object.assign(tag, { id: row.id, name: row.name || '', color: row.color || '#3677e6' })
const resetCategory = () => Object.assign(category, { id: null, name: '', description: '', sort: 0 })
const resetTag = () => Object.assign(tag, { id: null, name: '', color: '#3677e6' })
const removeCategory = async (id) => { await ElMessageBox.confirm('确认删除这个分类吗？', '删除分类', { type: 'warning' }); try { await adminApi.deleteCategory(id); if (category.id === id) resetCategory(); ElMessage.success('分类已删除'); await load() } catch (error) { console.error(error) } }
const removeTag = async (id) => { await ElMessageBox.confirm('确认删除这个标签吗？', '删除标签', { type: 'warning' }); try { await adminApi.deleteTag(id); if (tag.id === id) resetTag(); ElMessage.success('标签已删除'); await load() } catch (error) { console.error(error) } }
onMounted(load)
</script>
