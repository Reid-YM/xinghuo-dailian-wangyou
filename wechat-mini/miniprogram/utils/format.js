function timeAgo(ts) {
  if (!ts) return '还没同步过'
  const diff = Date.now() - ts
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 2592000000) return Math.floor(diff / 86400000) + ' 天前'
  return '很久以前'
}

function syncStatus(state) {
  if (state.syncing) return '同步中…'
  if (state.hint && state.hint.indexOf('同步失败') === 0) return '同步失败'
  return state.lastSyncAt ? '上次同步 ' + timeAgo(state.lastSyncAt) : '尚未同步'
}

// openid 不适合完整展示，只显示尾号够用
function shortId(openid) {
  if (!openid) return ''
  const s = String(openid)
  return s.length > 6 ? '…' + s.slice(-6) : s
}

/** ISO 时间串 -> 2026-10-12 15:30 */
function dateTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (isNaN(d.getTime())) return ''
  function p(n) {
    return n < 10 ? '0' + n : '' + n
  }
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) +
    ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
}

module.exports = {
  timeAgo,
  syncStatus,
  dateTime,
  shortId
}
