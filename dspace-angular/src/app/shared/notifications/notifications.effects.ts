import { Injectable } from '@angular/core';
import { Actions } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { AppState } from '../../app.reducer';

@Injectable()
export class NotificationsEffects {

  /**
   * Authenticate user.
   * @method authenticate
   */
 /* @Effect()
  public timer: Observable<Action> = this.actions$
    .pipe(ofType(NotificationsActionTypes.NEW_NOTIFICATION_WITH_TIMER),
    // .debounceTime((action: any) => action.payload.options.timeOut)
    debounceTime(3000),
    map(() => new RemoveNotificationAction());
     .switchMap((action: NewNotificationWithTimerAction) => Observable
      .timer(30000)
      .mapTo(() => new RemoveNotificationAction())
    ));*/

  /**
   * @constructor
   * @param {Actions} actions$
   * @param {Store} store
   */
  constructor(private actions$: Actions,
              private store: Store<AppState>) {
  }
}
