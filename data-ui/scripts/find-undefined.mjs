// 扫描模板中引用但 <script setup> 未声明的标识符（未定义变量 -> 运行时崩溃）
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

const GLOBALS = new Set([
  'true', 'false', 'null', 'undefined', 'Infinity', 'NaN',
  'Math', 'Date', 'JSON', 'Number', 'String', 'Boolean', 'Array', 'Object',
  'parseInt', 'parseFloat', 'isNaN', 'console', 'window', 'document',
  '$event', '$slots', '$attrs', '$emit', '$refs', '$router', '$route', '$el',
  'event', 'arguments', 'location'
])

const VUE_GLOBAL_COMPONENTS = new Set([
  'component', 'transition', 'keep-alive', 'teleport', 'suspense',
  'el-icon', 'transition-group'
])

function stripStrings(code) {
  return code.replace(/'(?:[^'\\]|\\.)*'/g, "''").replace(/"(?:[^"\\]|\\.)*"/g, '""').replace(/`(?:[^`\\]|\\.)*`/g, '``')
}

function extractExprs(template) {
  const exprs = []
  // {{ ... }}
  for (const m of template.matchAll(/\{\{([\s\S]*?)\}\}/g)) exprs.push(m[1])
  // 指令属性值（:prop / v-if / v-for / @event 等）
  for (const m of template.matchAll(/(?:^|\s)(?::[\w.:-]+|v-if|v-else-if|v-show|v-for|v-model(?::[\w-]+)?|v-html|v-text|@[\w.:-]+)="([^"]*)"/g)) {
    exprs.push(m[1])
  }
  return exprs.map(stripStrings)
}

// 模板局部作用域声明：v-for="item in list" / v-for="(item, i) in list"
function collectTemplateScope(template) {
  const scope = new Set()
  for (const m of template.matchAll(/v-for="([^"]*)"/g)) {
    const decl = m[1].split(/\s+in\s+/)[0].trim().replace(/^\(|\)$/g, '')
    for (const name of decl.split(',')) {
      const n = name.trim()
      if (n && /^[A-Za-z_$][\w$]*$/.test(n)) scope.add(n)
    }
  }
  // 作用域插槽：#default="{ row }" / #default="scope"
  for (const m of template.matchAll(/#[\w-]+="(\{[^}]*\}|[A-Za-z_$][\w$]*)"/g)) {
    const v = m[1].trim()
    if (v.startsWith('{')) {
      for (const part of v.slice(1, -1).split(',')) {
        const n = part.split(':')[0].trim()
        if (n && /^[A-Za-z_$][\w$]*$/.test(n)) scope.add(n)
      }
    } else if (/^[A-Za-z_$][\w$]*$/.test(v)) scope.add(v)
  }
  return scope
}

function collectSetupBindings(script) {
  const binds = new Set()
  for (const m of script.matchAll(/^import\s+(?:([\w$]+)\s*,\s*)?(?:\{([^}]*)\}|([\w$]+)|\*\s+as\s+([\w$]+))\s+from/gm)) {
    if (m[1]) binds.add(m[1])
    if (m[2]) for (const s of m[2].split(',')) {
      const n = s.trim().split(/\s+as\s+/).pop().trim()
      if (n) binds.add(n)
    }
    if (m[3]) binds.add(m[3])
    if (m[4]) binds.add(m[4])
  }
  for (const m of script.matchAll(/^(?:const|let|var)\s+([\w$,\s${}]+?)\s*(?:=|;|$)/gm)) {
    for (const part of m[1].split(',')) {
      let n = part.trim()
      const destructure = n.match(/^\{(.*)\}$/) || n.match(/^\[(.*)\]$/)
      if (destructure) {
        for (const d of destructure[1].split(',')) {
          n = d.trim().split(':')[0].trim().split(/\s+as\s+/).pop().trim()
          if (/^[A-Za-z_$][\w$]*$/.test(n)) binds.add(n)
        }
      } else if (/^[A-Za-z_$][\w$]*$/.test(n)) binds.add(n)
    }
  }
  for (const m of script.matchAll(/^(?:async\s+)?function\s*\*?\s*([\w$]+)/gm)) binds.add(m[1])
  return binds
}

let issues = 0
for (const f of walk('src/views').concat(walk('src/layout'), walk('src/components'))) {
  const src = readFileSync(f, 'utf8')
  const tpl = src.match(/<template>([\s\S]*)<\/template>/)
  const script = src.match(/<script setup>([\s\S]*)<\/script>/)
  if (!tpl || !script) continue

  const binds = collectSetupBindings(script[1])
  const scope = collectTemplateScope(tpl[1])

  for (const expr of extractExprs(tpl[1])) {
    // 跳过 v-for 的列表源（右侧 in 后面部分在 v-for 声明处理）
    const clean = expr.replace(/\bin\b\s+[\w$.]+$/, '')
    for (const m of clean.matchAll(/[A-Za-z_$][\w$]*/g)) {
      const id = m[0]
      const before = clean[m.index - 1]
      const after = clean[m.index + id.length]
      // 排除属性访问（.xxx / ?.xxx）和对象字面量键（xxx:）
      if (before === '.' || (before === '?' && clean[m.index - 2] === '.') || after === ':') continue
      if (GLOBALS.has(id) || binds.has(id) || scope.has(id)) continue
      console.log(`${f}: 未声明标识符 "${id}" 出现在表达式: ${expr.trim().slice(0, 80)}`)
      issues++
    }
  }
}
console.log(`\nscan done, ${issues} suspicious usages`)
