import fs from 'node:fs'
import path from 'node:path'

const root = 'data-ui/src'
const targets = ["'$", "'+", " S", " R", " H", " A", " (", " (", " 8", " 9", " 7", " 1", " 3", " 2", " 4", ":'", ": ", " {", "  ", "..", " <s", " <", "<o", "<r", "<e", "?-", "?*"]

function walk(d) {
  for (const f of fs.readdirSync(d)) {
    const p = path.join(d, f)
    const st = fs.statSync(p)
    if (st.isDirectory()) { walk(p); continue }
    if (!f.endsWith('.vue')) continue
    const c = fs.readFileSync(p, 'utf8')
    if (!c.includes('\uFFFD')) continue
    const lines = c.split(/\r?\n/)
    lines.forEach((l, i) => {
      for (let j = 0; j < l.length; j++) {
        if (l[j] === '\uFFFD') {
          const tail = l.slice(j + 1, j + 3)
          if (targets.includes(tail)) {
            console.log(`${p}:${i + 1} tail=${JSON.stringify(tail)} :: ${l.trim().slice(0, 150)}`)
          }
        }
      }
    })
  }
}

walk(root)
