import { NotificationsActions, NotificationsActionTypes, RemoveNotificationAction } from './notifications.actions';
import { INotification } from './models/notification.model';

/**
 * The notification state.
 */
// eslint-disable-next-line @typescript-eslint/no-empty-interface
export interface NotificationsState extends Array<INotification> {

}

/**
 * The initial state.
 */
const initialState: NotificationsState = [];

/**
 * The reducer function.
 * @function reducer
 * @param {State} state Current state
 * @param {NotificationsActions} action Incoming action
 */
export function notificationsReducer(state: any = initialState, action: NotificationsActions): NotificationsState {

  switch (action.type) {
    case NotificationsActionTypes.NEW_NOTIFICATION:
      return [...state, action.payload];

    case NotificationsActionTypes.REMOVE_ALL_NOTIFICATIONS:
      return [];

    case NotificationsActionTypes.REMOVE_NOTIFICATION:
      return removeNotification(state, action as RemoveNotificationAction);

    default:
      return state;
  }
}

const removeNotification = (state: NotificationsState, action: RemoveNotificationAction): NotificationsState => {
  return state.filter((item: INotification) => item.id !== action.payload);
};
