import { cn } from '@/lib/utils'

export function HamburgerIcon({ isOpen, className, onClick }) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "relative w-6 h-5 flex flex-col justify-between items-center group",
        className
      )}
      aria-label={isOpen ? "Close menu" : "Open menu"}
    >
      <span
        className={cn(
          "w-full h-0.5 bg-current rounded-full transition-all duration-300 ease-in-out origin-center",
          isOpen && "rotate-45 translate-y-2"
        )}
      />
      <span
        className={cn(
          "w-full h-0.5 bg-current rounded-full transition-all duration-300 ease-in-out",
          isOpen && "opacity-0 scale-0"
        )}
      />
      <span
        className={cn(
          "w-full h-0.5 bg-current rounded-full transition-all duration-300 ease-in-out origin-center",
          isOpen && "-rotate-45 -translate-y-2"
        )}
      />
    </button>
  )
}

export default HamburgerIcon
