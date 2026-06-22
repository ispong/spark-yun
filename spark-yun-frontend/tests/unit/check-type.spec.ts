import { describe, expect, it } from 'vitest'

import { getUrlParam, isArray, isObject, isUrl, setObjToUrlParams } from '@/app/utils/checkType'

describe('checkType utils', () => {
    it('checks common runtime types', () => {
        expect(isArray([1, 2])).toBe(true)
        expect(isObject({ name: 'spark-yun' })).toBe(true)
        expect(isObject(null)).toBe(false)
    })

    it('handles url helpers', () => {
        expect(isUrl('https://example.com')).toBe(true)
        expect(getUrlParam('https://example.com?a=1&b=2', 'b')).toBe('2')
        expect(setObjToUrlParams('/api/demo', { keyword: '至轻云' })).toBe(
            '/api/demo?keyword=%E8%87%B3%E8%BD%BB%E4%BA%91'
        )
    })
})
