export const getApiErrorMessage = (error, fallbackMessage) => {
    const data = error?.data

    if (typeof data === 'string' && data.trim()) {
        return data
    }

    return (
        data?.message ||
        data?.error?.message ||
        data?.error ||
        data?.errors?.[0]?.message ||
        error?.message ||
        error?.error ||
        fallbackMessage
    )
}
