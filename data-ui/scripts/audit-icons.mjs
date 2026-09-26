import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..', 'src')

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
const problems = []

for (const file of files) {
  const content = readFileSync(file, 'utf8')
  const rel = file.split(/[\\/]src[\\/]/)[1]

  // 找出所有 :icon="Xxx" / :prefix-icon / :suffix-icon 绑定中的大写开头标识符
  const bindingRe = /:(?:prefix-)?(?:suffix-)?icon="([A-Z][A-Za-z0-9]*)"|:icon="([A-Z][A-Za-z0-9]*)"/g
  const used = new Set()
  let m
  while ((m = bindingRe.exec(content))) {
    const id = m[1] || m[2]
    if (id) used.add(id)
  }

  // 找出 script setup 中的 import 语句
  const scriptMatch = content.match(/<script setup>([\s\S]*?)<\/script>/)
  const imported = new Set()
  if (scriptMatch) {
    const importRe = /import\s*\{([^}]+)\}\s*from\s*['"]@element-plus\/icons-vue['"]/g
    let im
    while ((im = importRe.exec(scriptMatch[1]))) {
      im[1].split(',').forEach(s => {
        const name = s.trim().split(/\s+as\s+/)[0].trim()
        if (name) imported.add(name)
      })
    }
  }

  const missing = [...used].filter(id => !imported.has(id))
  if (missing.length) problems.push({ file: rel, missing })
}

if (!problems.length) {
  console.log('OK: all icon bindings are imported')
} else {
  for (const p of problems) {
    console.log(`${p.file}:`)
    p.missing.forEach(id => console.log(`  - ${id}`))
  }
  console.log(`\n${problems.length} file(s) with unimported icon bindings`)
}
