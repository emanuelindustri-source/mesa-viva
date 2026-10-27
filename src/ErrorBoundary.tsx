import { Component, useEffect } from 'react';
import type { ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, Bug, RefreshCw } from 'lucide-react';
import { diag } from './services/diagnostics';
import { Button } from './ui/components';
import { navigate } from './app/router';

interface Props { children: ReactNode }
interface State { hasError: boolean; message: string }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, message: '' };

  static getDerivedStateFromError(error: unknown): State {
    return { hasError: true, message: error instanceof Error ? error.message : String(error) };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    diag('error', `Error de interfaz: ${error.message}`, `${error.stack ?? ''}\n${info.componentStack ?? ''}`, 'error-boundary');
  }

  render() {
    if (!this.state.hasError) return this.props.children;
    return (
      <div className="fatal-screen">
        <div className="fatal-card">
          <span className="fatal-icon"><AlertTriangle size={30} /></span>
          <h1>La mesa se detuvo</h1>
          <p>{this.state.message}</p>
          <div className="fatal-actions">
            <Button variant="primary" icon={<RefreshCw size={17} />} onClick={() => window.location.reload()}>Recargar la mesa</Button>
            <Button variant="secondary" icon={<Bug size={17} />} onClick={() => { navigate('/diagnostics'); this.setState({ hasError: false, message: '' }); }}>Abrir diagnóstico</Button>
          </div>
          <small>Mesa Viva guarda el detalle del error para que puedas enviarlo.</small>
        </div>
      </div>
    );
  }
}

export function useScrollTop(dependency: unknown): void {
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [dependency]);
}
