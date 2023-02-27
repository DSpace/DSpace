import { map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';

import { StoreActionTypes } from '../../store.actions';
import { ResetObjectCacheTimestampsAction } from './object-cache.actions';

@Injectable()
export class ObjectCacheEffects {

  /**
   * When the store is rehydrated in the browser, set all cache
   * timestamps to 'now', because the time zone of the server can
   * differ from the client.
   *
   * This assumes that the server cached everything a negligible
   * time ago, and will likely need to be revisited later
   */
   fixTimestampsOnRehydrate = createEffect(() => this.actions$
    .pipe(ofType(StoreActionTypes.REHYDRATE),
      map(() => new ResetObjectCacheTimestampsAction(new Date().getTime()))
    ));

  constructor(private actions$: Actions) {
  }

}
