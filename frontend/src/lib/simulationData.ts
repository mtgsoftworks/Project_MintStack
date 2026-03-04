const SIMULATION_SOURCE_TOKENS = ['manual', 'simulation', 'simulated', 'simulasyon', 'mock']

function normalizeString(value) {
  return typeof value === 'string' ? value.toLowerCase() : ''
}

function hasSimulationSourceToken(value) {
  const normalized = normalizeString(value)
  return SIMULATION_SOURCE_TOKENS.some((token) => normalized.includes(token))
}

export function isSimulatedMarketData(item) {
  if (!item) {
    return false
  }

  if (item.isSimulated === true) {
    return true
  }

  if (hasSimulationSourceToken(item.source)) {
    return true
  }

  if (hasSimulationSourceToken(item.dataSource)) {
    return true
  }

  return false
}
