/**
 * Returns the user state.
 * @function getUserState
 * @param {AppState} state Top level state.
 * @return {AuthState}
 */
import { AppState } from '../../app.reducer';

export const notificationsStateSelector = (state: AppState) => state.notifications;
