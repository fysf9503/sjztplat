import fs from 'node:fs'
import path from 'node:path'

const root = 'data-ui/src'
let colonAttr = 0
let colonElse = 0
const samples = []

function walk(d) {
  for (const f of fs.readdirSync(d)) {
    const p = path.join(d, f)
    const st = fs.statSync(p)
    if (st.isDirectory()) { walk(p); continue }
    if (!f.endsWith('.vue')) continue
    const c = fs.readFileSync(p, 'utf8')
    if (!c.includes('\uFFFD')) continue
    for (const m of c.matchAll(/[\u4e00-\u9fa5]\uFF1A[^\r\n]{0,6}/g)) {
      if (samples.length < 15) samples.push(m[0])
    }
    for (const m of c.matchAll(/="[^"]*[\u4e00-\u9fa5]\uFF1A"/g)) colonAttr++
    const attrStripped = c.replace(/="[^"]*"/g, '')
    colonElse += (attrStripped.match(/\uFF1A/g) || []).length
  }
}

walk(root)
console.log('fullwidth colon in attrs:', colonAttr)
console.log('fullwidth colon elsewhere:', colonElse)
console.log(samples.join('\n'))
