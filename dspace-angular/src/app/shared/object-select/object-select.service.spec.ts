import { ObjectSelectService } from './object-select.service';
import { Store } from '@ngrx/store';
import { ObjectSelectionListState, ObjectSelectionsState } from './object-select.reducer';
import { AppState } from '../../app.reducer';
import {
  ObjectSelectionDeselectAction,
  ObjectSelectionInitialDeselectAction,
  ObjectSelectionInitialSelectAction,
  ObjectSelectionResetAction,
  ObjectSelectionSelectAction,
  ObjectSelectionSwitchAction
} from './object-select.actions';
import { of } from 'rxjs';

describe('ObjectSelectService', () => {
  let service: ObjectSelectService;

  const mockKey = 'key';
  const mockObjectId = 'id1';

  const selectionStore: Store<ObjectSelectionListState> = jasmine.createSpyObj('selectionStore', {
    /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
    dispatch: {},
    /* eslint-enable no-empty,@typescript-eslint/no-empty-function */
    select: of(true)
  });

  const store: Store<ObjectSelectionsState> = jasmine.createSpyObj('store', {
    /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
    dispatch: {},
    /* eslint-enable no-empty,@typescript-eslint/no-empty-function */
    select: of(true)
  });

  const appStore: Store<AppState> = jasmine.createSpyObj('appStore', {
    /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
    dispatch: {},
    /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
    select: of(true)
  });

  beforeEach(() => {
    service = new ObjectSelectService(selectionStore, appStore);
  });

  describe('when the initialSelect method is triggered', () => {
    beforeEach(() => {
      service.initialSelect(mockKey, mockObjectId);
    });

    it('ObjectSelectionInitialSelectAction should be dispatched to the store', () => {
      expect(selectionStore.dispatch).toHaveBeenCalledWith(new ObjectSelectionInitialSelectAction(mockKey, mockObjectId));
    });
  });

  describe('when the initialDeselect method is triggered', () => {
    beforeEach(() => {
      service.initialDeselect(mockKey, mockObjectId);
    });

    it('ObjectSelectionInitialDeselectAction should be dispatched to the store', () => {
      expect(selectionStore.dispatch).toHaveBeenCalledWith(new ObjectSelectionInitialDeselectAction(mockKey, mockObjectId));
    });
  });

  describe('when the select method is triggered', () => {
    beforeEach(() => {
      service.select(mockKey, mockObjectId);
    });

    it('ObjectSelectionSelectAction should be dispatched to the store', () => {
      expect(selectionStore.dispatch).toHaveBeenCalledWith(new ObjectSelectionSelectAction(mockKey, mockObjectId));
    });
  });

  describe('when the deselect method is triggered', () => {
    beforeEach(() => {
      service.deselect(mockKey, mockObjectId);
    });

    it('ObjectSelectionDeselectAction should be dispatched to the store', () => {
      expect(selectionStore.dispatch).toHaveBeenCalledWith(new ObjectSelectionDeselectAction(mockKey, mockObjectId));
    });
  });

  describe('when the switch method is triggered', () => {
    beforeEach(() => {
      service.switch(mockKey, mockObjectId);
    });

    it('ObjectSelectionSwitchAction should be dispatched to the store', () => {
      expect(selectionStore.dispatch).toHaveBeenCalledWith(new ObjectSelectionSwitchAction(mockKey, mockObjectId));
    });
  });

  describe('when the reset method is triggered', () => {
    beforeEach(() => {
      service.reset(mockKey);
    });

    it('ObjectSelectionInitialSelectAction should be dispatched to the store', () => {
      expect(selectionStore.dispatch).toHaveBeenCalledWith(new ObjectSelectionResetAction(mockKey, null));
    });
  });

});
