import { CookieService } from '../core/services/cookie.service';
import { UUIDService } from '../core/shared/uuid.service';
import { Store, select } from '@ngrx/store';
import { AppState } from '../app.reducer';
import { isEmpty } from '../shared/empty.util';
import { take } from 'rxjs/operators';
import { SetCorrelationIdAction } from './correlation-id.actions';
import { Injectable } from '@angular/core';
import { correlationIdSelector } from './correlation-id.selector';

/**
 * Service to manage the correlation id, an id used to give context to server side logs
 */
@Injectable({
  providedIn: 'root'
})
export class CorrelationIdService {

  constructor(
    protected cookieService: CookieService,
    protected uuidService: UUIDService,
    protected store: Store<AppState>,
  ) {
  }

  /**
   * Initialize the correlation id based on the cookie or the ngrx store
   */
  initCorrelationId(): void {
    // first see of there's a cookie with a correlation-id
    let correlationId = this.cookieService.get('CORRELATION-ID');

    // if there isn't see if there's an ID in the store
    if (isEmpty(correlationId)) {
      correlationId = this.getCorrelationId();
    }

    // if no id was found, create a new id
    if (isEmpty(correlationId)) {
      correlationId = this.uuidService.generate();
    }

    // Store the correct id both in the store and as a cookie to ensure they're in sync
    this.store.dispatch(new SetCorrelationIdAction(correlationId));
    this.cookieService.set('CORRELATION-ID', correlationId);
  }

  /**
   * Get the correlation id from the store
   */
  getCorrelationId(): string {
    let correlationId;

    this.store.pipe(
      select(correlationIdSelector),
      take(1)
    ).subscribe((storeId: string) => {
      // we can do this because ngrx selects are synchronous
      correlationId = storeId;
    });

    return correlationId;
  }
}
