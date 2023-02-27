import { of as observableOf } from 'rxjs';
import { map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Action, Store } from '@ngrx/store';
import { Actions, createEffect, ofType } from '@ngrx/effects';

import { AppState } from './app.reducer';
import { StoreActionTypes } from './store.actions';
import { HostWindowResizeAction } from './shared/host-window.actions';

@Injectable()
export class StoreEffects {

   replay = createEffect(() => this.actions.pipe(
    ofType(StoreActionTypes.REPLAY),
    map((replayAction: Action) => {
      // TODO: should be able to replay all actions before the browser attempts to
      // replayAction.payload.forEach((action: Action) => {
      //   this.store.dispatch(action);
      // });
      return observableOf({});
    })), { dispatch: false });

   resize = createEffect(() => this.actions.pipe(
    ofType(StoreActionTypes.REPLAY, StoreActionTypes.REHYDRATE),
    map(() => new HostWindowResizeAction(window.innerWidth, window.innerHeight))
  ));

  constructor(private actions: Actions, private store: Store<AppState>) {

  }

}
