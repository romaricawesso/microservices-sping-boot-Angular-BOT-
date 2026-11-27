import {HttpErrorResponse} from '@angular/common/http';

// Transforme une erreur HTTP du backend (ProblemDetail Spring) en message lisible
export function apiErrorMessage(err: HttpErrorResponse): string {
  if (err.status === 0) {
    return "Serveur injoignable : verifiez que la gateway (8080) est demarree";
  }
  const body = err.error;
  if (body?.errors) {
    return Object.values(body.errors).join(", ");
  }
  return body?.detail || err.statusText || "Erreur inconnue";
}
