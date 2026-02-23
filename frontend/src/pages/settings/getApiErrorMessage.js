export const getApiErrorMessage = (error, fallbackMessage) => {
    console.error('[DETAILED_API_ERROR]', error);
    
    const data = error?.data

    let msg = '';
    if (typeof data === 'string') {
        msg = data
    } else {
        msg = (
            data?.message ||
            data?.error?.message ||
            data?.error ||
            data?.errors?.[0]?.message ||
            error?.message ||
            error?.error ||
            fallbackMessage
        );
    }
    
    try {
        const details = JSON.stringify(error);
        return `${msg} | Details: ${details}`;
    } catch (e) {
        return `${msg} | Details: [Unstringifiable Error]`;
    }
}
