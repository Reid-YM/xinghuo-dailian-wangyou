/**
 * 极简 Markdown 解析器：把攻略正文的 bodyMarkdown 拆成页面能直接渲染的块。
 *
 * 为什么不用 rich-text 直接塞 HTML：
 *   1. 暗色主题下的排版要能控制，rich-text 的默认样式改不动；
 *   2. 攻略正文里可能有指向站外的链接，转 HTML 等于把外链放进小程序（审核风险）。
 *      这里只保留链接文字、把 URL 丢掉，从根上杜绝。
 *
 * 支持：标题 / 段落 / 无序列表 / 有序列表 / 引用 / 分隔线 / 粗体 / 行内代码。
 * 不认识的行按普通段落处理，不会丢内容。
 */

function parseInline(text) {
  const src = String(text || '')
  const runs = []
  const re = /\*\*([^*]+)\*\*|`([^`]+)`|\[([^\]]+)\]\([^)]*\)/g
  let last = 0
  let m
  while ((m = re.exec(src))) {
    if (m.index > last) runs.push({ text: src.slice(last, m.index) })
    if (m[1] !== undefined) runs.push({ text: m[1], bold: true })
    else if (m[2] !== undefined) runs.push({ text: m[2], code: true })
    else if (m[3] !== undefined) runs.push({ text: m[3] })
    last = re.lastIndex
  }
  if (last < src.length) runs.push({ text: src.slice(last) })
  return runs.filter(function (r) { return r.text !== '' })
}

function parse(md) {
  const text = String(md || '')
    .replace(/\r\n?/g, '\n')
    // 行内图片直接去掉：正文图走 section.imageUrl，避免不可控的图片域名
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')

  const blocks = []
  let list = null

  function flushList() {
    if (list) {
      blocks.push(list)
      list = null
    }
  }

  text.split('\n').forEach(function (raw) {
    const line = raw.replace(/\s+$/, '')

    if (!line.trim()) {
      flushList()
      return
    }

    const heading = line.match(/^(#{1,6})\s+(.*)$/)
    if (heading) {
      flushList()
      blocks.push({ type: 'heading', level: heading[1].length, runs: parseInline(heading[2]) })
      return
    }

    if (/^(-{3,}|\*{3,}|_{3,})$/.test(line.trim())) {
      flushList()
      blocks.push({ type: 'divider' })
      return
    }

    const quote = line.match(/^>\s?(.*)$/)
    if (quote) {
      flushList()
      blocks.push({ type: 'quote', runs: parseInline(quote[1]) })
      return
    }

    const ul = line.match(/^\s*[-*+]\s+(.*)$/)
    if (ul) {
      if (!list || list.ordered) {
        flushList()
        list = { type: 'list', ordered: false, items: [] }
      }
      list.items.push(parseInline(ul[1]))
      return
    }

    const ol = line.match(/^\s*\d+[.)]\s+(.*)$/)
    if (ol) {
      if (!list || !list.ordered) {
        flushList()
        list = { type: 'list', ordered: true, items: [] }
      }
      list.items.push(parseInline(ol[1]))
      return
    }

    flushList()
    const prev = blocks[blocks.length - 1]
    if (prev && prev.type === 'paragraph') {
      prev.runs = prev.runs.concat([{ text: ' ' }], parseInline(line))
    } else {
      blocks.push({ type: 'paragraph', runs: parseInline(line) })
    }
  })

  flushList()
  return blocks
}

module.exports = {
  parse,
  parseInline
}
