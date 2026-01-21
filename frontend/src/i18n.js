import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

import tr from './locales/tr.json'
import en from './locales/en.json'

const resources = {
    tr: { translation: tr },
    en: { translation: en },
}

i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
        resources,
        lng: localStorage.getItem('mintstack_language') || 'tr', // Varsayılan Türkçe
        fallbackLng: 'tr',
        debug: import.meta.env.DEV,

        interpolation: {
            escapeValue: false, // React already escapes values
        },

        detection: {
            order: ['localStorage'], // Sadece localStorage'dan oku, tarayıcı dilini takip etme
            lookupLocalStorage: 'mintstack_language',
            caches: ['localStorage'],
        },
    })

export default i18n
