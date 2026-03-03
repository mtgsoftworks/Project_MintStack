import { useState, useEffect, useCallback, useRef } from 'react'

export function useInfiniteScroll({
  fetchMore,
  hasMore,
  threshold = 100,
  enabled = true
}) {
  const [isLoading, setIsLoading] = useState(false)
  const observerRef = useRef(null)
  const loadMoreRef = useRef(null)

  const handleLoadMore = useCallback(async () => {
    if (isLoading || !hasMore || !enabled) return
    
    setIsLoading(true)
    try {
      await fetchMore()
    } finally {
      setIsLoading(false)
    }
  }, [fetchMore, hasMore, isLoading, enabled])

  useEffect(() => {
    const element = loadMoreRef.current
    if (!element || !enabled) return

    observerRef.current = new IntersectionObserver(
      (entries) => {
        const [entry] = entries
        if (entry.isIntersecting && hasMore && !isLoading) {
          handleLoadMore()
        }
      },
      {
        rootMargin: `${threshold}px`,
      }
    )

    observerRef.current.observe(element)

    return () => {
      if (observerRef.current) {
        observerRef.current.disconnect()
      }
    }
  }, [handleLoadMore, hasMore, isLoading, threshold, enabled])

  return {
    loadMoreRef,
    isLoading,
    handleLoadMore
  }
}

// Scroll-based infinite scroll (alternative)
export function useScrollInfiniteScroll({
  fetchMore,
  hasMore,
  threshold = 200,
  enabled = true
}) {
  const [isLoading, setIsLoading] = useState(false)

  const handleScroll = useCallback(async () => {
    if (isLoading || !hasMore || !enabled) return

    const scrollTop = document.documentElement.scrollTop
    const scrollHeight = document.documentElement.scrollHeight
    const clientHeight = document.documentElement.clientHeight

    if (scrollTop + clientHeight >= scrollHeight - threshold) {
      setIsLoading(true)
      try {
        await fetchMore()
      } finally {
        setIsLoading(false)
      }
    }
  }, [fetchMore, hasMore, isLoading, threshold, enabled])

  useEffect(() => {
    if (!enabled) return
    
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [handleScroll, enabled])

  return { isLoading }
}

export default useInfiniteScroll
