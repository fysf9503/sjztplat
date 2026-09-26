import { readdirSync, readFileSync, statSync } from 'fs'
import { join } from 'path'

const CORRUPTED = new Set([
  'system/Services.vue','system/Projects.vue','system/Knowledge.vue','system/Models.vue',
  'system/Users.vue','system/License.vue','system/Roles.vue',
  'agent/Apps.vue','agent/Knowledge.vue','agent/Planning.vue','agent/Square.vue','agent/Workbench.vue',
  'lab/ModelConfig.vue','lab/Gov.vue','lab/Ecommerce.vue','lab/Datasources.vue','lab/Overview.vue',
  'search/Business.vue','search/Model.vue','search/Search.vue',
  'docs/Templates.vue','docs/Mine.vue'
])

function walk(dir, files = []) {
  for (const f of readdirSync(dir)) {
    const p = join(dir, f)
    if (statSync(p).isDirectory()) walk(p, files)
    else if (f.endsWith('.vue')) files.push(p)
  }
  return files
}

const found = new Map()
for (const f of walk('src/views')) {
  const rel = f.replace(/\\/g, '/').replace('src/views/', '')
  const isCorrupted = CORRUPTED.has(rel)
  const c = readFileSync(f, 'utf8')
  if (c.includes('\uFFFD')) continue
  for (const m of c.matchAll(/(label|placeholder|title)="([^"]*(?:数|列表|工具|知识库|次|条|量)[^"]*)"/g)) {
    const key = m[2]
    if (!found.has(key)) found.set(key, [])
    if (found.get(key).length < 3) found.get(key).push((isCorrupted ? '[C]' : '') + rel)
  }
}
const keys = [...found.keys()].sort()
for (const k of keys) console.log(JSON.stringify(k), '←', found.get(k).join(', '))
