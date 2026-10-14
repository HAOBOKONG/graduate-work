<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarketNav from '../components/MarketNav.vue'
import { currentUser } from '../api/auth'
import { getCategories, getProduct, saveProduct, conditions, type Category, type ProductInput } from '../api/products'
import { errorMessage } from '../api/http'
import { uploadImage } from '../api/uploads'
const route = useRoute(), router = useRouter()
const editId = route.params.id ? String(route.params.id) : undefined
const form = reactive<ProductInput>({ title: '', description: '', categoryId: null, price: '', originalPrice: null, conditionLevel: 4, imageUrls: [] })
const categories = ref<Category[]>([]), images = ref<string[]>([]), loading = ref(true), submitting = ref(false), uploading = ref(false), error = ref(''), ready = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
onMounted(async () => {
  try {
    categories.value = await getCategories()
    if (editId) {
      const detail = await getProduct(editId), user = await currentUser()
      if (detail.product.sellerId !== user.id) { error.value = '只能修改自己发布的商品'; return }
      const p = detail.product
      Object.assign(form, { title: p.title, description: p.description, categoryId: p.categoryId, price: String(p.price), originalPrice: p.originalPrice === null ? null : String(p.originalPrice), conditionLevel: p.conditionLevel })
      images.value = [...p.images]
    }
    ready.value = true
  } catch (failure) { error.value = errorMessage(failure) }
  finally { loading.value = false }
})
async function addFiles(files: FileList | File[]) {
  if (uploading.value) return
  const selected = Array.from(files)
  if (images.value.length + selected.length > 8) { error.value = '最多上传8张图片'; return }
  uploading.value = true; error.value = ''
  try {
    for (const file of selected) {
      if (!['image/jpeg', 'image/png'].includes(file.type)) { error.value = '仅支持JPG和PNG图片'; break }
      if (file.size > 5 * 1024 * 1024) { error.value = '单张图片不能超过5MB'; break }
      images.value.push(await uploadImage(file))
    }
  } catch (failure) { error.value = errorMessage(failure) }
  finally { uploading.value = false; if (fileInput.value) fileInput.value.value = '' }
}
function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files) void addFiles(input.files)
}
function onDrop(event: DragEvent) { if (event.dataTransfer?.files) void addFiles(event.dataTransfer.files) }
function onPaste(event: ClipboardEvent) {
  const files = Array.from(event.clipboardData?.items ?? []).filter(item => item.type.startsWith('image/'))
    .map(item => item.getAsFile()).filter((file): file is File => file !== null)
  if (files.length) { event.preventDefault(); void addFiles(files) }
}
onMounted(() => window.addEventListener('paste', onPaste))
onUnmounted(() => window.removeEventListener('paste', onPaste))
async function submit() {
  if (submitting.value || uploading.value || !ready.value) return
  error.value = ''
  if (!form.title.trim() || !form.description.trim() || !form.categoryId || !form.price.trim()) { error.value = '请填写标题、描述、分类和价格'; return }
  const money = /^(?:0|[1-9]\d{0,9})(?:\.\d{1,2})?$/
  if (!money.test(form.price) || Number(form.price) <= 0) { error.value = '价格须大于0，最多2位小数'; return }
  if (form.originalPrice && !money.test(form.originalPrice)) { error.value = '原价须大于等于0，最多2位小数'; return }
  submitting.value = true
  try {
    const result = await saveProduct({ ...form, originalPrice: form.originalPrice || null, imageUrls: images.value }, editId)
    ElMessage.success(editId ? '修改已提交，请等待管理员审核' : '商品已提交，请等待管理员审核')
    await router.push(`/products/${result.product.id}`)
  } catch (failure) { error.value = errorMessage(failure) }
  finally { submitting.value = false }
}
</script>
<template>
  <div class="market-page">
    <MarketNav />
    <div class="publish-heading"><span class="eyebrow">SHARE THE GOOD STUFF</span><h2>{{ editId ? '更新这件好物' : '发布一件闲置好物' }}</h2><p>拍几张清晰的照片，说说它的故事，让下一位同学放心接手。</p></div>
    <el-card class="publish-card" v-loading="loading">
      <p class="publish-note">发布或修改后需经管理员审核，通过后会出现在商品首页。</p>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-form label-position="top" @submit.prevent="submit">
        <div class="form-section-title"><span>01</span><div><h3>商品信息</h3><p>标题、分类与描述越具体，越容易被找到。</p></div></div>
        <el-form-item label="商品标题 *"><el-input v-model="form.title" maxlength="200" show-word-limit /></el-form-item>
        <el-form-item label="商品描述 *"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="10000" show-word-limit /></el-form-item>
        <div class="form-row"><el-form-item label="分类 *"><el-select v-model="form.categoryId" placeholder="请选择分类"><el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" /></el-select></el-form-item><el-form-item label="成色 *"><el-select v-model="form.conditionLevel"><el-option v-for="(condition, index) in conditions" :key="index" :label="condition" :value="index + 1" /></el-select></el-form-item></div>
        <div class="form-section-title"><span>02</span><div><h3>价格与照片</h3><p>真实照片和合理价格能让交易更顺利。</p></div></div>
        <div class="form-row"><el-form-item label="售价（元）*"><el-input v-model="form.price" placeholder="例如 59.90" inputmode="decimal" /></el-form-item><el-form-item label="原价（元，可选）"><el-input v-model="form.originalPrice" placeholder="可留空" inputmode="decimal" /></el-form-item></div>
        <div class="el-form-item__label">商品图片（可选，最多8张）</div>
        <div class="upload-zone" tabindex="0" role="button" aria-label="上传图片，可拖入或粘贴" @click="fileInput?.click()" @keydown.enter.prevent="fileInput?.click()" @dragover.prevent @drop.prevent="onDrop">
          <span class="upload-icon">＋</span><strong>{{ uploading ? '图片上传中…' : '点击选择、拖入，或按 Ctrl+V 粘贴图片' }}</strong><small>支持 JPG / PNG，单张不超过 5MB</small>
        </div>
        <input ref="fileInput" type="file" accept="image/jpeg,image/png" multiple class="visually-hidden" @change="onFileChange" />
        <div v-if="images.length" class="upload-previews"><div v-for="(url, index) in images" :key="url" class="upload-preview"><el-image :src="url" fit="cover"><template #error><div class="no-image">图片无法加载</div></template></el-image><button type="button" :aria-label="`移除第${index + 1}张图片`" @click="images.splice(index, 1)">×</button></div></div>
        <el-button class="publish-submit" type="primary" native-type="submit" :loading="submitting" :disabled="!ready || uploading">{{ editId ? '保存修改并提交审核' : '发布商品并提交审核' }}</el-button>
      </el-form>
    </el-card>
  </div>
</template>
