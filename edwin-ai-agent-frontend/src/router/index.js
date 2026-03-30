import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '../pages/HomePage.vue';
import LoveChatPage from '../pages/LoveChatPage.vue';
import ManusChatPage from '../pages/ManusChatPage.vue';

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomePage,
  },
  {
    path: '/love-app',
    name: 'love-app',
    component: LoveChatPage,
  },
  {
    path: '/manus',
    name: 'manus',
    component: ManusChatPage,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
