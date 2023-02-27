import { BitstreamFormat } from '../../../core/shared/bitstream-format.model';
import {
  BitstreamFormatsRegistryAction,
  BitstreamFormatsRegistryActionTypes,
  BitstreamFormatsRegistryDeselectAction,
  BitstreamFormatsRegistrySelectAction
} from './bitstream-format.actions';

/**
 * The bitstream format registry state.
 * @interface BitstreamFormatRegistryState
 */
export interface BitstreamFormatRegistryState {
  selectedBitstreamFormats: BitstreamFormat[];
}

/**
 * The initial state.
 */
const initialState: BitstreamFormatRegistryState = {
  selectedBitstreamFormats: [],
};

/**
 * Reducer that handles BitstreamFormatsRegistryActions to modify the bitstream format registry state
 * @param state   The current BitstreamFormatRegistryState
 * @param action  The BitstreamFormatsRegistryAction to perform on the state
 */
export function bitstreamFormatReducer(state = initialState, action: BitstreamFormatsRegistryAction): BitstreamFormatRegistryState {

  switch (action.type) {

    case BitstreamFormatsRegistryActionTypes.SELECT_FORMAT: {
      return Object.assign({}, state, {
        selectedBitstreamFormats: [...state.selectedBitstreamFormats, (action as BitstreamFormatsRegistrySelectAction).bitstreamFormat]
      });
    }

    case BitstreamFormatsRegistryActionTypes.DESELECT_FORMAT: {
      return Object.assign({}, state, {
        selectedBitstreamFormats: state.selectedBitstreamFormats.filter(
          (selectedBitstreamFormats) => selectedBitstreamFormats !== (action as BitstreamFormatsRegistryDeselectAction).bitstreamFormat
        )
      });
    }

    case BitstreamFormatsRegistryActionTypes.DESELECT_ALL_FORMAT: {
      return Object.assign({}, state, {
        selectedBitstreamFormats: []
      });
    }
    default:
      return state;
  }
}
