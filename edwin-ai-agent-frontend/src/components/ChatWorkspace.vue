<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { createSessionId } from '../utils/id';
import { openChatStream } from '../services/chat';

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: '',
  },
  badge: {
    type: String,
    default: '',
  },
  endpointPath: {
    type: String,
    required: true,
  },
  placeholder: {
    type: String,
    default: '请输入你的问题...',
  },
  sessionLabel: {
    type: String,
    default: '会话 ID',
  },
  emptyTitle: {
    type: String,
    default: '开始新对话',
  },
  emptyDescription: {
    type: String,
    default: '请输入消息并发送，AI 回复会在这里实时显示。',
  },
  quickPrompts: {
    type: Array,
    default: () => [],
  },
  requiresChatId: {
    type: Boolean,
    default: false,
  },
  theme: {
    type: String,
    default: 'sunrise',
  },
});

const router = useRouter();
const draft = ref('');
const isStreaming = ref(false);
const messages = ref([]);
const messageViewport = ref(null);
const streamRef = ref(null);
const chatId = ref('');

const themeClass = computed(() => `theme-${props.theme}`);

const resetChatId = () => {
  if (!props.requiresChatId) {
    chatId.value = '';
    return;
  }

  chatId.value = createSessionId('chat');
};

const closeStream = () => {
  if (streamRef.value) {
    streamRef.value.close();
    streamRef.value = null;
  }

  isStreaming.value = false;
};

const scrollToBottom = async () => {
  await nextTick();

  if (!messageViewport.value) {
    return;
  }

  messageViewport.value.scrollTop = messageViewport.value.scrollHeight;
};

const formatTime = (timestamp) =>
  new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(timestamp);

const pushMessage = (role, content = '') => {
  const message = {
    id: createSessionId(role),
    role,
    content,
    timestamp: Date.now(),
  };

  messages.value.push(message);
  return message;
};

const startNewConversation = () => {
  closeStream();
  messages.value = [];
  draft.value = '';
  resetChatId();
};

const applyQuickPrompt = async (prompt) => {
  if (isStreaming.value) {
    return;
  }

  draft.value = prompt;
  await nextTick();
  await submitMessage();
};

const normalizeChunk = (chunk) => {
  if (chunk === '[DONE]') {
    return '';
  }

  return chunk ?? '';
};

const submitMessage = async () => {
  const message = draft.value.trim();

  if (!message || isStreaming.value) {
    return;
  }

  draft.value = '';
  pushMessage('user', message);

  const assistantMessage = pushMessage('assistant', '');
  await scrollToBottom();

  isStreaming.value = true;

  const eventSource = openChatStream({
    path: props.endpointPath,
    message,
    chatId: props.requiresChatId ? chatId.value : undefined,
  });

  streamRef.value = eventSource;
  let hasReceivedChunk = false;

  eventSource.onmessage = async (event) => {
    hasReceivedChunk = true;
    assistantMessage.content += normalizeChunk(event.data);
    await scrollToBottom();
  };

  eventSource.onerror = async () => {
    const hasContent = assistantMessage.content.trim().length > 0 || hasReceivedChunk;
    const isClosed = eventSource.readyState === EventSource.CLOSED;

    if (isClosed && hasReceivedChunk) {
      closeStream();
      await scrollToBottom();
      return;
    }

    if (!hasContent) {
      assistantMessage.content =
        '无法连接到后端服务，请确认 127.0.0.1:8124 已启动，并且 /api 接口可访问。';
    }

    closeStream();
    await scrollToBottom();
  };
};

const onComposerKeydown = async (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    await submitMessage();
  }
};

onMounted(() => {
  resetChatId();
});

onBeforeUnmount(() => {
  closeStream();
});
</script>

<template>
  <div class="chat-page" :class="themeClass">
    <div class="page-glow page-glow-left"></div>
    <div class="page-glow page-glow-right"></div>

    <div class="chat-shell">
      <header class="chat-header">
        <div class="chat-heading">
          <button class="nav-button" type="button" @click="router.push('/')">
            返回主页
          </button>
          <span class="heading-badge">{{ badge }}</span>
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </div>

        <div class="chat-tools">
          <div v-if="requiresChatId" class="session-card">
            <span>{{ sessionLabel }}</span>
            <strong>{{ chatId }}</strong>
          </div>

          <button class="secondary-button" type="button" @click="startNewConversation">
            新建会话
          </button>
        </div>
      </header>

      <main ref="messageViewport" class="message-board">
        <section v-if="messages.length === 0" class="empty-state">
          <span class="empty-label">Ready to Chat</span>
          <h2>{{ emptyTitle }}</h2>
          <p>{{ emptyDescription }}</p>

          <div class="quick-prompts">
            <button
              v-for="prompt in quickPrompts"
              :key="prompt"
              class="prompt-chip"
              type="button"
              @click="applyQuickPrompt(prompt)"
            >
              {{ prompt }}
            </button>
          </div>
        </section>

        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <div class="avatar">
            {{ message.role === 'user' ? '我' : 'AI' }}
          </div>

          <div class="bubble">
            <div class="bubble-meta">
              <strong>{{ message.role === 'user' ? '用户' : '智能助手' }}</strong>
              <span>{{ formatTime(message.timestamp) }}</span>
            </div>
            <p>{{ message.content || '正在生成中...' }}</p>
          </div>
        </article>
      </main>

      <footer class="composer-card">
        <div class="composer-frame">
          <textarea
            v-model="draft"
            class="composer-input"
            :placeholder="placeholder"
            :disabled="isStreaming"
            rows="3"
            @keydown="onComposerKeydown"
          ></textarea>

          <div class="composer-footer">
            <div class="status-text">
              {{ isStreaming ? 'SSE 正在推送回复，请稍候...' : 'Enter 发送，Shift + Enter 换行' }}
            </div>

            <button
              class="primary-button"
              type="button"
              :disabled="!draft.trim() || isStreaming"
              @click="submitMessage"
            >
              {{ isStreaming ? '生成中...' : '发送消息' }}
            </button>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: 24px;
}

.theme-sunrise {
  background:
    radial-gradient(circle at 18% 20%, rgba(255, 189, 121, 0.34), transparent 28%),
    radial-gradient(circle at 82% 12%, rgba(247, 130, 104, 0.24), transparent 24%),
    linear-gradient(180deg, #fff5eb 0%, #fff9f3 34%, #f8fbff 100%);
}

.theme-ocean {
  background:
    radial-gradient(circle at 20% 18%, rgba(93, 187, 255, 0.28), transparent 28%),
    radial-gradient(circle at 84% 14%, rgba(90, 134, 255, 0.24), transparent 22%),
    linear-gradient(180deg, #eef8ff 0%, #f6fbff 36%, #fbfcff 100%);
}

.page-glow {
  position: absolute;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
  opacity: 0.55;
}

.theme-sunrise .page-glow-left {
  top: -120px;
  left: -60px;
  background: rgba(255, 170, 127, 0.55);
}

.theme-sunrise .page-glow-right {
  right: -80px;
  bottom: -120px;
  background: rgba(255, 208, 110, 0.38);
}

.theme-ocean .page-glow-left {
  top: -120px;
  left: -60px;
  background: rgba(94, 181, 255, 0.4);
}

.theme-ocean .page-glow-right {
  right: -80px;
  bottom: -120px;
  background: rgba(104, 132, 255, 0.32);
}

.chat-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 18px;
  width: min(1240px, 100%);
  height: calc(100vh - 48px);
  margin: 0 auto;
}

.chat-header,
.message-board,
.composer-card {
  border: 1px solid rgba(255, 255, 255, 0.68);
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(18px);
  box-shadow: 0 24px 70px rgba(40, 60, 98, 0.12);
}

.chat-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 24px;
  border-radius: 28px;
}

.chat-heading h1 {
  margin: 12px 0 10px;
  font-size: clamp(2rem, 3vw, 3rem);
  color: #162035;
}

.chat-heading p {
  max-width: 760px;
  margin: 0;
  color: #5a6780;
  line-height: 1.75;
}

.heading-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(17, 28, 44, 0.08);
  color: #32415d;
  font-size: 0.88rem;
  font-weight: 700;
}

.chat-tools {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.session-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(28, 44, 74, 0.08);
}

.session-card span {
  font-size: 0.82rem;
  color: #6b7690;
}

.session-card strong {
  color: #1d2840;
  word-break: break-all;
}

.nav-button,
.secondary-button,
.primary-button,
.prompt-chip {
  border: none;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    opacity 0.2s ease,
    box-shadow 0.2s ease;
}

.nav-button:hover,
.secondary-button:hover,
.primary-button:hover,
.prompt-chip:hover {
  transform: translateY(-2px);
}

.nav-button,
.secondary-button {
  padding: 12px 18px;
  border-radius: 16px;
  background: rgba(18, 31, 54, 0.06);
  color: #22304a;
  font-weight: 700;
}

.primary-button {
  padding: 14px 22px;
  border-radius: 18px;
  background: linear-gradient(135deg, #1f6fd9 0%, #2f88ff 100%);
  color: #ffffff;
  font-weight: 700;
  box-shadow: 0 12px 30px rgba(48, 119, 225, 0.28);
}

.primary-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  transform: none;
  box-shadow: none;
}

.message-board {
  overflow-y: auto;
  padding: 26px;
  border-radius: 32px;
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 100%;
  padding: 32px 16px;
  text-align: center;
}

.empty-label {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(25, 41, 69, 0.08);
  color: #475675;
  font-size: 0.84rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.empty-state h2 {
  margin: 18px 0 12px;
  color: #162035;
  font-size: clamp(1.8rem, 2.6vw, 2.8rem);
}

.empty-state p {
  max-width: 680px;
  margin: 0;
  color: #60708c;
  line-height: 1.8;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 26px;
}

.prompt-chip {
  padding: 12px 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(28, 44, 74, 0.08);
  color: #23314c;
  font-weight: 600;
}

.message-row {
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.user .avatar {
  order: 2;
  background: linear-gradient(135deg, #f08c4f 0%, #ffb15b 100%);
}

.message-row.user .bubble {
  order: 1;
  background: linear-gradient(135deg, #2a6fda 0%, #4f98ff 100%);
  color: #ffffff;
  border-bottom-right-radius: 10px;
}

.message-row.user .bubble-meta span,
.message-row.user .bubble-meta strong {
  color: rgba(255, 255, 255, 0.88);
}

.message-row.assistant .bubble {
  border-bottom-left-radius: 10px;
}

.avatar {
  flex: 0 0 46px;
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: linear-gradient(135deg, #0c6f99 0%, #29a7c8 100%);
  color: #ffffff;
  font-weight: 800;
  box-shadow: 0 10px 24px rgba(38, 87, 125, 0.18);
}

.bubble {
  max-width: min(76%, 820px);
  padding: 16px 18px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid rgba(25, 42, 72, 0.08);
  box-shadow: 0 12px 28px rgba(26, 45, 76, 0.08);
}

.bubble-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.bubble-meta strong {
  color: #1d2941;
  font-size: 0.94rem;
}

.bubble-meta span {
  color: #6a7790;
  font-size: 0.84rem;
}

.bubble p {
  margin: 0;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.composer-card {
  padding: 18px;
  border-radius: 28px;
}

.composer-frame {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.composer-input {
  width: 100%;
  min-height: 108px;
  resize: none;
  padding: 18px 20px;
  border: 1px solid rgba(34, 49, 78, 0.12);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  color: #172033;
  font: inherit;
  line-height: 1.7;
  outline: none;
}

.composer-input:focus {
  border-color: rgba(31, 111, 217, 0.48);
  box-shadow: 0 0 0 4px rgba(47, 136, 255, 0.12);
}

.composer-input:disabled {
  cursor: wait;
  background: rgba(247, 249, 252, 0.88);
}

.composer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.status-text {
  color: #60708c;
  font-size: 0.92rem;
}

@media (max-width: 960px) {
  .chat-shell {
    height: auto;
    min-height: calc(100vh - 48px);
  }

  .chat-header {
    flex-direction: column;
  }

  .chat-tools {
    width: 100%;
    justify-content: flex-start;
  }

  .message-board {
    min-height: 52vh;
  }

  .bubble {
    max-width: 100%;
  }
}

@media (max-width: 720px) {
  .chat-page {
    padding: 16px;
  }

  .chat-shell {
    gap: 14px;
    height: auto;
  }

  .chat-header,
  .message-board,
  .composer-card {
    padding: 18px;
    border-radius: 24px;
  }

  .composer-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .primary-button,
  .secondary-button,
  .nav-button {
    width: 100%;
  }

  .message-row {
    align-items: flex-start;
  }

  .avatar {
    width: 40px;
    height: 40px;
    flex-basis: 40px;
  }
}
</style>
