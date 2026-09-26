let seed = 20260926
export function rnd() {
  seed |= 0; seed = (seed + 0x6d2b79f5) | 0
  let t = Math.imul(seed ^ (seed >>> 15), 1 | seed)
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296
}
export const pick = arr => arr[Math.floor(rnd() * arr.length)]
export const int = (min, max) => Math.floor(rnd() * (max - min + 1)) + min
export const intId = () => int(10000, 99999)
export function pad(n) { return n < 10 ? '0' + n : '' + n }
export function dateStr(offsetDays = 0, withTime = true) {
  const d = new Date(2026, 8, 26 - offsetDays, int(8, 22), int(0, 59), int(0, 59))
  const base = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  return withTime ? `${base} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}` : base
}
export function genRows(n, fn) {
  const arr = []
  for (let i = 0; i < n; i++) arr.push(fn(i))
  return arr
}
export function paginate(arr, page = 1, size = 10) {
  return { list: arr.slice((page - 1) * size, page * size), total: arr.length, page, size }
}
export function genTrend(days = 30, base = 100, amp = 40, fmt = v => Math.round(v)) {
  const arr = []
  for (let i = days - 1; i >= 0; i--) {
    arr.push({ date: dateStr(i, false).slice(5), value: fmt(base + rnd() * amp) })
  }
  return arr
}
export const owners = ['张伟', '李娜', '王强', '刘洋', '陈静', '赵磊', '孙悦', '周杰', '吴敏', '郑浩']
export const depts = ['数据治理部', '数据开发部', '数据分析部', 'AI应用部', '运维保障部', '业务运营部']
