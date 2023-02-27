import { type } from './shared/ngrx/type';
import { Action } from '@ngrx/store';
import { AppState } from './app.reducer';

export const StoreActionTypes = {
  REHYDRATE: type('dspace/ngrx/REHYDRATE'),
  REPLAY: type('dspace/ngrx/REPLAY'),
};

export class StoreAction implements Action {
  type: string;
  payload: AppState | Action[];
  // eslint-disable-next-line @typescript-eslint/no-shadow
  constructor(type: string, payload: AppState | Action[]) {
    this.type = type;
    this.payload = payload;
  }
}
