import { Store } from '@ngrx/store';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { SelectableListService } from './selectable-list.service';
import { ListableObject } from '../../object-collection/shared/listable-object.model';
import { hasValue } from '../../empty.util';
import {
  SelectableListDeselectAction,
  SelectableListDeselectSingleAction,
  SelectableListSelectAction,
  SelectableListSelectSingleAction
} from './selectable-list.actions';
import { AppState } from '../../../app.reducer';

class SelectableObject extends ListableObject {
  constructor(private value: string) {
    super();
  }

  equals(other: SelectableObject): boolean {
    return hasValue(this.value) && hasValue(other.value) && this.value === other.value;
  }

  getRenderTypes() {
    return ['selectable'];
  }
}

describe('SelectableListService', () => {
  const listID1 = 'id1';
  const value1 = 'Selected object';
  const value2 = 'Another selected object';
  const value3 = 'Selection';
  const value4 = 'Selected object numero 4';

  const selected1 = new SelectableObject(value1);
  const selected2 = new SelectableObject(value2);
  const selected3 = new SelectableObject(value3);
  const selected4 = new SelectableObject(value4);

  let service: SelectableListService;
  const store: Store<AppState> = jasmine.createSpyObj('store', {
    /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
    dispatch: {},
    /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
  });
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({

      providers: [
        {
          provide: Store, useValue: store
        }
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    service = new SelectableListService(store);
  });

  describe('when the selectSingle method is triggered', () => {
    beforeEach(() => {
      service.selectSingle(listID1, selected3);
    });

    it('SelectableListSelectSingleAction should be dispatched to the store', () => {
      expect(store.dispatch).toHaveBeenCalledWith(new SelectableListSelectSingleAction(listID1, selected3));
    });

  });

  describe('when the select method is triggered', () => {
    beforeEach(() => {
      service.select(listID1, [selected1, selected4]);
    });

    it('SelectableListSelectAction should be dispatched to the store', () => {
      expect(store.dispatch).toHaveBeenCalledWith(new SelectableListSelectAction(listID1, [selected1, selected4]));
    });
  });

  describe('when the deselectSingle method is triggered', () => {
    beforeEach(() => {
      service.deselectSingle(listID1, selected4);
    });

    it('SelectableListDeselectSingleAction should be dispatched to the store', () => {
      expect(store.dispatch).toHaveBeenCalledWith(new SelectableListDeselectSingleAction(listID1, selected4));
    });

  });

  describe('when the deselect method is triggered', () => {
    beforeEach(() => {
      service.deselect(listID1, [selected2, selected4]);
    });

    it('SelectableListDeselectAction should be dispatched to the store', () => {
      expect(store.dispatch).toHaveBeenCalledWith(new SelectableListDeselectAction(listID1, [selected2, selected4]));
    });
  });

});
