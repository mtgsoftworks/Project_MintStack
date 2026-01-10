import { useTranslation } from 'react-i18next'
import { useState } from 'react'

const languages = [
    { code: 'tr', name: 'Türkçe', flag: '🇹🇷' },
    { code: 'en', name: 'English', flag: '🇬🇧' },
]

/**
 * Language switcher component
 * Allows users to change the application language
 */
export function LanguageSwitcher({ variant = 'dropdown' }) {
    const { i18n } = useTranslation()
    const [isOpen, setIsOpen] = useState(false)

    const currentLanguage = languages.find(lang => lang.code === i18n.language) || languages[0]

    const changeLanguage = (langCode) => {
        i18n.changeLanguage(langCode)
        setIsOpen(false)
    }

    if (variant === 'buttons') {
        return (
            <div className="flex gap-2">
                {languages.map(lang => (
                    <button
                        key={lang.code}
                        onClick={() => changeLanguage(lang.code)}
                        className={`px-3 py-1 rounded-md text-sm font-medium transition-colors ${i18n.language === lang.code
                                ? 'bg-emerald-500 text-white'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                            }`}
                    >
                        {lang.flag} {lang.name}
                    </button>
                ))}
            </div>
        )
    }

    return (
        <div className="relative">
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="flex items-center gap-2 px-3 py-2 rounded-lg bg-gray-100 hover:bg-gray-200 transition-colors"
            >
                <span className="text-lg">{currentLanguage.flag}</span>
                <span className="text-sm font-medium">{currentLanguage.name}</span>
                <svg
                    className={`w-4 h-4 transition-transform ${isOpen ? 'rotate-180' : ''}`}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
            </button>

            {isOpen && (
                <div className="absolute right-0 mt-2 w-40 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
                    {languages.map(lang => (
                        <button
                            key={lang.code}
                            onClick={() => changeLanguage(lang.code)}
                            className={`w-full flex items-center gap-3 px-4 py-2 text-sm hover:bg-gray-50 ${i18n.language === lang.code ? 'bg-emerald-50 text-emerald-600' : 'text-gray-700'
                                }`}
                        >
                            <span className="text-lg">{lang.flag}</span>
                            <span>{lang.name}</span>
                            {i18n.language === lang.code && (
                                <svg className="w-4 h-4 ml-auto" fill="currentColor" viewBox="0 0 20 20">
                                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                                </svg>
                            )}
                        </button>
                    ))}
                </div>
            )}
        </div>
    )
}

export default LanguageSwitcher
