const asArray = (value) => {
    if (Array.isArray(value)) {
        return value
    }

    if (!value) {
        return []
    }

    return [value]
}

export const getProviderInfo = (t) => ({
    YAHOO_FINANCE: {
        title: t('settings.providers.info.YAHOO_FINANCE.title'),
        description: t('settings.providers.info.YAHOO_FINANCE.desc'),
        supported: asArray(t('settings.providers.info.YAHOO_FINANCE.supported', { returnObjects: true })),
        missing: asArray(t('settings.providers.info.YAHOO_FINANCE.missing', { returnObjects: true })),
        color: 'bg-blue-50 text-blue-900 border-blue-200'
    },
    ALPHA_VANTAGE: {
        title: t('settings.providers.info.ALPHA_VANTAGE.title'),
        description: t('settings.providers.info.ALPHA_VANTAGE.desc'),
        supported: asArray(t('settings.providers.info.ALPHA_VANTAGE.supported', { returnObjects: true })),
        missing: asArray(t('settings.providers.info.ALPHA_VANTAGE.missing', { returnObjects: true })),
        color: 'bg-yellow-50 text-yellow-900 border-yellow-200'
    },
    FINNHUB: {
        title: t('settings.providers.info.FINNHUB.title'),
        description: t('settings.providers.info.FINNHUB.desc'),
        supported: asArray(t('settings.providers.info.FINNHUB.supported', { returnObjects: true })),
        missing: asArray(t('settings.providers.info.FINNHUB.missing', { returnObjects: true })),
        color: 'bg-orange-50 text-orange-900 border-orange-200'
    },
    TCMB: {
        title: t('settings.providers.info.TCMB.title'),
        description: t('settings.providers.info.TCMB.desc'),
        supported: asArray(t('settings.providers.info.TCMB.supported', { returnObjects: true })),
        missing: asArray(t('settings.providers.info.TCMB.missing', { returnObjects: true })),
        color: 'bg-slate-50 text-slate-900 border-slate-200'
    }
})

export const DATA_SOURCE_TYPES = [
    {
        type: 'CURRENCY_RATES',
        label: 'Doviz Kurlari',
        providers: ['TCMB', 'YAHOO_FINANCE', 'ALPHA_VANTAGE', 'FINNHUB']
    },
    {
        type: 'BIST_STOCKS',
        label: 'BIST Hisseleri',
        providers: ['YAHOO_FINANCE']
    },
    {
        type: 'US_STOCKS',
        label: 'ABD Hisseleri',
        providers: ['YAHOO_FINANCE', 'ALPHA_VANTAGE', 'FINNHUB']
    },
    {
        type: 'CRYPTO',
        label: 'Kripto Paralar',
        providers: ['YAHOO_FINANCE', 'ALPHA_VANTAGE', 'FINNHUB']
    },
    {
        type: 'NEWS',
        label: 'Haberler',
        providers: ['YAHOO_FINANCE', 'ALPHA_VANTAGE', 'FINNHUB']
    }
]
