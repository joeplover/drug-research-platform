export function formatDateTime(value?: string | number | Date | number[] | null) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const normalized = normalizeDateInput(value)
  if (!normalized) {
    return String(value)
  }
  return normalized.toLocaleString('zh-CN', { hour12: false })
}

export function formatDate(value?: string | number | Date | number[] | null) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const normalized = normalizeDateInput(value)
  if (!normalized) {
    return String(value)
  }
  return normalized.toLocaleDateString('zh-CN')
}

export function formatPercent(value?: number | null) {
  if (value === null || value === undefined) {
    return '-'
  }
  return `${(value * 100).toFixed(1)}%`
}

export function formatNumber(value?: number | null) {
  if (value === null || value === undefined) {
    return '-'
  }
  return `${value}`
}

function normalizeDateInput(value: string | number | Date | number[]) {
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }

  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    if (
      [year, month, day, hour, minute, second].every((item) => typeof item === 'number' && Number.isFinite(item))
    ) {
      return new Date(year, month - 1, day, hour, minute, second)
    }
    return null
  }

  const direct = new Date(value)
  if (!Number.isNaN(direct.getTime())) {
    return direct
  }

  if (typeof value !== 'string') {
    return null
  }

  const normalizedValue = value.trim().replace(' ', 'T')
  const localDateTimeMatch = normalizedValue.match(
    /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/
  )

  if (localDateTimeMatch) {
    const [, year, month, day, hour, minute, second = '00'] = localDateTimeMatch
    return new Date(
      Number(year),
      Number(month) - 1,
      Number(day),
      Number(hour),
      Number(minute),
      Number(second)
    )
  }

  return null
}
