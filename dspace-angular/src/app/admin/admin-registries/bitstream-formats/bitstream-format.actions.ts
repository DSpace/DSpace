/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';
import { type } from '../../../shared/ngrx/type';
import { BitstreamFormat } from '../../../core/shared/bitstream-format.model';

/**
 * For each action type in an action group, make a simple
 * enum object for all of this group's action types.
 *
 * The 'type' utility function coerces strings into string
 * literal types and runs a simple check to guarantee all
 * action types in the application are unique.
 */
export const BitstreamFormatsRegistryActionTypes = {

  SELECT_FORMAT: type('dspace/bitstream-formats-registry/SELECT_FORMAT'),
  DESELECT_FORMAT: type('dspace/bitstream-formats-registry/DESELECT_FORMAT'),
  DESELECT_ALL_FORMAT: type('dspace/bitstream-formats-registry/DESELECT_ALL_FORMAT')
};

/**
 * Used to select a single bitstream format in the bitstream format registry
 */
export class BitstreamFormatsRegistrySelectAction implements Action {
  type = BitstreamFormatsRegistryActionTypes.SELECT_FORMAT;

  bitstreamFormat: BitstreamFormat;

  constructor(bitstreamFormat: BitstreamFormat) {
    this.bitstreamFormat = bitstreamFormat;
  }
}

/**
 * Used to deselect a single bitstream format in the bitstream format registry
 */
export class BitstreamFormatsRegistryDeselectAction implements Action {
  type = BitstreamFormatsRegistryActionTypes.DESELECT_FORMAT;

  bitstreamFormat: BitstreamFormat;

  constructor(bitstreamFormat: BitstreamFormat) {
    this.bitstreamFormat = bitstreamFormat;
  }
}

/**
 * Used to deselect all bitstream formats in the bitstream format registry
 */
export class BitstreamFormatsRegistryDeselectAllAction implements Action {
  type = BitstreamFormatsRegistryActionTypes.DESELECT_ALL_FORMAT;
}


/**
 * Export a type alias of all actions in this action group
 * so that reducers can easily compose action types
 * These are all the actions to perform on the bitstream format registry state
 */
export type BitstreamFormatsRegistryAction
  = BitstreamFormatsRegistrySelectAction
  | BitstreamFormatsRegistryDeselectAction
  | BitstreamFormatsRegistryDeselectAllAction;
