import { HttpInterceptorFn } from '@angular/common/http';
import {inject} from '@angular/core';
import {Loadingservice} from '../servives/loading';
import {finalize} from 'rxjs';

export const loadingInterceptorInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(Loadingservice);
  loadingService.setLoading(true);
  return next(req).pipe(finalize(() =>{
    loadingService.setLoading(false);
  }));
};
