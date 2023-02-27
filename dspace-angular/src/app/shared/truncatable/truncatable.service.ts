import { map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { createSelector, MemoizedSelector, select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { TruncatablesState, TruncatableState } from './truncatable.reducer';
import {
  TruncatableExpandAction,
  TruncatableToggleAction,
  TruncatableCollapseAction
} from './truncatable.actions';
import { hasValue } from '../empty.util';

const truncatableStateSelector = (state: TruncatablesState) => state.truncatable;

/**
 * Service responsible for truncating/clamping text and performing actions on truncatable elements
 */
@Injectable()
export class TruncatableService {

  constructor(private store: Store<TruncatablesState>) {
  }

  /**
   * Checks if a trunctable component should currently be collapsed
   * @param {string} id The UUID of the truncatable component
   * @returns {Observable<boolean>} Emits true if the state in the store is currently collapsed for the given truncatable component
   */
  isCollapsed(id: string): Observable<boolean> {
    return this.store.pipe(
      select(truncatableByIdSelector(id)),
      map((object: TruncatableState) => {
        if (object) {
          return object.collapsed;
        } else {
          return false;
        }
      })
    );
  }

  /**
   * Dispatches a toggle action to the store for a given truncatable component
   * @param {string} id The identifier of the truncatable for which the action is dispatched
   */
  public toggle(id: string): void {
    this.store.dispatch(new TruncatableToggleAction(id));
  }

  /**
   * Dispatches a collapse action to the store for a given truncatable component
   * @param {string} id The identifier of the truncatable for which the action is dispatched
   */
  public collapse(id: string): void {
    this.store.dispatch(new TruncatableCollapseAction(id));
  }

  /**
   * Dispatches an expand action to the store for a given truncatable component
   * @param {string} id The identifier of the truncatable for which the action is dispatched
   */
  public expand(id: string): void {
    this.store.dispatch(new TruncatableExpandAction(id));
  }
}

function truncatableByIdSelector(id: string): MemoizedSelector<TruncatablesState, TruncatableState> {
  return keySelector<TruncatableState>(id);
}

export function keySelector<T>(key: string): MemoizedSelector<TruncatablesState, T> {
  return createSelector(truncatableStateSelector, (state: TruncatableState) => {
    if (hasValue(state)) {
      return state[key];
    } else {
      return undefined;
    }
  });
}
