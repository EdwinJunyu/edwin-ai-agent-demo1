<script setup>
import ChatWorkspace from '../components/ChatWorkspace.vue';

const quickPrompts = [
  '帮我规划一个 5 天 4 晚的成都旅行路线，偏向美食和慢节奏。',
  '设计一个适合情侣的三亚周末旅行计划，包含预算建议。',
  '我想带父母去杭州旅行 3 天，请给出轻松不赶路的安排。',
];
</script>

<template>
  <ChatWorkspace
    title="AI 旅行计划大师"
    subtitle="进入页面后自动生成聊天室 ID，通过 Love App SSE 接口实时返回旅行建议。"
    badge="Love App"
    endpoint-path="/ai/love_app/chat/sse/emitter"
    placeholder="输入你的旅行需求，例如目的地、天数、预算、同行人群..."
    session-label="聊天室 ID"
    empty-title="从一句需求开始你的旅行策划"
    empty-description="你可以描述目的地、时长、预算、偏好、同行人群，AI 会在当前聊天室内持续理解上下文。"
    :quick-prompts="quickPrompts"
    :requires-chat-id="true"
    theme="sunrise"
  />
</template>
