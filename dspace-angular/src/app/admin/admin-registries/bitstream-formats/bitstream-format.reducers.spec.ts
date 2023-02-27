import { Action } from '@ngrx/store';
import { BitstreamFormat } from '../../../core/shared/bitstream-format.model';
import { bitstreamFormatReducer, BitstreamFormatRegistryState } from './bitstream-format.reducers';
import {
  BitstreamFormatsRegistryDeselectAction,
  BitstreamFormatsRegistryDeselectAllAction,
  BitstreamFormatsRegistrySelectAction
} from './bitstream-format.actions';

const bitstreamFormat1: BitstreamFormat = new BitstreamFormat();
bitstreamFormat1.id = 'test-uuid-1';
bitstreamFormat1.shortDescription = 'test-short-1';

const bitstreamFormat2: BitstreamFormat = new BitstreamFormat();
bitstreamFormat2.id = 'test-uuid-2';
bitstreamFormat2.shortDescription = 'test-short-2';

const initialState: BitstreamFormatRegistryState = {
  selectedBitstreamFormats: []
};

const bitstream1SelectedState: BitstreamFormatRegistryState = {
  selectedBitstreamFormats: [bitstreamFormat1]
};

const bitstream1and2SelectedState: BitstreamFormatRegistryState = {
  selectedBitstreamFormats: [bitstreamFormat1, bitstreamFormat2]
};

describe('BitstreamFormatReducer', () => {
  describe('BitstreamFormatsRegistryActionTypes.SELECT_FORMAT', () => {
    it('should add the format to the list of selected formats when initial list is empty', () => {
      const state = initialState;
      const action = new BitstreamFormatsRegistrySelectAction(bitstreamFormat1);
      const newState = bitstreamFormatReducer(state, action);

      expect(newState).toEqual(bitstream1SelectedState);
    });
    it('should add the format to the list of selected formats when formats are already present', () => {
      const state = bitstream1SelectedState;
      const action = new BitstreamFormatsRegistrySelectAction(bitstreamFormat2);
      const newState = bitstreamFormatReducer(state, action);

      expect(newState).toEqual(bitstream1and2SelectedState);
    });
  });
  describe('BitstreamFormatsRegistryActionTypes.DESELECT_FORMAT', () => {
    it('should deselect a format', () => {
      const state = bitstream1and2SelectedState;
      const action = new BitstreamFormatsRegistryDeselectAction(bitstreamFormat2);
      const newState = bitstreamFormatReducer(state, action);

      expect(newState).toEqual(bitstream1SelectedState);
    });
  });
  describe('BitstreamFormatsRegistryActionTypes.DESELECT_ALL_FORMAT', () => {
    it('should deselect all formats', () => {
      const state = bitstream1and2SelectedState;
      const action = new BitstreamFormatsRegistryDeselectAllAction();
      const newState = bitstreamFormatReducer(state, action);

      expect(newState).toEqual(initialState);
    });
  });
  describe('Invalid action', () => {
    it('should return the current state', () => {
      const state = initialState;
      const action = new NullAction();

      const newState = bitstreamFormatReducer(state, action);

      expect(newState).toEqual(state);
    });
  });
});

class NullAction implements Action {
  type = null;

  constructor() {
    // empty constructor
  }
}
