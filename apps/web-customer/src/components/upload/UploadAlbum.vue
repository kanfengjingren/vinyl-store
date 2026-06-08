<template>
    <form @submit.prevent="handleSubmit" class="max-w-2xl mx-auto p-6 space-y-6">
        <h2 class="text-2xl font-bold tracking-tight">创建新专辑</h2>

        <!-- 使用 grid 布局，两列响应式 -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <!-- 左侧字段 -->
            <div>
                <label for="title" class="block text-sm font-medium mb-1">专辑名称 *</label>
                <input id="title" v-model="form.title" type="text" required
                    class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="例如：万能青年旅店" />
            </div>

            <div>
                <label for="artist" class="block text-sm font-medium mb-1">艺术家 *</label>
                <input id="artist" v-model="form.artist" type="text" required
                    class="w-full px-3 py-2 border rounded-lg ..." placeholder="乐队或音乐人" />
            </div>

            <div>
                <label for="price" class="block text-sm font-medium mb-1">价格 *</label>
                <input id="price" v-model.number="form.price" type="number" min="0"
                    class="w-full px-3 py-2 border rounded-lg ..." />
            </div>

            <div>
                <label for="label" class="block text-sm font-medium mb-1">发行商 *</label>
                <input id="label" v-model="form.label" type="text" class="w-full px-3 py-2 border rounded-lg ..." />
            </div>

            <div>
                <label for="country" class="block text-sm font-medium mb-1">国家 *</label>
                <input id="country" v-model="form.country" type="text" class="w-full px-3 py-2 border rounded-lg ..." />
            </div>

            <div>
                <label for="badge" class="block text-sm font-medium mb-1">标签 *</label>
                <input id="badge" v-model="form.badge" type="text" class="w-full px-3 py-2 border rounded-lg ..." />
            </div>

            <div>
                <label for="slug" class="block text-sm font-medium mb-1">URL 标识 *</label>
                <input id="slug" v-model="form.slug" type="text" required class="w-full px-3 py-2 border rounded-lg ..."
                    placeholder="my-album-slug" />
            </div>

            <div>
                <label for="year" class="block text-sm font-medium mb-1">发行年份</label>
                <input id="year" v-model.number="form.year" type="number"
                    class="w-full px-3 py-2 border rounded-lg ..." />
            </div>

            <div>
                <label for="stock" class="block text-sm font-medium mb-1">库存</label>
                <input id="stock" v-model.number="form.stock" type="number" min="0"
                    class="w-full px-3 py-2 border rounded-lg ..." />
            </div>
        </div>

        <!-- 全宽的字段 -->

        <div>
            <label for="description" class="block text-sm font-medium mb-1">简介</label>
            <textarea id="description" v-model="form.description" rows="3"
                class="w-full px-3 py-2 border rounded-lg ..." placeholder="关于这张专辑的一些描述..."></textarea>
        </div>

        <!-- 上传封面 -->
        <div class="w-[100px] h-[100px] border-[2px] border-black rounded-lg" @click.prevent="imgClick">
            <div><img :src="url" alt=""></div>
            <input type="file" style="display: none;" ref="img-input" @click.stop @change="imgChange">

        </div>


        <!-- 消息提示 -->
        <div v-if="errorMsg" class="text-red-600 text-sm">{{ errorMsg }}</div>
        <div v-if="successMsg" class="text-green-600 text-sm">{{ successMsg }}</div>

        <!-- 提交按钮 -->
        <button type="submit" :disabled="submitting"
            class="w-full py-3 bg-blue-600 text-white font-semibold rounded-full hover:bg-blue-700 disabled:opacity-50 transition-colors">
            {{ submitting ? '提交中...' : '创建专辑' }}
        </button>

        


    </form>
    <button @click="reset">reset</button>

    <!-- <button @click="uploadImg">上传图片</button> -->
</template>

<script setup>
import { reactive, ref, useTemplateRef } from 'vue';
import axios from 'axios';

const url = ref('')

const initialState = {
  title: '',
  artist: '',
  price: 0,
  label: '',
  country: '',
  badge: '',
  slug: '',
  coverUrl: '',
  description: '',
  year: new Date().getFullYear(),
  gradient: '',
  stock: 0,
}

const form = reactive({
    title: '',
    artist: '',
    price: 0,
    label: '',
    country: '',
    badge: '',
    slug: '',
    coverUrl: '',
    description: '',
    year: new Date().getFullYear(),
    gradient: '',
    stock: 0,
});

const submitting = ref(false);
const errorMsg = ref('');
const successMsg = ref('');

const imgInput = useTemplateRef('img-input')

const imgClick = () => {

    imgInput.value.click()
}
const imgChange = (e) => {
    const file = e.target.files[0]
    url.value = URL.createObjectURL(file)

}
const uploadImg = async () => {
    const file = imgInput.value.files[0]


    const formData = new FormData()
    formData.append("file", file)
    const res = await axios.post('/api/upload/cover', formData)
    console.log(res.data.url);

    const url = res.data.url.slice(1)   //  /uploads/covers/cd-cne.jpg
    form.coverUrl = url

}
const reset = ()=>{
    Object.assign(form,initialState)
}
async function handleSubmit() {
    // 简单的前端验证
    if (!form.title.trim() || !form.artist.trim()) {
        errorMsg.value = '请填写专辑名称和艺术家';
        return;
    }
    if (form.price < 0) {
        errorMsg.value = '价格不能为负数';
        return;
    }

    await uploadImg()
    if (!form.coverUrl) {
        errorMsg.value = '请上传封面';
        return;
    }
    // ... 更多校验按需添加


    submitting.value = true;
    errorMsg.value = '';
    successMsg.value = '';

    try {
        //提交的逻辑：发送封面文件，发送专辑信息
        await axios.post('/api/albums', form);
        successMsg.value = '专辑创建成功！';

        // 清空表单或跳转
    } catch (err) {
        errorMsg.value = err.response?.data?.message || '创建失败';
    } finally {
        submitting.value = false;
    }
}
</script>
