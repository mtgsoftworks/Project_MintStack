export function getApiErrorMessage(error, fallbackMessage = 'Islem basarisiz oldu') {
  const data = error?.data

  if (typeof data === 'string' && data.trim()) {
    return data
  }

  const candidates = [
    data?.message,
    data?.details,
    data?.error,
    data?.error?.message,
    data?.error?.details,
    data?.error?.error,
    data?.errors?.[0]?.message,
    error?.message,
    error?.error,
  ]

  for (const candidate of candidates) {
    if (typeof candidate === 'string' && candidate.trim()) {
      return candidate
    }
  }

  return fallbackMessage
}
