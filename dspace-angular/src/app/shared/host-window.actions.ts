import { Action } from '@ngrx/store';

import { type } from './ngrx/type';

export const HostWindowActionTypes = {
  RESIZE: type('dspace/host-window/RESIZE')
};

export class HostWindowResizeAction implements Action {
  type = HostWindowActionTypes.RESIZE;
  payload: {
    width: number;
    height: number;
  };

  constructor(width: number, height: number) {
    this.payload = { width, height };
  }
}

export type HostWindowAction
  = HostWindowResizeAction;
