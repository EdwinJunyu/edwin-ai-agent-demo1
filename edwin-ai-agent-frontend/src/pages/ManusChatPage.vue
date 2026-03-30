<script setup>
import ChatWorkspace from '../components/ChatWorkspace.vue';

const quickPrompts = [
  '请帮我拆解一个从 0 到 1 的 AI 产品 MVP 开发计划。',
  '作为超级智能体，请给我一份提升个人效率的 7 天行动方案。',
  '帮我设计一个 AI Agent 应用的功能结构、接口层和前端页面建议。',
];
</script>

<template>
  <ChatWorkspace
    title="AI 超级智能体"
    subtitle="保持同样的聊天室体验，通过 Manus SSE 接口实时展示智能体输出内容。"
    badge="Manus"
    endpoint-path="/ai/manus/chat"
    placeholder="输入你想交给超级智能体的问题、任务或目标..."
    empty-title="把复杂问题交给超级智能体"
    empty-description="适合需求拆解、方案生成、任务编排等场景，消息会在下方实时流式展示。"
    :quick-prompts="quickPrompts"
    :requires-chat-id="false"
    theme="ocean"
  />
</template>
