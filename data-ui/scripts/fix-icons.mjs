import { readFileSync, writeFileSync, readdirSync, statSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const icons = require('@element-plus/icons-vue')
const validIcons = new Set(Object.keys(icons))

const root = join(dirname(fileURLToPath(import.meta.url)), '..', 'src')

// 无效图标名 -> 有效替代
const aliasMap = {
  Undo: 'RefreshLeft',
  Redo: 'RefreshRight',
  SelectAll: 'Finished'
}

function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    const s = statSync(p)
    if (s.isDirectory()) walk(p, out)
    else if (name.endsWith('.vue')) out.push(p)
  }
  return out
}

const files = walk(root)
let fixedCount = 0
const invalidReport = []

for (const file of files) {
  let content = readFileSync(file, 'utf8')
  const rel = file.split(/[\\/]src[\\/]/)[1]
  let changed = false

  // 1. 替换无效图标名（仅限绑定属性与组件标签处）
  for (const [bad, good] of Object.entries(aliasMap)) {
    const patterns = [
      [`:${'prefix-'}icon="${bad}"`, `:${'prefix-'}icon="${good}"`],
      [`:${'suffix-'}icon="${bad}"`, `:${'suffix-'}icon="${good}"`],
      [`:icon="${bad}"`, `:icon="${good}"`],
      [`<${bad} `, `<${good} `],
      [`<${bad}/>`, `<${good}/>`],
      [`<${bad} />`, `<${good} />`],
      [`</${bad}>`, `</${good}>`]
    ]
    for (const [from, to] of patterns) {
      if (content.includes(from)) {
        content = content.split(from).join(to)
        changed = true
      }
    }
  }

  // 2. 收集绑定中使用的大写标识符
  const bindingRe = /:(?:prefix-|suffix-)?icon="([A-Z][A-Za-z0-9]*)"/g
  const used = new Set()
  let m
  while ((m = bindingRe.exec(content))) used.add(m[1])

  if (!used.size) {
    if (changed) { writeFileSync(file, content, 'utf8'); fixedCount++ }
    continue
  }

  // 3. 校验图标有效性
  for (const id of used) {
    if (!validIcons.has(id)) invalidReport.push(`${rel}: ${id}`)
  }
  const importable = [...used].filter(id => validIcons.has(id))
  if (!importable.length) continue

  // 4. 检查现有 import
  const scriptMatch = content.match(/<script setup>([\s\S]*?)<\/script>/)
  if (!scriptMatch) continue
  const scriptBody = scriptMatch[1]

  const existingImportRe = /import\s*\{([^}]+)\}\s*from\s*['"]@element-plus\/icons-vue['"]/
  const existing = scriptBody.match(existingImportRe)
  const alreadyImported = new Set()
  if (existing) {
    existing[1].split(',').forEach(s => {
      const n = s.trim().split(/\s+as\s+/)[0].trim()
      if (n) alreadyImported.add(n)
    })
  }

  const toAdd = importable.filter(id => !alreadyImported.has(id))
  const aliasAdd = Object.entries(aliasMap)
    .filter(([bad]) => used.has(bad))
    .map(([, good]) => good)
    .filter(good => !alreadyImported.has(good) && !toAdd.includes(good))
  const finalAdd = [...new Set([...toAdd, ...aliasAdd])]

  if (finalAdd.length) {
    const importLine = `import { ${finalAdd.sort().join(', ')} } from '@element-plus/icons-vue'`
    if (existing) {
      const merged = [...new Set([...alreadyImported, ...finalAdd])].sort()
      const newImport = `import { ${merged.join(', ')} } from '@element-plus/icons-vue'`
      content = content.replace(existing[0], newImport)
    } else {
      // 插入到最后一个 import 之后；没有 import 则紧跟 <script setup>
      const imports = [...scriptBody.matchAll(/^import[^\n]*$/gm)]
      if (imports.length) {
        const last = imports[imports.length - 1]
        const idx = content.indexOf(last[0]) + last[0].length
        content = content.slice(0, idx) + '\n' + importLine + content.slice(idx)
      } else {
        content = content.replace('<script setup>', '<script setup>\n' + importLine)
      }
    }
    changed = true
  }

  if (changed) {
    writeFileSync(file, content, 'utf8')
    fixedCount++
  }
}

console.log(`fixed: ${fixedCount} files`)
if (invalidReport.length) {
  console.log('\nSTILL INVALID (not imported, need manual fix):')
  invalidReport.forEach(l => console.log('  ' + l))
} else {
  console.log('all binding icons are valid')
}
