import { type } from '../../shared/ngrx/type';
import { Action } from '@ngrx/store';
import { DSpaceObject } from '../shared/dspace-object.model';

export const ResolverActionTypes = {
  RESOLVED: type('dspace/resolver/RESOLVED')
};

/**
 * An action that indicates a route object has been resolved.
 *
 * It isn't used in a reducer for now. Its purpose is to be able to be notified that an object
 * has been resolved in an effect.
 */
export class ResolvedAction implements Action {
  type = ResolverActionTypes.RESOLVED;
  payload: {
    url: string,
    dso: DSpaceObject
  };

  constructor(url: string, dso: DSpaceObject) {
    this.payload = { url, dso };
  }
}

export type ResolverAction = ResolvedAction;
