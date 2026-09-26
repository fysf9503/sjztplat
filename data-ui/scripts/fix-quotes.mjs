import { readFileSync, writeFileSync } from 'node:fs'

const FIX = [
  ['src/views/agent/Apps.vue', "=== '已下线\"", "=== '已下线'\""],
  ['src/views/lab/Datasources.vue', ": '新增数据源\"", ": '新增数据源'\""],
  ['src/views/lab/Overview.vue', "=== '运行中\"", "=== '运行中'\""],
  ['src/views/system/Knowledge.vue', "=== '构建中\"", "=== '构建中'\""],
  ['src/views/system/Knowledge.vue', ": '创建知识库\"", ": '创建知识库'\""],
  ['src/views/system/Models.vue', "!== '运行中\"", "!== '运行中'\""],
]

for (const [file, from, to] of FIX) {
  let c = readFileSync(file, 'utf8')
  const n = c.split(from).length - 1
  if (n === 0) { console.log(`MISS ${file}: ${from}`); continue }
  c = c.split(from).join(to)
  writeFileSync(file, c, 'utf8')
  console.log(`OK ${file} x${n}`)
}
