import { describe, expect, it } from 'vitest'
import tr from '../tr.json'
import en from '../en.json'

function flattenKeys(value: unknown, prefix = ''): string[] {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return prefix ? [prefix] : []
  }

  return Object.entries(value).flatMap(([key, child]) => {
    const nextPrefix = prefix ? `${prefix}.${key}` : key
    return flattenKeys(child, nextPrefix)
  })
}

describe('locale keys', () => {
  it('keeps Turkish and English translation keys in sync', () => {
    const trKeys = new Set(flattenKeys(tr))
    const enKeys = new Set(flattenKeys(en))

    const missingInTr = [...enKeys].filter((key) => !trKeys.has(key)).sort()
    const missingInEn = [...trKeys].filter((key) => !enKeys.has(key)).sort()

    expect(missingInTr).toEqual([])
    expect(missingInEn).toEqual([])
  })
})
