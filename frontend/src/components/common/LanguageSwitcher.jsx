import { useTranslation } from 'react-i18next'
import { Globe, Check } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

const languages = [
    { code: 'tr', name: 'Türkçe', shortCode: 'TR' },
    { code: 'en', name: 'English', shortCode: 'EN' },
]

/**
 * Language switcher component
 * Allows users to change the application language
 */
export function LanguageSwitcher({ variant = 'dropdown' }) {
    const { i18n } = useTranslation()

    const currentLanguage = languages.find(lang => lang.code === i18n.language) || languages[0]

    const changeLanguage = (langCode) => {
        i18n.changeLanguage(langCode)
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
                                : 'bg-muted text-muted-foreground hover:bg-muted/80'
                            }`}
                    >
                        {lang.shortCode} - {lang.name}
                    </button>
                ))}
            </div>
        )
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="sm" className="gap-2">
                    <Globe className="h-4 w-4" />
                    <span className="hidden sm:inline">{currentLanguage.name}</span>
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                {languages.map(lang => (
                    <DropdownMenuItem
                        key={lang.code}
                        onClick={() => changeLanguage(lang.code)}
                        className="gap-3 cursor-pointer"
                    >
                        <span className="text-xs font-semibold text-muted-foreground w-6">{lang.shortCode}</span>
                        <span>{lang.name}</span>
                        {i18n.language === lang.code && (
                            <Check className="h-4 w-4 ml-auto text-primary" />
                        )}
                    </DropdownMenuItem>
                ))}
            </DropdownMenuContent>
        </DropdownMenu>
    )
}

export default LanguageSwitcher
