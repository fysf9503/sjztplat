// 扫描 <script setup> 中：常量初始化器引用了后面才声明的 const（TDZ 风险）
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

for (const f of walk('src/views')) {
  const src = readFileSync(f, 'utf8')
  const m = src.match(/<script setup>([\s\S]*?)<\/script>/)
  if (!m) continue
  const body = m[1]
  // 顶层 const 声明（跳过函数体内的）
  const decls = []
  for (const mm of body.matchAll(/^(?:const|let)\s+([A-Za-z_$][\w$]*)\s*=/gm)) {
    decls.push({ name: mm[1], idx: mm.index })
  }
  for (let i = 0; i < decls.length; i++) {
    const d = decls[i]
    // 该声明的初始化表达式范围（到行尾）
    const lineEnd = body.indexOf('\n', d.idx)
    const init = body.slice(d.idx, lineEnd === -1 ? body.length : lineEnd)
    // 检查初始化器中是否引用了后面才声明的顶层变量
    for (let j = i + 1; j < decls.length; j++) {
      const later = decls[j]
      const re = new RegExp(`\\b${later.name}\\b`)
      if (re.test(init)) {
        console.log(`${f}: "${d.name}" 的初始化引用了后声明的 "${later.name}"`)
        console.log(`  > ${init.trim().slice(0, 100)}`)
      }
    }
  }
}
console.log('scan done')
