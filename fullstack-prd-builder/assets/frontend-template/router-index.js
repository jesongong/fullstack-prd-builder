import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {{#each modules}}
  {
    path: '/{{kebabCase name}}',
    name: '{{pascalCase name}}',
    component: () => import('@/views/{{kebabCase name}}/index.vue')
  },
  {{/each}}
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
