import { readFileSync, writeFileSync, readdirSync, statSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const COLORS = {
  'dp-primary': '#1664ff',
  'dp-primary-light': '#e8f0ff',
  'dp-primary-bg': '#f5f9ff',
  'dp-success': '#00b42a',
  'dp-success-light': '#e8ffea',
  'dp-warning': '#ff7d00',
  'dp-warning-light': '#fff3e8',
  'dp-danger': '#f53f3f',
  'dp-danger-light': '#ffece8',
  'dp-purple': '#722ed1',
  'dp-purple-light': '#f5e8ff',
  'dp-cyan': '#0fc6c2',
  'dp-cyan-light': '#e0fffa',
  'dp-pink': '#f759ab',
  'dp-pink-light': '#ffe8f4',
  'dp-text-1': '#1d2129',
  'dp-text-2': '#4e5969',
  'dp-text-3': '#86909c',
  'dp-text-4': '#c9cdd4',
  'dp-border': '#e5e6eb',
  'dp-border-light': '#f2f3f5',
  'dp-bg': '#f2f3f5',
  'dp-bg-page': '#f7f8fa',
  'dp-bg-card': '#ffffff'
}

const root = join(dirname(fileURLToPath(import.meta.url)), '..', 'src')

function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    const s = statSync(p)
    if (s.isDirectory()) walk(p, out)
    else if (name.endsWith('.vue')) out.push(p)
  }
  return out
}

const varRe = /var\(--(dp-[a-z0-9-]+)\)/g
let totalFiles = 0
let totalReplaced = 0

for (const file of walk(root)) {
  const content = readFileSync(file, 'utf8')

  const scriptOpen = content.indexOf('<script setup>')
  if (scriptOpen === -1) continue
  const scriptClose = content.indexOf('</script>', scriptOpen)
  if (scriptClose === -1) continue

  const before = content.slice(0, scriptOpen + '<script setup>'.length)
  const script = content.slice(scriptOpen + '<script setup>'.length, scriptClose)
  const after = content.slice(scriptClose)

  let count = 0
  const fixed = script.replace(varRe, (m, name) => {
    if (COLORS[name]) {
      count++
      return COLORS[name]
    }
    return m
  })

  if (count > 0) {
    writeFileSync(file, before + fixed + after, 'utf8')
    totalFiles++
    totalReplaced += count
    const rel = file.split(/[\\/]src[\\/]/)[1]
    console.log(`${rel}: ${count} replaced`)
  }
}

console.log(`\nTotal: ${totalReplaced} replacements in ${totalFiles} files (script blocks only)`)
