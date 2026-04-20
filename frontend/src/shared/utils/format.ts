import dayjs from 'dayjs'

export function formatDate(val?: string | number) {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm:ss')
}

export function formatDuration(minutes?: number) {
  if (minutes == null) return '-'
  if (minutes < 60) return `${Math.round(minutes)} 分钟`
  return `${(minutes / 60).toFixed(1)} 小时`
}
