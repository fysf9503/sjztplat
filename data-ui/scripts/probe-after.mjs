import fs from 'node:fs'
import path from 'node:path'

const root = 'data-ui/src'
const after = new Map()

function walk(d) {
  for (const f of fs.readdirSync(d)) {
    const p = path.join(d, f)
    const st = fs.statSync(p)
    if (st.isDirectory()) { walk(p); continue }
    if (!f.endsWith('.vue')) continue
    const c = fs.readFileSync(p, 'utf8')
    for (let i = 0; i < c.length; i++) {
      if (c[i] === '\uFFFD') {
        const tail = c.slice(i + 1, i + 3).replace(/\r/g, '\\r').replace(/\n/g, '\\n')
        after.set(tail, (after.get(tail) || 0) + 1)
      }
    }
  }
}

walk(root)
const arr = [...after.entries()].sort((a, b) => b[1] - a[1])
for (const [k, n] of arr) console.log(n.toString().padStart(4), JSON.stringify(k))
console.log('distinct tails:', arr.length)
