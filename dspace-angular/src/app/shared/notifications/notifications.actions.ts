/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';
import { type } from '../ngrx/type';
import { INotification } from './models/notification.model';

export const NotificationsActionTypes = {
  NEW_NOTIFICATION: type('dspace/notifications/NEW_NOTIFICATION'),
  REMOVE_ALL_NOTIFICATIONS: type('dspace/notifications/REMOVE_ALL_NOTIFICATIONS'),
  REMOVE_NOTIFICATION: type('dspace/notifications/REMOVE_NOTIFICATION'),
};


/**
 * New notification.
 * @class NewNotificationAction
 * @implements {Action}
 */
export class NewNotificationAction implements Action {
  public type: string = NotificationsActionTypes.NEW_NOTIFICATION;
  payload: INotification;

  constructor(notification: INotification) {
    this.payload = notification;
  }
}

/**
 * Remove all notifications.
 * @class RemoveAllNotificationsAction
 * @implements {Action}
 */
export class RemoveAllNotificationsAction implements Action {
  public type: string = NotificationsActionTypes.REMOVE_ALL_NOTIFICATIONS;

  constructor(public payload?: any) { }
}

/**
 * Remove a notification.
 * @class RemoveNotificationAction
 * @implements {Action}
 */
export class RemoveNotificationAction implements Action {
  public type: string = NotificationsActionTypes.REMOVE_NOTIFICATION;
  payload: any;

  constructor(notificationId: any) {
    this.payload = notificationId;
  }
}


/**
 * Actions type.
 * @type {NotificationsActions}
 */
export type NotificationsActions
  = NewNotificationAction
  | RemoveAllNotificationsAction
  | RemoveNotificationAction;
