/**
 * 迁移文件自检：不连数据库，只检查 SQL 本身有没有低级错误。
 *   node tools/check-migration.js <sql 文件路径>
 */
const fs = require('fs')
const file = process.argv[2]
const text = fs.readFileSync(file, 'utf8')
const lines = text.split(/\r?\n/)
let problems = 0

function fail(m) { problems++; console.log('  ✗ ' + m) }
function ok(m) { console.log('  ✓ ' + m) }

// 1. 编码 / BOM
const buf = fs.readFileSync(file)
if (buf[0] === 0xef && buf[1] === 0xbb && buf[2] === 0xbf) fail('文件带 BOM，MySQL 可能报错')
else ok('无 BOM，UTF-8')

// 2. 命名
if (!/^V\d+__[A-Za-z0-9_]+\.sql$/.test(require('path').basename(file))) fail('文件名不符合 Flyway 规范')
else ok('命名符合 V<n>__<name>.sql')

// 3. 语句检查
const stmts = lines.filter(function (l) { return l.trim().toUpperCase().startsWith('UPDATE ') })
ok('UPDATE 语句 ' + stmts.length + ' 条')
stmts.forEach(function (s, i) {
  if (!s.trim().endsWith(';')) fail('第 ' + (i + 1) + ' 条没以分号结尾')
  const quotes = (s.match(/'/g) || []).length
  if (quotes % 2 !== 0) fail('第 ' + (i + 1) + ' 条单引号不成对（' + quotes + ' 个）')
  if (!/^UPDATE (game_guides|guide_sections|guide_faqs) SET (title|subtitle|summary|heading|body_markdown|question|answer) = REPLACE\(\2, /.test(s)) {
    fail('第 ' + (i + 1) + ' 条格式不对：' + s.slice(0, 60))
  }
  // 替换前后不能出现同样的词
  const m = s.match(/REPLACE\((\w+), '([\s\S]*)', '([\s\S]*)'\);$/)
  if (m && m[2] === m[3]) fail('第 ' + (i + 1) + ' 条替换前后一样')
})

// 4. 不碰不该碰的东西（注释里的提及不算）
const codeOnly = lines.filter(function (l) { return !l.trim().startsWith('--') }).join('\n')
const forbidden = ['DROP ', 'DELETE ', 'TRUNCATE', 'ALTER ', 'guide_products', 'game_status', 'INSERT ']
forbidden.forEach(function (w) {
  if (codeOnly.toUpperCase().indexOf(w.toUpperCase()) !== -1) fail('出现了不该有的语句/表：' + w)
})
if (!problems) ok('没有 DROP / DELETE / ALTER，没碰 guide_products')

// 5. 改后的文案里不能再有高危词
const HIGH = ['代练', '代打', '代肝', '代做', '包上分', '代刷', '陪练']
let bad = 0
stmts.forEach(function (s) {
  const m = s.match(/REPLACE\((\w+), '([\s\S]*)', '([\s\S]*)'\);$/)
  if (!m) return
  HIGH.forEach(function (w) { if (m[3].indexOf(w) !== -1) { bad++; fail('改后文案仍含「' + w + '」：' + m[3].slice(0, 40)) } })
})
if (!bad) ok('改后文案不含任何高危词')

console.log('\n' + (problems === 0 ? 'SQL 自检通过 ✅' : problems + ' 处问题 ❌'))
process.exit(problems === 0 ? 0 : 1)
