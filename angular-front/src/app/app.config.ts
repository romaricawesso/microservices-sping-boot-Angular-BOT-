import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { loadingInterceptorInterceptor } from './interceptors/loading-interceptor-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // l'intercepteur affiche le spinner pendant chaque appel HTTP
    provideHttpClient(withInterceptors([loadingInterceptorInterceptor])),
  ],
};
