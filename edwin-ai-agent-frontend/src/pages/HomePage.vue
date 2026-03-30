<script setup>
const appCards = [
  {
    title: 'AI 旅行计划大师',
    route: '/love-app',
    tag: 'SSE Chat',
    summary: '面向旅行规划场景的对话式应用，进入页面自动生成聊天室 ID，并通过 Love App 流式返回旅行建议。',
    highlights: ['自动生成 chatId', '流式展示 AI 回复', '聊天室风格界面'],
  },
  {
    title: 'AI 超级智能体',
    route: '/manus',
    tag: 'Manus Agent',
    summary: '超级智能体对话页面，延续同款聊天工作区体验，通过 Manus 接口实时展示推理和生成过程。',
    highlights: ['SSE 实时输出', '统一交互体验', '便于拓展多智能体能力'],
  },
];
</script>

<template>
  <div class="home-page">
    <div class="ambient ambient-left"></div>
    <div class="ambient ambient-right"></div>

    <section class="hero-card">
      <span class="eyebrow">Vue3 + Axios + SSE</span>
      <h1>Edwin AI 应用中心</h1>
      <p class="hero-copy">
        一个用于切换不同 AI 能力的前端工作台。首页负责应用导航，两个业务页统一采用聊天式布局，让用户可以直接进入对话与流式交互。
      </p>

      <div class="tech-pills">
        <span>Vue 3</span>
        <span>Vue Router</span>
        <span>Axios</span>
        <span>Server-Sent Events</span>
      </div>
    </section>

    <section class="app-grid">
      <RouterLink
        v-for="card in appCards"
        :key="card.route"
        :to="card.route"
        class="app-card"
      >
        <div class="card-topline">
          <span class="card-tag">{{ card.tag }}</span>
          <span class="card-action">进入应用</span>
        </div>

        <h2>{{ card.title }}</h2>
        <p>{{ card.summary }}</p>

        <div class="card-highlights">
          <span v-for="item in card.highlights" :key="item">{{ item }}</span>
        </div>
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: 56px 24px 72px;
  background:
    radial-gradient(circle at top left, rgba(255, 189, 122, 0.28), transparent 30%),
    radial-gradient(circle at 85% 15%, rgba(122, 197, 255, 0.24), transparent 26%),
    linear-gradient(140deg, #f7f2e9 0%, #eef7ff 54%, #fffdf8 100%);
}

.ambient {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.55;
  pointer-events: none;
}

.ambient-left {
  top: -120px;
  left: -100px;
  background: rgba(255, 160, 122, 0.4);
}

.ambient-right {
  right: -80px;
  bottom: -100px;
  background: rgba(114, 160, 255, 0.35);
}

.hero-card,
.app-grid {
  position: relative;
  z-index: 1;
  width: min(1120px, 100%);
  margin: 0 auto;
}

.hero-card {
  padding: 40px;
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 28px 80px rgba(34, 55, 94, 0.12);
  backdrop-filter: blur(18px);
}

.eyebrow {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(17, 28, 44, 0.08);
  color: #33415f;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 18px 0 12px;
  color: #172033;
  font-size: clamp(2.6rem, 5vw, 4.4rem);
  line-height: 1.04;
}

.hero-copy {
  max-width: 740px;
  color: #4d5b73;
  font-size: 1.08rem;
  line-height: 1.8;
}

.tech-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.tech-pills span {
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(32, 45, 72, 0.08);
  color: #2c3851;
  font-weight: 600;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-top: 28px;
}

.app-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 280px;
  padding: 28px;
  border-radius: 28px;
  color: inherit;
  text-decoration: none;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 20px 60px rgba(30, 48, 86, 0.09);
  transition:
    transform 0.28s ease,
    box-shadow 0.28s ease,
    border-color 0.28s ease;
}

.app-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 28px 70px rgba(30, 48, 86, 0.16);
  border-color: rgba(28, 57, 104, 0.14);
}

.card-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-tag,
.card-action {
  font-size: 0.88rem;
  font-weight: 700;
}

.card-tag {
  color: #0a5f8b;
}

.card-action {
  color: #d85e20;
}

.app-card h2 {
  margin: 0;
  color: #172033;
  font-size: 1.65rem;
}

.app-card p {
  margin: 0;
  color: #52617b;
  line-height: 1.75;
}

.card-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: auto;
}

.card-highlights span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(19, 34, 61, 0.06);
  color: #2e3952;
  font-size: 0.88rem;
}

@media (max-width: 720px) {
  .home-page {
    padding: 24px 16px 40px;
  }

  .hero-card,
  .app-card {
    padding: 22px;
    border-radius: 24px;
  }
}
</style>
