<template>
  <Transition name="welcome-fade">
    <section v-if="shouldShow" ref="gateRef" class="welcome-gate" :class="{ 'is-static': !animationEnabled }" :style="gateStyle" aria-label="欢迎页" @pointermove="handlePointerMove" @pointerleave="handlePointerLeave">
      <canvas ref="canvasRef" class="welcome-canvas" aria-hidden="true"></canvas>
      <div ref="backdropRef" class="welcome-backdrop" aria-hidden="true"></div>
      <div ref="vignetteRef" class="welcome-vignette" aria-hidden="true"></div>
      <div class="welcome-pointer-glow" aria-hidden="true"></div>

      <div ref="contentRef" class="welcome-content">
        <div class="welcome-avatar-shell" :class="{ 'is-static': !animationEnabled }">
          <div class="welcome-avatar-ring ring-one"></div>
          <div class="welcome-avatar-ring ring-two"></div>
          <div class="welcome-avatar-ring ring-three"></div>
          <span class="welcome-ring-dot dot-one"></span>
          <span class="welcome-ring-dot dot-two"></span>
          <span class="welcome-ring-dot dot-three"></span>
          <div class="welcome-avatar">
            <img v-if="avatarUrl" :src="avatarUrl" alt="欢迎头像" />
            <span v-else>{{ initials }}</span>
          </div>
        </div>

        <p class="welcome-name">{{ displayName }}</p>
        <h1>{{ title }}</h1>
        <p class="welcome-subtitle">{{ subtitle }}</p>
        <p class="welcome-description">{{ description }}</p>
        <button type="button" class="welcome-enter" @click="enter">
          <span>{{ buttonText }}</span>
          <span class="welcome-enter-arrow" aria-hidden="true">↗</span>
        </button>
        <p class="welcome-hint">点击进入 · 祝你今天也有好心情</p>
      </div>
    </section>
  </Transition>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSiteStore } from '../stores/site'
import { normalizeAssetUrl } from '../utils/assets'
import { isAdminPagePath } from '../utils/adminPath'

const SESSION_KEY = 'blog_welcome_gate_entered'
const route = useRoute()
const router = useRouter()
const site = useSiteStore()
const gateRef = ref(null)
const canvasRef = ref(null)
const backdropRef = ref(null)
const vignetteRef = ref(null)
const contentRef = ref(null)
const entered = ref(false)
const routeReady = ref(false)
const reducedMotion = ref(false)
const pointer = { targetX: 0, targetY: 0, currentX: 0, currentY: 0 }
let frameId = 0
let resizeHandler

const excludedRoute = computed(() => {
  const path = route.path || ''
  const isLegacyAdminPath = path === '/admin' || path.startsWith('/admin/')
  return path === '/install' || path === '/login' || isLegacyAdminPath
    || route.meta.requiresAdmin || route.meta.loginType === 'ADMIN'
    || isAdminPagePath(path, site.adminLoginPath)
})
const shouldShow = computed(() => routeReady.value && site.loaded && site.welcomeEnabled && !entered.value && !excludedRoute.value)
const animationEnabled = computed(() => site.welcomeAnimationEnabled)
const avatarUrl = computed(() => normalizeAssetUrl(site.welcomeAvatarUrl))
const backgroundUrl = computed(() => normalizeAssetUrl(site.welcomeBackgroundUrl || site.backgroundUrl) || '/welcome-background.png')
const accent = computed(() => /^#[0-9a-f]{6}$/i.test(site.welcomeAccentColor) ? site.welcomeAccentColor : '#c9a227')
const displayName = computed(() => site.welcomeDisplayName || 'Lily')
const title = computed(() => site.welcomeTitle || '愿你在这里，遇见温柔的光')
const subtitle = computed(() => site.welcomeSubtitle || '每一次相遇，都值得被认真收藏')
const description = computed(() => site.welcomeDescription || '在字里行间，慢慢认识我。')
const buttonText = computed(() => site.welcomeButtonText || '进入博客')
const initials = computed(() => displayName.value.trim().slice(0, 1).toUpperCase() || 'L')
const gateStyle = computed(() => ({ '--welcome-accent': accent.value, '--welcome-bg-image': backgroundUrl.value ? `url("${backgroundUrl.value}")` : 'none' }))

const handlePointerMove = (event) => {
  if (!animationEnabled.value || reducedMotion.value || event.pointerType === 'touch') return
  const rect = event.currentTarget.getBoundingClientRect()
  pointer.targetX = ((event.clientX - rect.left) / rect.width - 0.5) * 2
  pointer.targetY = ((event.clientY - rect.top) / rect.height - 0.5) * 2
}

const handlePointerLeave = () => {
  pointer.targetX = 0
  pointer.targetY = 0
}

const enter = () => {
  sessionStorage.setItem(SESSION_KEY, '1')
  entered.value = true
}

const stopParticles = () => {
  if (frameId) cancelAnimationFrame(frameId)
  frameId = 0
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
}

const startParticles = async () => {
  stopParticles()
  if (!animationEnabled.value || reducedMotion.value || !shouldShow.value) return
  await nextTick()
  const canvas = canvasRef.value
  if (!canvas) return
  const context = canvas.getContext('2d')
  const particles = []
  const count = Math.min(72, Math.max(34, Math.floor(window.innerWidth / 22)))
  const resize = () => {
    const ratio = Math.min(window.devicePixelRatio || 1, 2)
    canvas.width = window.innerWidth * ratio
    canvas.height = window.innerHeight * ratio
    canvas.style.width = `${window.innerWidth}px`
    canvas.style.height = `${window.innerHeight}px`
    context.setTransform(ratio, 0, 0, ratio, 0, 0)
  }
  resizeHandler = resize
  resize()
  for (let index = 0; index < count; index += 1) {
    particles.push({ x: Math.random() * window.innerWidth, y: Math.random() * window.innerHeight, r: Math.random() * 1.8 + 0.4, speed: Math.random() * 0.22 + 0.08, drift: Math.random() * 0.4 - 0.2, phase: Math.random() * Math.PI * 2 })
  }
  window.addEventListener('resize', resize)
  const draw = (time) => {
    context.clearRect(0, 0, window.innerWidth, window.innerHeight)
    pointer.currentX += (pointer.targetX - pointer.currentX) * 0.08
    pointer.currentY += (pointer.targetY - pointer.currentY) * 0.08
    if (backdropRef.value) backdropRef.value.style.transform = `translate3d(${pointer.currentX * -16}px, ${pointer.currentY * -10}px, 0) scale(1.08)`
    if (vignetteRef.value) vignetteRef.value.style.transform = `translate3d(${pointer.currentX * 7}px, ${pointer.currentY * 5}px, 0) scale(1.03)`
    if (contentRef.value) contentRef.value.style.transform = `translate3d(${pointer.currentX * 3}px, ${pointer.currentY * 2}px, 0)`
    if (gateRef.value) {
      gateRef.value.style.setProperty('--pointer-x', `${50 + pointer.currentX * 18}%`)
      gateRef.value.style.setProperty('--pointer-y', `${45 + pointer.currentY * 18}%`)
    }
    particles.forEach((particle) => {
      particle.y -= particle.speed
      particle.x += Math.sin(time / 2400 + particle.phase) * particle.drift
      if (particle.y < -8) { particle.y = window.innerHeight + 8; particle.x = Math.random() * window.innerWidth }
      const alpha = 0.18 + ((Math.sin(time / 1100 + particle.phase) + 1) / 2) * 0.48
      context.beginPath()
      context.fillStyle = `rgba(247, 231, 202, ${alpha})`
      context.arc(particle.x, particle.y, particle.r, 0, Math.PI * 2)
      context.fill()
    })
    frameId = requestAnimationFrame(draw)
  }
  frameId = requestAnimationFrame(draw)
}

onMounted(async () => {
  entered.value = sessionStorage.getItem(SESSION_KEY) === '1'
  reducedMotion.value = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches || false
  await router.isReady()
  routeReady.value = true
  await site.loadSite()
  startParticles()
})
watch(shouldShow, startParticles)
watch(animationEnabled, startParticles)
onBeforeUnmount(stopParticles)
</script>

<style scoped>
.welcome-gate { --ease-out: cubic-bezier(0.23, 1, 0.32, 1); position: fixed; inset: 0; z-index: 2000; overflow: hidden; display: grid; place-items: center; isolation: isolate; background: #171d2d; color: #f8f0e6; }
.welcome-backdrop, .welcome-vignette, .welcome-canvas { position: absolute; inset: 0; pointer-events: none; }
.welcome-backdrop { z-index: -3; background-image: var(--welcome-bg-image), radial-gradient(circle at 76% 16%, rgba(224, 193, 158, .2), transparent 30%), linear-gradient(135deg, #2a3553 0%, #172136 55%, #101725 100%); background-size: cover, auto, auto; background-position: center, center, center; transform: scale(1.04); filter: saturate(.86) contrast(1.04); }
.welcome-vignette { z-index: -2; background: radial-gradient(circle at 50% 42%, transparent 0%, rgba(13, 18, 31, .12) 44%, rgba(8, 12, 23, .7) 100%), linear-gradient(90deg, rgba(8, 12, 22, .7), transparent 52%, rgba(8, 12, 22, .24)); }
.welcome-canvas { z-index: -1; opacity: .72; }
.welcome-pointer-glow { position: absolute; inset: -20%; z-index: -1; pointer-events: none; background: radial-gradient(circle at var(--pointer-x, 50%) var(--pointer-y, 45%), rgba(239, 209, 170, .2), transparent 23%, transparent 42%); opacity: .68; }
.welcome-content { width: min(680px, calc(100vw - 40px)); margin-top: -2vh; text-align: center; display: flex; flex-direction: column; align-items: center; animation: welcome-rise .85s var(--ease-out) both; will-change: transform; }
.welcome-avatar-shell { position: relative; width: 172px; height: 172px; margin-bottom: 22px; display: grid; place-items: center; filter: drop-shadow(0 18px 32px rgba(4, 9, 20, .4)); }
.welcome-avatar { width: 126px; height: 126px; border-radius: 50%; overflow: hidden; display: grid; place-items: center; background: linear-gradient(140deg, rgba(252,235,209,.95), rgba(183, 202, 193, .82)); color: #3f4a51; font: 500 42px/1 Georgia, serif; border: 2px solid rgba(255, 244, 225, .75); box-shadow: inset 0 0 22px rgba(255,255,255,.32); }
.welcome-avatar img { width: 100%; height: 100%; object-fit: cover; }
.welcome-avatar-ring { position: absolute; inset: 8px; border-radius: 50%; border: 1px solid color-mix(in srgb, var(--welcome-accent) 72%, white 28%); opacity: .82; }
.ring-one { inset: 4px; border-width: 1px 3px 1px 1px; transform: rotate(-30deg); animation: ring-float 8s ease-in-out infinite; }
.ring-two { inset: 0; border-color: rgba(255, 233, 198, .65); border-style: dashed; border-spacing: 8px; transform: rotate(34deg); animation: ring-float 11s ease-in-out infinite reverse; }
.ring-three { inset: 14px; border-color: rgba(206, 226, 216, .76); border-width: 2px 1px 1px 2px; transform: rotate(112deg); animation: ring-float 7s ease-in-out infinite reverse; }
.welcome-ring-dot { position: absolute; width: 7px; height: 7px; border-radius: 50%; background: var(--welcome-accent); box-shadow: 0 0 12px color-mix(in srgb, var(--welcome-accent) 70%, white 30%); }
.dot-one { top: 9px; right: 35px; animation: dot-breathe 3.8s ease-in-out infinite; }
.dot-two { right: 8px; bottom: 52px; animation: dot-breathe 4.3s ease-in-out .8s infinite; }
.dot-three { left: 17px; bottom: 33px; animation: dot-breathe 3.2s ease-in-out 1.2s infinite; }
.is-static .welcome-avatar-ring, .is-static .welcome-ring-dot { animation: none; }
.welcome-name { margin: 0 0 11px; color: rgba(250, 232, 209, .92); letter-spacing: .24em; font-size: 13px; text-transform: uppercase; }
.welcome-content h1 { margin: 0; max-width: 680px; font: 400 clamp(28px, 4vw, 48px)/1.18 Georgia, 'Songti SC', serif; letter-spacing: .08em; text-shadow: 0 5px 30px rgba(10, 16, 30, .4); }
.welcome-subtitle { margin: 15px 0 0; color: rgba(248, 239, 227, .88); font: 300 15px/1.8 Georgia, 'Songti SC', serif; letter-spacing: .18em; }
.welcome-description { margin: 9px 0 24px; color: rgba(229, 224, 219, .72); font-size: 13px; letter-spacing: .08em; }
.welcome-enter { position: relative; isolation: isolate; overflow: hidden; border: 1px solid transparent; border-radius: 999px; padding: 12px 23px 12px 27px; display: inline-flex; align-items: center; gap: 15px; color: #fff8ee; background: rgba(29, 38, 59, .3); backdrop-filter: blur(10px); box-shadow: 0 10px 28px rgba(6, 11, 23, .22), inset 0 0 0 1px rgba(255,255,255,.06); cursor: pointer; font: 500 14px/1 'Songti SC', serif; letter-spacing: .16em; transition: transform 180ms var(--ease-out), background 180ms var(--ease-out), box-shadow 180ms var(--ease-out); }
.welcome-enter::before { content: ''; position: absolute; inset: -1px; z-index: 0; padding: 1px; border-radius: inherit; background: conic-gradient(from 0deg, transparent 0deg, color-mix(in srgb, var(--welcome-accent) 70%, white 30%) 42deg, #e9b5d0 104deg, #a9d8dc 166deg, transparent 224deg, transparent 360deg); -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0); -webkit-mask-composite: xor; mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0); mask-composite: exclude; animation: welcome-border-flow 4.8s linear infinite; }
.welcome-enter > * { position: relative; z-index: 1; }
.welcome-enter:focus-visible { outline: 2px solid #fff8ee; outline-offset: 4px; }
.welcome-enter-arrow { font-size: 18px; line-height: .7; transition: transform 160ms var(--ease-out); }
.welcome-gate.is-static .welcome-enter::before { animation: none; }
.welcome-hint { margin: 17px 0 0; color: rgba(232, 224, 214, .5); font-size: 11px; letter-spacing: .12em; }
.welcome-fade-enter-active, .welcome-fade-leave-active { transition: opacity .7s ease; }
.welcome-fade-enter-from, .welcome-fade-leave-to { opacity: 0; }
@keyframes welcome-rise { from { opacity: 0; transform: translateY(22px); } to { opacity: 1; transform: translateY(0); } }
@keyframes ring-float { 0%, 100% { transform: rotate(-30deg) scale(1); opacity: .62; } 50% { transform: rotate(-12deg) scale(1.035); opacity: 1; } }
@keyframes dot-breathe { 0%, 100% { transform: scale(.75); opacity: .55; } 50% { transform: scale(1.45); opacity: 1; } }
@keyframes welcome-border-flow { to { transform: rotate(360deg); } }
@media (hover: hover) and (pointer: fine) { .welcome-enter:hover { transform: translateY(-3px); background: color-mix(in srgb, var(--welcome-accent) 24%, rgba(29, 38, 59, .42)); box-shadow: 0 15px 36px rgba(6, 11, 23, .34), 0 0 28px color-mix(in srgb, var(--welcome-accent) 20%, transparent); } .welcome-enter:hover .welcome-enter-arrow { transform: translate(3px, -3px); } }
@media (prefers-reduced-motion: reduce) { .welcome-content, .welcome-avatar-ring, .welcome-ring-dot { animation: none; } .welcome-pointer-glow { opacity: .24; } .welcome-enter { transition: none; } .welcome-enter::before { animation: none; } }
@media (max-width: 600px) { .welcome-avatar-shell { width: 142px; height: 142px; margin-bottom: 17px; } .welcome-avatar { width: 106px; height: 106px; } .welcome-content h1 { letter-spacing: .04em; } .welcome-subtitle { letter-spacing: .08em; } }
</style>
