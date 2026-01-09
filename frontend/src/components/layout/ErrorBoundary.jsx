import { Component } from 'react'
import { AlertTriangle, RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    console.error('Error caught by boundary:', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center p-4 bg-background">
          <Card className="max-w-md w-full">
            <CardHeader className="text-center">
              <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-danger/10">
                <AlertTriangle className="h-8 w-8 text-danger" />
              </div>
              <CardTitle className="text-xl">Bir Hata Oluştu</CardTitle>
              <CardDescription>
                Beklenmeyen bir hata meydana geldi. Lütfen sayfayı yenileyip tekrar deneyin.
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
              <Button onClick={this.handleReset} className="w-full">
                <RefreshCw className="mr-2 h-4 w-4" />
                Sayfayı Yenile
              </Button>
            </CardContent>
          </Card>
        </div>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary
