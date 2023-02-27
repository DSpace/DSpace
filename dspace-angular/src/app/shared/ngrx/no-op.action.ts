import { Action } from '@ngrx/store';
import { type } from './type';

export const NO_OP_ACTION_TYPE = type('dspace/ngrx/NO_OP_ACTION');

/**
 * An action to use when nothing needs to happen, but you're forced to dispatch an action anyway.
 * e.g. an effect that needs to do something if a certain check succeeds, and nothing otherwise.
 *
 * It should not be used in any reducer or listened for in any effect.
 */
export class NoOpAction implements Action {
  public readonly type = NO_OP_ACTION_TYPE;
}
