import { Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, take } from 'rxjs/operators';
import { AppState } from '../../app.reducer';
import { isAuthenticationBlocking } from './selectors';

/**
 * A guard that blocks the loading of any
 * route until the authentication status has loaded.
 * To ensure all rest requests get the correct auth header.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthBlockingGuard implements CanActivate {

  constructor(private store: Store<AppState>) {
  }

  /**
   * True when the authentication isn't blocking everything
   */
  canActivate(): Observable<boolean> {
    return this.store.pipe(select(isAuthenticationBlocking)).pipe(
      map((isBlocking: boolean) => isBlocking === false),
      distinctUntilChanged(),
      filter((finished: boolean) => finished === true),
      take(1),
    );
  }

}
