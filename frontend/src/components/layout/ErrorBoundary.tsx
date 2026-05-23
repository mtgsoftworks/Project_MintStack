import { Component } from 'react'
import { withTranslation } from 'react-i18next'
import { AlertTriangle } from 'lucide-react'
import RefreshButton from '@/components/common/RefreshButton'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export class ErrorBoundary extends Component<any, any> {
  constructor(props: any) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: any) {
    return { hasError: true, error }
  }

  componentDidCatch(error: any, errorInfo: any) {
    console.error('Error caught by boundary:', error, errorInfo)
  }

  handleReset = async () => {
    this.setState({ hasError: false, error: null })
    await new Promise((resolve) => {
      setTimeout(resolve, 250)
    })
    window.location.reload()
  }

  render() {
    const translate = typeof this.props.t === 'function'
      ? this.props.t
      : (key: string) => key

    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center p-4 bg-background">
          <Card className="max-w-md w-full">
            <CardHeader className="text-center">
              <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-danger/10">
                <AlertTriangle className="h-8 w-8 text-danger" />
              </div>
              <CardTitle className="text-xl">{translate('errors.boundaryTitle')}</CardTitle>
              <CardDescription>
                {translate('errors.boundaryDescription')}
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              {import.meta.env.DEV && this.state.error && (
                <div className="rounded-lg bg-muted p-4 text-sm">
                  <p className="font-mono text-xs text-muted-foreground break-all">
                    {this.state.error.toString()}
                  </p>
                </div>
              )}
              <RefreshButton onRefresh={this.handleReset} className="w-full">
                {translate('common.refreshPage')}
              </RefreshButton>
            </CardContent>
          </Card>
        </div>
      )
    }

    return this.props.children
  }
}

const TranslatedErrorBoundary = withTranslation()(ErrorBoundary)

export default TranslatedErrorBoundary
