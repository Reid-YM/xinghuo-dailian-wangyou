/**
 * 小程序静态校验：不需要开发者工具，检查最容易出错的三件事。
 *
 *   1. app.json 里登记的页面，四个文件是否齐全
 *   2. WXML 里 bind / catch 绑定的处理函数，在 JS 里是否真的存在
 *   3. WXML 里 {{}} 用到的变量，在页面 data / wx:for 作用域里有没有定义
 *
 * 用法：node tools/validate.js
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.join(__dirname, '..')
const projectConfig = JSON.parse(fs.readFileSync(path.join(ROOT, 'project.config.json'), 'utf8'))
const MP = path.join(ROOT, projectConfig.miniprogramRoot || 'miniprogram')
const appJson = JSON.parse(fs.readFileSync(path.join(MP, 'app.json'), 'utf8'))

let problems = 0
function fail(msg) {
  problems++
  console.log('  ✗ ' + msg)
}
function ok(msg) {
  console.log('  ✓ ' + msg)
}

/* --------------------------------------------------- 1. 页面文件齐全 */

console.log('\n[1] 页面文件完整性')
appJson.pages.forEach(function (p) {
  ;['js', 'json', 'wxml'].forEach(function (ext) {
    const file = path.join(MP, p + '.' + ext)
    if (!fs.existsSync(file)) fail('缺少 ' + p + '.' + ext)
  })
  const wxss = path.join(MP, p + '.wxss')
  if (!fs.existsSync(wxss)) console.log('  · ' + p + '.wxss 不存在（可选，用 app.wxss 的样式）')
})
if (problems === 0) ok(appJson.pages.length + ' 个页面的 js/json/wxml 都在')

/* 所有 json 语法 */
console.log('\n[2] JSON 语法')
function walk(dir, out) {
  fs.readdirSync(dir, { withFileTypes: true }).forEach(function (d) {
    const full = path.join(dir, d.name)
    if (d.isDirectory()) walk(full, out)
    else if (d.name.endsWith('.json')) out.push(full)
  })
  return out
}
const jsonFiles = walk(ROOT, []).filter(function (f) { return f.indexOf('node_modules') === -1 })
jsonFiles.forEach(function (f) {
  try {
    JSON.parse(fs.readFileSync(f, 'utf8'))
    ok(path.relative(ROOT, f))
  } catch (e) {
    fail(path.relative(ROOT, f) + ' 解析失败：' + e.message)
  }
})

/* --------------------------------------------------- 3. 事件绑定 */

console.log('\n[3] 事件处理函数绑定')
appJson.pages.forEach(function (p) {
  const wxml = fs.readFileSync(path.join(MP, p + '.wxml'), 'utf8')
  const js = fs.readFileSync(path.join(MP, p + '.js'), 'utf8')

  const handlers = new Set()
  const re = /\b(?:bind|catch|capture-bind|capture-catch)[:]?([a-zA-Z]+)\s*=\s*"([^"{}]+)"/g
  let m
  while ((m = re.exec(wxml))) handlers.add(m[2].trim())

  let bad = 0
  handlers.forEach(function (h) {
    if (!new RegExp('(^|[^\\w.])' + h + '\\s*\\(').test(js)) {
      fail(p + '.wxml 绑定了 ' + h + '，但 ' + p + '.js 里找不到这个函数')
      bad++
    }
  })
  if (!bad) ok(p + '：' + handlers.size + ' 个绑定全部有对应函数')
})

/* --------------------------------------------------- 4. 模板变量 */

console.log('\n[4] 模板变量')
appJson.pages.forEach(function (p) {
  const wxml = fs.readFileSync(path.join(MP, p + '.wxml'), 'utf8')
  const js = fs.readFileSync(path.join(MP, p + '.js'), 'utf8')

  // wx:for-item 声明的局部名，以及 WXML 内置名
  const locals = new Set(['item', 'index', 'true', 'false', 'null', 'undefined'])
  const itemRe = /wx:for-item\s*=\s*"([^"]+)"/g
  let im
  while ((im = itemRe.exec(wxml))) locals.add(im[1])

  // 取出所有 {{ }} 里的表达式
  const exprs = []
  const tplRe = /\{\{([\s\S]*?)\}\}/g
  let tm
  while ((tm = tplRe.exec(wxml))) exprs.push(tm[1])

  const idents = new Set()
  exprs.forEach(function (e) {
    const stripped = e.replace(/'[^']*'/g, '').replace(/"[^"]*"/g, '')
    const idRe = /[A-Za-z_$][A-Za-z0-9_$]*/g
    let idm
    while ((idm = idRe.exec(stripped))) {
      const id = idm[0]
      // 排除对象属性访问和对象字面量的键
      const before = stripped.slice(0, idm.index).trimEnd()
      if (before.endsWith('.')) continue
      if (/^[a-z]+$/.test(id) && /[:.]$/.test(before)) continue
      idents.add(id)
    }
  })

  const unknown = []
  idents.forEach(function (id) {
    if (locals.has(id)) return
    if (/^(item|index)$/.test(id)) return
    if (new RegExp('(^|[^\\w.$])' + id + '\\s*:').test(js)) return
    if (new RegExp('(^|[^\\w.$])' + id + '\\b').test(js)) return
    unknown.push(id)
  })
  if (unknown.length) fail(p + '.wxml 里这些变量在 JS 里没定义：' + unknown.join(', '))
  else ok(p + '：' + idents.size + ' 个模板变量全部有定义')
})

/* --------------------------------------------------- 5. WXML 标签闭合 */

console.log('\n[5] WXML 标签闭合')
appJson.pages.forEach(function (p) {
  const wxml = fs.readFileSync(path.join(MP, p + '.wxml'), 'utf8')
  const stack = []
  const tagRe = /<(\/?)([a-zA-Z][\w-]*)([^>]*?)(\/?)>/g
  let tm
  let bad = null
  while ((tm = tagRe.exec(wxml))) {
    const closing = tm[1] === '/'
    const name = tm[2]
    const selfClosed = tm[4] === '/'
    if (selfClosed) continue
    if (closing) {
      const top = stack.pop()
      if (top !== name) {
        bad = '闭合标签 </' + name + '> 与 <' + (top || '空') + '> 不匹配'
        break
      }
    } else {
      stack.push(name)
    }
  }
  if (bad) fail(p + '.wxml ' + bad)
  else if (stack.length) fail(p + '.wxml 有未闭合标签：' + stack.join(', '))
  else ok(p + '.wxml 标签闭合正常')
})

console.log('\n' + (problems === 0 ? '静态校验全部通过 ✅' : problems + ' 项问题 ❌'))
process.exit(problems === 0 ? 0 : 1)
