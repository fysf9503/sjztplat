import fs from 'node:fs'
import path from 'node:path'

const root = 'data-ui/src'
const garbled = []
const punct = new Map()

function walk(d) {
  for (const f of fs.readdirSync(d)) {
    const p = path.join(d, f)
    const st = fs.statSync(p)
    if (st.isDirectory()) { walk(p); continue }
    if (!f.endsWith('.vue')) continue
    const c = fs.readFileSync(p, 'utf8')
    if (!c.includes('\uFFFD')) continue
    garbled.push(p)
    for (const ch of c) {
      const cp = ch.codePointAt(0)
      if (cp >= 0x2000 && cp <= 0x303F || cp === 0xFF1A || cp >= 0xFF00 && cp <= 0xFFEF) {
        punct.set(ch, (punct.get(ch) || 0) + 1)
      }
    }
  }
}

walk(root)
console.log('garbled files:', garbled.length)
console.log('=== surviving fullwidth/CJK punctuation in garbled files ===')
for (const [ch, n] of [...punct.entries()].sort((a, b) => b[1] - a[1])) {
  console.log(n.toString().padStart(5), 'U+' + ch.codePointAt(0).toString(16).toUpperCase().padStart(4, '0'), JSON.stringify(ch))
}
