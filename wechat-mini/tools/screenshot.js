/**
 * 用开发者工具的自动化接口给各页面截图 + 抓运行时异常（改完 UI 自查用）。
 *
 * 为什么需要：WXML/WXSS 的渲染结果光看代码看不出来，这个脚本能直接出图，
 * 还能顺便验证「页面到底渲染出了什么」「有没有运行时异常」。
 *
 * 前置步骤（一次性）：
 *   1. 开发者工具 → 设置 → 安全设置 → 打开「服务端口」
 *   2. 装自动化库（放仓库外，别污染仓库）：
 *        mkdir <某处>\mp-automator && cd <某处>\mp-automator
 *        npm init -y && npm i miniprogram-automator
 *   3. 让工具进入自动化模式：
 *        "D:\微信web开发者工具\cli.bat" auto --project "<本仓库路径>" --auto-port 9420
 *
 * 用法：
 *   node tools/screenshot.js
 *   node tools/screenshot.js <automator 模块所在目录> [输出目录]
 *   也可用环境变量：AUTOMATOR_DIR / SHOT_OUT / AUTO_PORT
 */
const path = require('path')
const fs = require('fs')

const ROOT = path.join(__dirname, '..')
const AUTOMATOR_DIR = process.argv[2] || process.env.AUTOMATOR_DIR || 'C:\\Users\\1\\Documents\\ChatGPT\\mp-automator'
const OUT = process.argv[3] || process.env.SHOT_OUT || path.join(ROOT, 'preview')
const PORT = process.env.AUTO_PORT || '9420'

let automator
try {
  automator = require(path.join(AUTOMATOR_DIR, 'node_modules', 'miniprogram-automator'))
} catch (e) {
  try {
    automator = require('miniprogram-automator')
  } catch (e2) {
    console.error('找不到 miniprogram-automator。先按文件顶部注释装好，或用参数指定目录：')
    console.error('  node tools/screenshot.js <automator 模块所在目录>')
    process.exit(1)
  }
}

const exceptions = []

;(async function () {
  fs.mkdirSync(OUT, { recursive: true })
  console.log('连接 ws://127.0.0.1:' + PORT + ' …')
  const mp = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:' + PORT })
  try {
    mp.on('exception', function (e) { exceptions.push(JSON.stringify(e).slice(0, 300)) })
  } catch (e) {}

  async function shot(page, name) {
    await mp.screenshot({ path: path.join(OUT, name) })
    console.log('  ✓ ' + name)
  }

  console.log('[1] 首页')
  let page = await mp.reLaunch('/pages/index/index')
  await page.waitFor(3500)
  const idx = await page.data()
  console.log('  数据源=' + idx.sourceText + ' | 模式=' + idx.activeModeName + ' | 进度=' + idx.stats.count + '/' + idx.stats.total)
  console.log('  三档迷彩图=' + idx.tierCounts.map(function (t) { return t.image }).join(', '))
  await shot(page, 'shot-1-index.png')

  console.log('[2] 攻略')
  page = await mp.switchTab('/pages/guides/guides')
  await page.waitFor(3000)
  const g = await page.data()
  console.log('  ' + (g.shown ? g.shown.length : 0) + ' 篇 | error="' + g.error + '"')
  await shot(page, 'shot-2-guides.png')

  if (g.shown && g.shown.length) {
    console.log('[3] 攻略详情')
    page = await mp.navigateTo('/pages/guides/detail?slug=' + g.shown[0].slug)
    await page.waitFor(3500)
    const d = await page.data()
    console.log('  ' + (d.guide ? d.guide.title : '(打不开)'))
    await shot(page, 'shot-3-guide-detail.png')
  }

  console.log('[4] 我的')
  page = await mp.switchTab('/pages/me/me')
  await page.waitFor(2500)
  await shot(page, 'shot-4-me.png')

  console.log('[5] 查单（真输入 + 真点击）')
  page = await mp.switchTab('/pages/record/record')
  await page.waitFor(2000)
  await page.setData({ no: 'XH00000000000' })
  const btn = await page.$('.btn.primary')
  if (btn) await btn.tap()
  await page.waitFor(2500)
  const r = await page.data()
  console.log('  错误提示=' + r.error)
  await shot(page, 'shot-5-record.png')

  console.log('\n运行时异常：' + (exceptions.length ? '\n  ' + exceptions.join('\n  ') : '无'))
  console.log('截图输出目录：' + OUT)
  process.exit(0)
})().catch(function (e) {
  console.error('失败：' + (e && e.message))
  console.error('提示：确认已执行 cli auto 启动自动化模式（端口 ' + PORT + '）')
  process.exit(1)
})
