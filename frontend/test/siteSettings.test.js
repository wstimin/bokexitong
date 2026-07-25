import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createHeroTextStyle,
  normalizeHeroColor,
  normalizeHeroTextShadow
} from '../src/utils/heroAppearance.js'

test('hero colors only accept six-digit hex values', () => {
  assert.equal(normalizeHeroColor('#A1B2C3'), '#a1b2c3')
  assert.equal(normalizeHeroColor('red'), '#ffffff')
  assert.equal(normalizeHeroColor('url(javascript:alert(1))'), '#ffffff')
})

test('hero shadow mode uses the supported presets', () => {
  assert.equal(normalizeHeroTextShadow('light'), 'light')
  assert.equal(normalizeHeroTextShadow('custom'), 'dark')
})

test('hero style exposes normalized CSS variables', () => {
  assert.deepEqual(createHeroTextStyle('#111827', '#F8FAFC', 'none'), {
    '--hero-title-color': '#111827',
    '--hero-subtitle-color': '#f8fafc',
    '--hero-title-shadow': 'none',
    '--hero-subtitle-shadow': 'none'
  })
})
