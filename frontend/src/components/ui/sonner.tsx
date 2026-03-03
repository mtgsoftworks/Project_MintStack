import { Toaster as Sonner } from "sonner"

const Toaster = ({ ...props }) => {
  return (
    <Sonner
      className="toaster group"
      toastOptions={{
        classNames: {
          toast:
            "group toast group-[.toaster]:bg-background group-[.toaster]:text-foreground group-[.toaster]:border-border group-[.toaster]:shadow-lg",
          description: "group-[.toast]:text-muted-foreground",
          actionButton:
            "group-[.toast]:bg-primary group-[.toast]:text-primary-foreground",
          cancelButton:
            "group-[.toast]:bg-muted group-[.toast]:text-muted-foreground",
          success: "group-[.toaster]:bg-success-light group-[.toaster]:text-success group-[.toaster]:border-success/20",
          error: "group-[.toaster]:bg-danger-light group-[.toaster]:text-danger group-[.toaster]:border-danger/20",
          warning: "group-[.toaster]:bg-warning-light group-[.toaster]:text-warning-dark group-[.toaster]:border-warning/20",
          info: "group-[.toaster]:bg-info-light group-[.toaster]:text-info group-[.toaster]:border-info/20",
        },
      }}
      {...props}
    />
  )
}

export { Toaster }
