import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const icons = require('@element-plus/icons-vue')
const validIcons = new Set(Object.keys(icons))

const root = join(dirname(fileURLToPath(import.meta.url)), '..', 'src')

const builtins = new Set([
  'Transition', 'KeepAlive', 'Teleport', 'Suspense', 'Component', 'RouterView', 'RouterLink',
  'PageHeader', 'StatCard', 'EChart', 'ElIcon'
])

function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    const s = statSync(p)
    if (s.isDirectory()) walk(p, out)
    else if (name.endsWith('.vue')) out.push(p)
  }
  return out
}

const localComponents = new Set()
for (const f of walk(join(root, 'components'))) localComponents.add(f.replace(/\.vue$/, '').split(/[\\/]/).pop())

const problems = []

for (const file of walk(root)) {
  const content = readFileSync(file, 'utf8')
  const rel = file.split(/[\\/]src[\\/]/)[1]

  // script setup 中 import 的标识符（任何来源）
  const imported = new Set()
  const importRe = /import\s+(?:\{([^}]+)\}|(\w+))\s+from/g
  let im
  while ((im = importRe.exec(content))) {
    if (im[1]) im[1].split(',').forEach(s => {
      const n = s.trim().split(/\s+as\s+/).pop().trim()
      if (n) imported.add(n)
    })
    if (im[2]) imported.add(im[2])
  }

  // 模板区（首个 <template> 到 </script> 之间取 template 部分）
  const tplStart = content.indexOf('<template>')
  const tplEnd = content.indexOf('</template>\n\n<script') !== -1
    ? content.indexOf('</template>\n\n<script')
    : content.lastIndexOf('</template>')
  const tpl = content.slice(tplStart, tplEnd)

  // 找出大写开头的组件标签 <Xxx /> 或 <Xxx ...>
  const tagRe = /<([A-Z][A-Za-z0-9]*)((\s[^>]*)?)\/?>(?:<\/\1>)?/g
  const used = new Set()
  let m
  while ((m = tagRe.exec(tpl))) used.add(m[1])

  for (const tag of used) {
    if (validIcons.has(tag)) continue
    if (imported.has(tag)) continue
    if (builtins.has(tag)) continue
    if (localComponents.has(tag)) continue
    if (tag.startsWith('El')) continue
    problems.push(`${rel}: <${tag} />`)
  }
}

if (!problems.length) console.log('OK: no unresolved component tags')
else {
  problems.forEach(p => console.log(p))
  console.log(`\n${problems.length} unresolved tag(s)`)
}
