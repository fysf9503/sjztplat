import fs from 'node:fs'
import path from 'node:path'

const root = 'data-ui/src'
const seen = new Set()

function walk(d) {
  for (const f of fs.readdirSync(d)) {
    const p = path.join(d, f)
    const st = fs.statSync(p)
    if (st.isDirectory()) { walk(p); continue }
    if (!f.endsWith('.vue')) continue
    const c = fs.readFileSync(p, 'utf8')
    if (c.includes('\uFFFD')) continue
    for (const m of c.matchAll(/(?:label|placeholder|title)="([^"]*(?:负责人|状态|名称|登录|长度|提供)[^"]*)"/g)) {
      const key = m[1]
      if (!seen.has(key)) {
        seen.add(key)
        console.log(JSON.stringify(key))
      }
    }
  }
}

walk(root)
