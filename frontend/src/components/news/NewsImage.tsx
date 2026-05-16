import { useEffect, useMemo, useState } from 'react'
import { getNewsImageFallback } from '@/lib/news'

type NewsImageProps = {
  imageUrl?: string | null
  title?: string | null
  categorySlug?: string | null
}

export default function NewsImage({ imageUrl, title, categorySlug }: NewsImageProps) {
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    setFailed(false)
  }, [imageUrl])

  const fallback = useMemo(() => getNewsImageFallback({ categorySlug }), [categorySlug])
  const src = !failed && imageUrl ? imageUrl : fallback

  return (
    <div className="aspect-video overflow-hidden rounded-t-xl bg-muted">
      <img
        src={src}
        alt={title || 'Haber gorseli'}
        loading="lazy"
        onError={() => setFailed(true)}
        className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
      />
    </div>
  )
}
