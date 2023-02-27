import { Injectable } from '@angular/core';
import { MemoizedSelector, select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { SelectableListState } from './selectable-list.reducer';
import { AppState, keySelector } from '../../../app.reducer';
import { ListableObject } from '../../object-collection/shared/listable-object.model';
import {
  SelectableListDeselectAction,
  SelectableListDeselectAllAction,
  SelectableListDeselectSingleAction,
  SelectableListSelectAction,
  SelectableListSelectSingleAction
} from './selectable-list.actions';
import { hasValue, isNotEmpty } from '../../empty.util';

const selectableListsStateSelector = (state: AppState) => state.selectableLists;

const menuByIDSelector = (id: string): MemoizedSelector<AppState, SelectableListState> => {
  return keySelector<SelectableListState>(id, selectableListsStateSelector);
};

@Injectable()
export class SelectableListService {

  constructor(private store: Store<AppState>) {
  }

  /**
   * Retrieve a selectable list's state by its ID
   * @param {string} id ID of the requested Selectable list
   * @returns {Observable<SelectableListState>} Observable that emits the current state of the requested selectable list
   */
  getSelectableList(id: string): Observable<SelectableListState> {
    return this.store.pipe(select(menuByIDSelector(id)));
  }

  /**
   * Select an object in a specific list in the store
   * @param {string} id The id of the list on which the object should be selected
   * @param {ListableObject} object The object to select
   */
  selectSingle(id: string, object: ListableObject) {
    this.store.dispatch(new SelectableListSelectSingleAction(id, object));
  }

  /**
   * Select multiple objects in a specific list in the store
   * @param {string} id The id of the list on which the objects should be selected
   * @param {ListableObject[]} objects The objects to select
   */
  select(id: string, objects: ListableObject[]) {
    this.store.dispatch(new SelectableListSelectAction(id, objects));
  }

  /**
   * Deselect an object in a specific list in the store
   * @param {string} id The id of the list on which the object should be deselected
   * @param {ListableObject} object The object to deselect
   */
  deselectSingle(id: string, object: ListableObject) {
    this.store.dispatch(new SelectableListDeselectSingleAction(id, object));
  }

  /**
   * Deselect multiple objects in a specific list in the store
   * @param {string} id The id of the list on which the objects should be deselected
   * @param {ListableObject[]} objects The objects to deselect
   */
  deselect(id: string, objects: ListableObject[]) {
    this.store.dispatch(new SelectableListDeselectAction(id, objects));
  }

  /**
   * Deselect all objects in a specific list in the store
   * @param {string} id The id of the list on which the objects should be deselected
   */
  deselectAll(id: string) {
    this.store.dispatch(new SelectableListDeselectAllAction(id));
  }

  /**
   * Check if a given object is selected in a specific list
   * @param {string} id The ID of the selectable list the object should be selected in
   * @param {ListableObject} object The object to check for if it's selected
   * @returns {Observable<boolean>} Emits true if the given object is selected, emits false when it's deselected
   */
  isObjectSelected(id: string, object: ListableObject): Observable<boolean> {
    return this.getSelectableList(id).pipe(
      map((state: SelectableListState) => hasValue(state) && isNotEmpty(state.selection) && hasValue(state.selection.find((selected) => selected.equals(object)))),
      distinctUntilChanged()
    );
  }

  /**
   * Find a selected object by a custom condition
   * @param id        The ID of the selectable list to search in
   * @param condition The condition that the required object has to match
   */
  findSelectedByCondition(id: string, condition: (object: ListableObject) => boolean): Observable<ListableObject> {
    return this.getSelectableList(id).pipe(
      map((state: SelectableListState) => (hasValue(state) && isNotEmpty(state.selection)) ? state.selection.find((selected) => condition(selected)) : undefined)
    );
  }
}
