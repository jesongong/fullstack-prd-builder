# Frontend Coding Standards (Vue3 + Vite + Axios + antdv)

**Default UI library**: ant-design-vue (antdv) @latest.
**vxe-table** is optional -- only introduce it when the user confirms they need big-data table features (virtual scroll, large datasets). See section 3 below.

## 1. API Request Wrapper: request.js (MANDATORY)

Every project must have `src/utils/request.js`:

```javascript
import axios from 'axios'
import { message } from 'ant-design-vue'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 30000
})

// Request interceptor: auto-attach Token
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

// Response interceptor: unwrap res.data.data, handle errors uniformly
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      message.error(res.message || 'Request failed')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data  // Caller receives data directly, no .data chaining
  },
  error => {
    message.error(error.message || 'Network error')
    return Promise.reject(error)
  }
)

export default service
```

## 2. Table: a-table (Default, MANDATORY)

Default tables use ant-design-vue `a-table` with pagination and loading state:

```vue
<template>
  <div>
    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a @click="handleEdit(record)">Edit</a>
          <a-divider type="vertical" />
          <a @click="handleDelete(record.id)">Delete</a>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getPage, deleteApi } from '@/api/user-manage'

const loading = ref(false)
const tableData = ref([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `Total ${total} items`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: 'Name', dataIndex: 'name', key: 'name' },
  { title: 'Action', key: 'action', width: 150 }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPage({
      pageSize: pagination.pageSize,
      currentPage: pagination.current
    })
    tableData.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

onMounted(() => fetchData())
</script>
```

## 3. vxe-table: Big-Data Mode (OPTIONAL, Conditional)

Only use vxe-table when the user explicitly confirms they need it for large datasets.

**When to ask**: Before generating frontend code (SKILL.md Step 5), ask:
"Will any table in this project display large amounts of data (1000+ rows, virtual scrolling)? If yes, I'll use vxe-table for those tables."

If the user says yes, replace `a-table` with `vxe-grid`:

```vue
<template>
  <div>
    <vxe-grid
      v-bind="gridOptions"
      :loading="loading"
      :data="tableData"
      :pager-config="pagerConfig"
      @page-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getPage } from '@/api/user-manage'

const loading = ref(false)
const tableData = ref([])

const pagerConfig = reactive({
  pageSize: 10,
  currentPage: 1,
  total: 0,
  layouts: ['Total', 'PrevPage', 'Number', 'NextPage', 'Sizes', 'FullJump']
})

const gridOptions = reactive({
  columns: [
    { field: 'id', title: 'ID', width: 80 },
    { field: 'name', title: 'Name' }
  ]
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPage({
      pageSize: pagerConfig.pageSize,
      currentPage: pagerConfig.currentPage
    })
    tableData.value = res.records
    pagerConfig.total = res.total
  } finally {
    loading.value = false
  }
}

const handlePageChange = ({ currentPage, pageSize }) => {
  pagerConfig.currentPage = currentPage
  pagerConfig.pageSize = pageSize
  fetchData()
}

onMounted(() => fetchData())
</script>
```

## 4. Modal Logic: Single Modal for Add/Edit (MANDATORY)

Add and Edit share one `a-modal`. Edit passes `row` data for backfill:

```vue
<template>
  <div>
    <a-button type="primary" @click="handleAdd">Add</a-button>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }">
        <a-form-item label="Name" name="name">
          <a-input v-model:value="form.name" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'

const modalVisible = ref(false)
const submitLoading = ref(false)
const editId = ref(null)
const formRef = ref(null)

const defaultForm = () => ({ name: '' })

const form = reactive(defaultForm())
const rules = { name: [{ required: true, message: 'Required' }] }

const modalTitle = computed(() => editId.value ? 'Edit' : 'Add')

const handleAdd = () => {
  editId.value = null
  Object.assign(form, defaultForm())
  formRef.value?.clearValidate()
  modalVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(form, { name: row.name })  // backfill from row
  formRef.value?.clearValidate()
  modalVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch { return }
  submitLoading.value = true
  try {
    if (editId.value) {
      await updateApi({ id: editId.value, ...form })
    } else {
      await saveApi(form)
    }
    modalVisible.value = false
    fetchData()
  } finally {
    submitLoading.value = false
  }
}
</script>
```

## 5. Route Naming (MANDATORY)

- **path**: lowercase with hyphens (kebab-case): `/user-manage`, `/order-detail`
- **name**: UpperCamelCase (PascalCase): `UserManage`, `OrderDetail`

```javascript
// router/index.js
const routes = [
  {
    path: '/user-manage',
    name: 'UserManage',
    component: () => import('@/views/user-manage/index.vue')
  }
]
```

## 6. Environment Variables + Vite Proxy (MANDATORY)

### .env.development

```env
VITE_API_BASE = '/api'
```

### vite.config.js

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```
