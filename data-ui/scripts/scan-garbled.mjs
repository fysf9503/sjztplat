import { readdirSync, readFileSync, statSync } from 'fs'
import { join } from 'path'

const SKIP = new Set(['node_modules', 'dist', '.git'])

function walk(dir, files = []) {
  for (const f of readdirSync(dir)) {
    if (SKIP.has(f)) continue
    const p = join(dir, f)
    if (statSync(p).isDirectory()) walk(p, files)
    else if (/\.(vue|js|ts|mjs|scss|css|json|html|md)$/.test(f)) files.push(p)
  }
  return files
}

const files = walk('src').concat(walk('scripts'), walk('.').filter(f => f.endsWith('.html') || f.endsWith('.json') || f.endsWith('.md')))

const fileCounts = []
const patMap = new Map()
const lineSamples = []
let total = 0

for (const f of files) {
  let c
  try { c = readFileSync(f, 'utf8') } catch { continue }
  if (!c.includes('\uFFFD')) continue
  const lines = c.split(/\r?\n/)
  let count = 0
  for (let i = 0; i < lines.length; i++) {
    const n = (lines[i].match(/\uFFFD/g) || []).length
    if (!n) continue
    count += n
    if (lineSamples.length < 400) lineSamples.push(`${f}:${i + 1}: ${lines[i].trim().slice(0, 160)}`)
    for (const m of lines[i].matchAll(/([\u4e00-\u9fa5]{0,4})\uFFFD([^\uFFFD]{0,3})/g)) {
      const key = m[1] + '◆' + m[2]
      if (!patMap.has(key)) patMap.set(key, [])
      patMap.get(key).push(`${f}:${i + 1}`)
    }
  }
  fileCounts.push([count, f])
  total += count
}

fileCounts.sort((a, b) => b[0] - a[0])
console.log('=== Files with U+FFFD ===')
for (const [n, f] of fileCounts) console.log(String(n).padStart(4), f)
console.log('TOTAL:', total, 'in', fileCounts.length, 'files')
console.log('\n=== Patterns (prefix ◆ suffix) sorted by count ===')
const pats = [...patMap.entries()].sort((a, b) => b[1].length - a[1].length)
for (const [k, locs] of pats) {
  console.log(String(locs.length).padStart(4), JSON.stringify(k), locs.length <= 3 ? locs.join(' ') : `${locs[0]} (+${locs.length - 1})`)
}

console.log('\n=== Dollar-loss candidates ({xxx. without $ or {{) ===')
const dollarRe = /(^|[^$({\w])\{((?:row|item|record|form|query|node|task|col|scope|s|d|a|m|tab|file|chat|msg|agent|app|log|chart|legend|opt|g|p|e|k|v|t|x|y)\.[\w.]+)/g
for (const f of files) {
  let c
  try { c = readFileSync(f, 'utf8') } catch { continue }
  const lines = c.split(/\r?\n/)
  for (let i = 0; i < lines.length; i++) {
    const m = lines[i].match(dollarRe)
    if (m) console.log(`${f}:${i + 1}:`, m.map(s => s.trim()).join(' | '))
  }
}

console.log('\n=== First 200 garbled lines ===')
for (const l of lineSamples.slice(0, 200)) console.log(l)
