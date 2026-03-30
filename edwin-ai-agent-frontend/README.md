# Edwin AI Agent Frontend

基于 Vue 3 + Vite + Vue Router + Axios 的前端项目，包含一个主页和两个聊天式 AI 应用页面。

## 页面说明

- `/`：主页，用于切换不同应用
- `/love-app`：AI 旅行计划大师，进入页面自动生成 `chatId`，通过 SSE 调用 Love App 接口
- `/manus`：AI 超级智能体，通过 SSE 调用 Manus 接口

## 启动方式

```bash
npm install
npm run dev
```

## 后端代理

开发环境已在 `vite.config.js` 中配置 `/api -> http://127.0.0.1:8124` 代理，因此前端通过 `/api/ai/...` 访问后端即可。

## 说明

- 项目中保留了 Axios 实例用于常规 HTTP 请求管理
- SSE 流式对话使用浏览器原生 `EventSource`，更适合对接 Spring Boot `SseEmitter`
