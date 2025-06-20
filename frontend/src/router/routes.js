const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/login' },
      {
        path: 'search',
        component: () => import('pages/SearchPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'advanced-search',
        component: () => import('pages/AdvancedSearchPage.vue'),
        meta: { requiresAuth: true },
      },
      { path: 'login', component: () => import('pages/LoginPage.vue') },
      { path: 'register', component: () => import('pages/RegisterPage.vue') },
      {
        path: 'profile',
        component: () => import('pages/UserProfile.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'edit-profile',
        component: () => import('pages/EditProfile.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'change-password',
        component: () => import('pages/ChangePassword.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'admin-panel',
        component: () => import('pages/AdminPanel.vue'),
        meta: { requiresAuth: true, requiresAdmin: true },
      },
    ],
  },
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
]

export default routes
