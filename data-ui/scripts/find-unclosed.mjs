import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'

function walk(dir, out = []) {
  for (const e of readdirSync(dir)) {
    const p = join(dir, e)
    if (statSync(p).isDirectory()) walk(p, out)
    else if (p.endsWith('.vue')) out.push(p)
  }
  return out
}

// attribute value like ="....'中文"  — single-quoted JS string never closed before the closing double quote
const re = /="[^"]*'[^'"]*[\u4e00-\u9fa5]"/g
// also catch: 'xxx" inside template expressions where quote pairs are unbalanced
const lines2check = []

for (const f of walk('src/views')) {
  const lines = readFileSync(f, 'utf8').split(/\r?\n/)
  lines.forEach((l, i) => {
    if (/<el-|:title|:disabled|:label|placeholder|v-model/.test(l)) {
      // count single quotes in attribute sections — unbalanced = suspicious
      const m = l.match(/="[^"]*"/g) || []
      for (const attr of m) {
        const sq = (attr.match(/'/g) || []).length
        if (sq % 2 === 1) lines2check.push(`${f}:${i + 1}: ${l.trim()}`)
      }
    }
  })
}
console.log(lines2check.join('\n') || 'CLEAN')
console.log(`\n${lines2check.length} suspicious lines`)
