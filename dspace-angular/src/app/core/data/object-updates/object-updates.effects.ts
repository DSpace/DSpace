import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import {
  DiscardObjectUpdatesAction,
  ObjectUpdatesAction,
  ObjectUpdatesActionTypes,
  RemoveAllObjectUpdatesAction,
  RemoveObjectUpdatesAction
} from './object-updates.actions';
import { delay, filter, map, switchMap, take, tap } from 'rxjs/operators';
import { of as observableOf, race as observableRace, Subject } from 'rxjs';
import { hasNoValue, hasValue } from '../../../shared/empty.util';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { INotification } from '../../../shared/notifications/models/notification.model';
import {
  NotificationsActions,
  NotificationsActionTypes,
  RemoveNotificationAction
} from '../../../shared/notifications/notifications.actions';
import { Action } from '@ngrx/store';
import { NoOpAction } from '../../../shared/ngrx/no-op.action';

/**
 * NGRX effects for ObjectUpdatesActions
 */
@Injectable()
export class ObjectUpdatesEffects {

  /**
   * Identifier for when an action on all notifications is performed
   */
  private allIdentifier = 'all';

  /**
   * Map that keeps track of the latest ObjectUpdatesAction for each page's url
   */
  private actionMap$: {
    /* Use Subject instead of BehaviorSubject:
      we only want Actions that are fired while we're listening
      actions that were previously fired do not matter anymore
    */
    [url: string]: Subject<ObjectUpdatesAction>
  } = {};

  private notificationActionMap$: {
    /* Use Subject instead of BehaviorSubject:
      we only want Actions that are fired while we're listening
      actions that were previously fired do not matter anymore
    */
    [id: string]: Subject<NotificationsActions>
  } = { all: new Subject() };
  /**
   * Effect that makes sure all last fired ObjectUpdatesActions are stored in the map of this service, with the url as their key
   */
   mapLastActions$ = createEffect(() => this.actions$
    .pipe(
      ofType(...Object.values(ObjectUpdatesActionTypes)),
      map((action: ObjectUpdatesAction) => {
        if (hasValue((action as any).payload)) {
          const url: string = (action as any).payload.url;
          if (hasNoValue(this.actionMap$[url])) {
            this.actionMap$[url] = new Subject<ObjectUpdatesAction>();
          }
          this.actionMap$[url].next(action);
        }
      })
    ), { dispatch: false });

  /**
   * Effect that makes sure all last fired NotificationActions are stored in the notification map of this service, with the id as their key
   */
   mapLastNotificationActions$ = createEffect(() => this.actions$
    .pipe(
      ofType(...Object.values(NotificationsActionTypes)),
      map((action: RemoveNotificationAction) => {
          const id: string = action.payload.id || action.payload || this.allIdentifier;
          if (hasNoValue(this.notificationActionMap$[id])) {
            this.notificationActionMap$[id] = new Subject<NotificationsActions>();
          }
          this.notificationActionMap$[id].next(action);
        }
      )
    ), { dispatch: false });

  /**
   * Effect that checks whether the removeAction's notification timeout ends before a user triggers another ObjectUpdatesAction
   * When no ObjectUpdatesAction is fired during the timeout, a RemoteObjectUpdatesAction will be returned
   * When a REINSTATE action is fired during the timeout, a NO_ACTION action will be returned
   * When any other ObjectUpdatesAction is fired during the timeout, a RemoteObjectUpdatesAction will be returned
   */
   removeAfterDiscardOrReinstateOnUndo$ = createEffect(() => this.actions$
    .pipe(
      ofType(ObjectUpdatesActionTypes.DISCARD),
      switchMap((action: DiscardObjectUpdatesAction) => {
          const url: string = action.payload.url;
          const notification: INotification = action.payload.notification;
          const timeOut = notification.options.timeOut;

          let removeAction: Action = new RemoveObjectUpdatesAction(action.payload.url);
          if (action.payload.discardAll) {
            removeAction = new RemoveAllObjectUpdatesAction();
          }

          return observableRace(
            // Either wait for the delay and perform a remove action
            observableOf(removeAction).pipe(delay(timeOut)),
            // Or wait for a a user action
            this.actionMap$[url].pipe(
              take(1),
              tap(() => {
                this.notificationsService.remove(notification);
              }),
              map((updateAction: ObjectUpdatesAction) => {
                if (updateAction.type === ObjectUpdatesActionTypes.REINSTATE) {
                  // If someone reinstated, do nothing, just let the reinstating happen
                  return new NoOpAction();
                }
                // If someone performed another action, assume the user does not want to reinstate and remove all changes
                return removeAction;
              })
            ),
            this.notificationActionMap$[notification.id].pipe(
              filter((notificationsAction: NotificationsActions) => notificationsAction.type === NotificationsActionTypes.REMOVE_NOTIFICATION),
              map(() => {
                return removeAction;
              })
            ),
            this.notificationActionMap$[this.allIdentifier].pipe(
              filter((notificationsAction: NotificationsActions) => notificationsAction.type === NotificationsActionTypes.REMOVE_ALL_NOTIFICATIONS),
              map(() => {
                return removeAction;
              })
            )
          );
        }
      )
    ));

  constructor(private actions$: Actions,
              private notificationsService: NotificationsService) {
  }

}
