import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'

export function MarketStatus({ lastUpdate }: any) {
    const { t, i18n } = useTranslation()
    const [marketOpen, setMarketOpen] = useState(false)
    const [timeToClose, setTimeToClose] = useState('')

    useEffect(() => {
        const checkMarket = () => {
            const now = new Date()
            const hours = now.getHours()
            const day = now.getDay()

            const isBistOpen = day > 0 && day < 6 && hours >= 10 && hours < 18
            setMarketOpen(isBistOpen)

            if (isBistOpen) {
                const closeTime = new Date(now)
                closeTime.setHours(18, 0, 0, 0)
                const diff = closeTime.getTime() - now.getTime()
                const hoursLeft = Math.floor(diff / 3600000)
                const minsLeft = Math.floor((diff % 3600000) / 60000)
                setTimeToClose(t('marketStatus.timeToClose', { hours: hoursLeft, minutes: minsLeft }))
            }
        }

        checkMarket()
        const interval = setInterval(checkMarket, 60000)
        return () => clearInterval(interval)
    }, [t])

    const locale = i18n.language === 'en' ? 'en-US' : 'tr-TR'

    return (
        <div className="flex items-center gap-4 text-sm">
            <div className="flex items-center gap-2">
                <div className={`w-2 h-2 rounded-full ${marketOpen ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
                <span className="text-gray-600">{t('marketStatus.bist')}:</span>
                <span className={marketOpen ? 'text-green-600 font-medium' : 'text-red-600'}>
                    {marketOpen
                        ? `${t('marketStatus.open')} (${t('marketStatus.closesIn', { time: timeToClose })})`
                        : t('marketStatus.closed')}
                </span>
            </div>
            {lastUpdate && (
                <div className="text-gray-500">
                    {t('marketStatus.lastUpdate')}: {new Date(lastUpdate).toLocaleTimeString(locale)}
                </div>
            )}
        </div>
    )
}
