import fs from 'node:fs'
import path from 'node:path'

const root = 'data-ui/src'

function walk(d, cb) {
  for (const f of fs.readdirSync(d)) {
    const p = path.join(d, f)
    const st = fs.statSync(p)
    if (st.isDirectory()) { walk(p, cb); continue }
    if (f.endsWith('.vue')) cb(p)
  }
}

const units = new Set()
const colonLabels = new Set()
walk(root, p => {
  const c = fs.readFileSync(p, 'utf8')
  const bad = c.includes('\uFFFD')
  for (const m of c.matchAll(/unit="([^"]*)"/g)) {
    if (!bad) units.add(m[1])
    else if (!m[1].includes('\uFFFD')) units.add('[GARBLEDFILE] ' + m[1])
  }
  for (const m of c.matchAll(/(?:label|placeholder|title)="([^"]*[\u4e00-\u9fa5]:)"/g)) {
    colonLabels.add((bad ? '[G] ' : '') + m[1])
  }
})

console.log('=== unit values in clean files ===')
console.log([...units].sort().join('\n'))
console.log('=== labels ending with ASCII colon ===')
console.log([...colonLabels].sort().slice(0, 30).join('\n') || '(none)')
