export default function Loading({ fullScreen = false, text = 'Yükleniyor...' }) {
  const content = (
    <div className="flex flex-col items-center justify-center gap-4">
      <div className="relative">
        <div className="w-12 h-12 border-4 border-dark-700 rounded-full" />
        <div className="absolute top-0 left-0 w-12 h-12 border-4 border-primary-500 rounded-full border-t-transparent animate-spin" />
      </div>
      {text && <p className="text-dark-400 text-sm">{text}</p>}
    </div>
  )

  if (fullScreen) {
    return (
      <div role="status" className="fixed inset-0 bg-dark-950 flex items-center justify-center z-50">
        {content}
      </div>
    )
  }

  return (
    <div role="status" className="flex items-center justify-center py-12">
      {content}
    </div>
  )
}
