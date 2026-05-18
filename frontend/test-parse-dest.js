/**
 * parseDestForCharge 单元测试
 * 运行: node test-parse-dest.js
 */

const parseDestForCharge = (dest) => {
  const lines = (dest || '').split('\n').map(l => l.trim()).filter(Boolean)
  let portParts = []
  let warehouse = ''
  for (const line of lines) {
    const whMatch = line.match(/[（(]([^，,)）]*?(?:直拼|仓))/)
    if (whMatch) {
      warehouse = whMatch[1].trim()
      // 如果仓库在同一行，提取仓库前面的港口名
      const before = line.substring(0, whMatch.index).replace(/[（(].*/, '').trim()
      if (before) portParts.push(before)
      break
    }
    portParts.push(line.replace(/[（(].*/, '').trim())
  }
  const port = portParts.join(' ').replace(/[/,]\s*$/, '').trim()
  return { port, warehouse, full: warehouse ? port + '(' + warehouse + ')' : port }
}

// ── 测试用例 ──
const tests = [
  // 1. BANGKOK 滘心直拼（有逗号分隔）
  { input: 'BANGKOK(PAT)\n(滘心直拼，低收)', expected: { port: 'BANGKOK', warehouse: '滘心直拼', full: 'BANGKOK(滘心直拼)' } },
  // 2. BANGKOK 南沙直拼
  { input: 'BANGKOK(PAT)\n(南沙直拼，高收)', expected: { port: 'BANGKOK', warehouse: '南沙直拼', full: 'BANGKOK(南沙直拼)' } },
  // 3. KAOHSIUNG 窖心直拼（无逗号，直接括号结尾）
  { input: 'KAOHSIUNG\n(窖心直拼)', expected: { port: 'KAOHSIUNG', warehouse: '窖心直拼', full: 'KAOHSIUNG(窖心直拼)' } },
  // 4. NEW YORK 3行（子港口+仓库，含全角括号）
  { input: 'NEW YORK, NY\n（ NEWARK,NJ ）\n（乌冲直拼）', expected: { port: 'NEW YORK, NY', warehouse: '乌冲直拼', full: 'NEW YORK, NY(乌冲直拼)' } },
  // 5. DUBAI 乌冲直拼
  { input: 'DUBAI/JEBEL ALI\n(乌冲直拼)', expected: { port: 'DUBAI/JEBEL ALI', warehouse: '乌冲直拼', full: 'DUBAI/JEBEL ALI(乌冲直拼)' } },
  // 6. 北沙仓库
  { input: 'SINGAPORE\n(北沙直拼)', expected: { port: 'SINGAPORE', warehouse: '北沙直拼', full: 'SINGAPORE(北沙直拼)' } },
  // 7. 无仓库
  { input: 'TOKYO', expected: { port: 'TOKYO', warehouse: '', full: 'TOKYO' } },
  // 8. KAOHSIUNG 只有港口
  { input: 'KAOHSIUNG', expected: { port: 'KAOHSIUNG', warehouse: '', full: 'KAOHSIUNG' } },
  // 9. 同一行含仓库（极端 case）
  { input: 'BANGKOK(PAT) (滘心直拼 高收)', expected: { port: 'BANGKOK', warehouse: '滘心直拼', full: 'BANGKOK(滘心直拼)' } },
  // 10. 空字符串
  { input: '', expected: { port: '', warehouse: '', full: '' } },
  // 11. LAEM CHABANG
  { input: 'LAEM CHABANG\n(窖心直拼)', expected: { port: 'LAEM CHABANG', warehouse: '窖心直拼', full: 'LAEM CHABANG(窖心直拼)' } },
]

let passed = 0
let failed = 0

for (const t of tests) {
  const result = parseDestForCharge(t.input)
  const ok = result.port === t.expected.port
    && result.warehouse === t.expected.warehouse
    && result.full === t.expected.full
  
  if (ok) {
    passed++
  } else {
    failed++
    console.log(`❌ FAIL: "${t.input.replace(/\n/g, '\\n')}"`)
    console.log(`   expected:`, JSON.stringify(t.expected))
    console.log(`   got:     `, JSON.stringify(result))
  }
}

console.log(`\n${passed}/${tests.length} passed, ${failed} failed`)
if (passed === tests.length) console.log('🎉 All tests passed!')
process.exit(failed > 0 ? 1 : 0)
