import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';

import { Observable } from 'rxjs';

import { LocaleService } from './locale.service';
import { mergeMap, scan } from 'rxjs/operators';

@Injectable()
export class LocaleInterceptor implements HttpInterceptor {

  constructor(private localeService: LocaleService) {
  }

  /**
   * Intercept method
   * @param req
   * @param next
   */
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    let newReq: HttpRequest<any>;
    return this.localeService.getLanguageCodeList()
      .pipe(
        scan((acc: any, value: any) => [...acc, value], []),
        mergeMap((languages) => {
          // Clone the request to add the new header.
          newReq = req.clone({
            headers: req.headers
              .set('Accept-Language', languages.toString())
          });
          // Pass on the new request instead of the original request.
          return next.handle(newReq);
        }));
  }
}
