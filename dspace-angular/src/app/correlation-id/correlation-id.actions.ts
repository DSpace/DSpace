import { type } from '../shared/ngrx/type';
import { Action } from '@ngrx/store';

export const CorrelationIDActionTypes = {
  SET: type('dspace/core/correlationId/SET')
};

/**
 * Action for setting a new correlation ID
 */
export class SetCorrelationIdAction implements Action {
  type = CorrelationIDActionTypes.SET;

  constructor(public payload: string) {
  }
}

/**
 * Type alias for all correlation ID actions
 */
export type CorrelationIdAction = SetCorrelationIdAction;
