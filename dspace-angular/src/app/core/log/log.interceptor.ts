import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';

import { Observable } from 'rxjs';
import { hasValue } from '../../shared/empty.util';
import { CorrelationIdService } from '../../correlation-id/correlation-id.service';

/**
 * Log Interceptor intercepting Http Requests & Responses to
 * exchange add headers of the user using the application utilizing unique id in cookies.
 * Add header for users current page path.
 */
@Injectable()
export class LogInterceptor implements HttpInterceptor {

  constructor(private cidService: CorrelationIdService, private router: Router) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    // Get the correlation id for the user from the store
    const correlationId = this.cidService.getCorrelationId();

    // Add headers from the intercepted request
    let headers = request.headers;
    if (hasValue(correlationId)) {
      headers = headers.append('X-CORRELATION-ID', correlationId);
    }
    headers = headers.append('X-REFERRER', this.router.url);

    // Add new headers to the intercepted request
    request = request.clone({ withCredentials: true, headers: headers });
    return next.handle(request);
  }
}
