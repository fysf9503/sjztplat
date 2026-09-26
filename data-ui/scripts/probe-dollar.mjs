import fs from 'node:fs'

const c = fs.readFileSync('data-ui/src/views/agent/Apps.vue', 'utf8')
const lines = c.split(/\r?\n/)
for (const n of [260, 269]) {
  const l = lines[n - 1]
  const i = l.indexOf('\uFFFD')
  const seg = l.slice(i - 6, i + 14)
  console.log(n, JSON.stringify(seg))
  console.log('   codes:', [...seg].map(ch => ch.codePointAt(0).toString(16).padStart(4, '0')).join(' '))
}
const d = fs.readFileSync('data-ui/src/views/search/Model.vue', 'utf8').split(/\r?\n/)
const m = d[285]
console.log(286, JSON.stringify(m.slice(0, 120)))
