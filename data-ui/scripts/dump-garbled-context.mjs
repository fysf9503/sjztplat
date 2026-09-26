import { readFileSync } from 'node:fs'
import { execSync } from 'node:child_process'

const files = execSync('rg -l "\\uFFFD" src scripts', { encoding: 'utf8', cwd: process.cwd() }).trim().split(/\r?\n/)
for (const f of files) {
  const lines = readFileSync(f, 'utf8').split(/\r?\n/)
  const idx = new Set()
  lines.forEach((l, i) => { if (l.includes('\uFFFD')) { idx.add(i - 1); idx.add(i); idx.add(i + 1) } })
  console.log(`\n===== ${f} =====`)
  let prev = -2
  for (const i of [...idx].sort((a, b) => a - b)) {
    if (i < 0 || i >= lines.length) continue
    if (i > prev + 1) console.log('  ...')
    console.log(`${i + 1}: ${lines[i]}`)
    prev = i
  }
}
