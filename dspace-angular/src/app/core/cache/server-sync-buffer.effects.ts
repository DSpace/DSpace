import { delay, exhaustMap, map, switchMap, take } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { coreSelector } from '../core.selectors';
import {
  AddToSSBAction,
  CommitSSBAction,
  EmptySSBAction,
  ServerSyncBufferActionTypes
} from './server-sync-buffer.actions';
import { Action, createSelector, MemoizedSelector, select, Store } from '@ngrx/store';
import { ServerSyncBufferEntry, ServerSyncBufferState } from './server-sync-buffer.reducer';
import { combineLatest as observableCombineLatest, Observable, of as observableOf } from 'rxjs';
import { RequestService } from '../data/request.service';
import { PatchRequest } from '../data/request.models';
import { ObjectCacheService } from './object-cache.service';
import { ApplyPatchObjectCacheAction } from './object-cache.actions';
import { hasValue, isNotEmpty, isNotUndefined } from '../../shared/empty.util';
import { RestRequestMethod } from '../data/rest-request-method';
import { environment } from '../../../environments/environment';
import { ObjectCacheEntry } from './object-cache.reducer';
import { Operation } from 'fast-json-patch';
import { NoOpAction } from '../../shared/ngrx/no-op.action';
import { CoreState } from '../core-state.model';

@Injectable()
export class ServerSyncBufferEffects {

  /**
   * When an ADDToSSBAction is dispatched
   * Set a time out (configurable per method type)
   * Then dispatch a CommitSSBAction
   * When the delay is running, no new AddToSSBActions are processed in this effect
   */
   setTimeoutForServerSync = createEffect(() => this.actions$
    .pipe(
      ofType(ServerSyncBufferActionTypes.ADD),
      exhaustMap((action: AddToSSBAction) => {
        const autoSyncConfig = environment.cache.autoSync;
        const timeoutInSeconds = autoSyncConfig.timePerMethod[action.payload.method] || autoSyncConfig.defaultTime;
        return observableOf(new CommitSSBAction(action.payload.method)).pipe(
          delay(timeoutInSeconds * 1000),
        );
      })
    ));

  /**
   * When a CommitSSBAction is dispatched
   * Create a list of actions for each entry in the current buffer state to be dispatched
   * When the list of actions is not empty, also dispatch an EmptySSBAction
   * When the list is empty dispatch a NO_ACTION placeholder action
   */
   commitServerSyncBuffer = createEffect(() => this.actions$
    .pipe(
      ofType(ServerSyncBufferActionTypes.COMMIT),
      switchMap((action: CommitSSBAction) => {
        return this.store.pipe(
          select(serverSyncBufferSelector()),
          take(1), /* necessary, otherwise delay will not have any effect after the first run */
          switchMap((bufferState: ServerSyncBufferState) => {
            const actions: Observable<Action>[] = bufferState.buffer
              .filter((entry: ServerSyncBufferEntry) => {
                /* If there's a request method, filter
                 If there's no filter, commit everything */
                if (hasValue(action.payload)) {
                  return entry.method === action.payload;
                }
                return true;
              })
              .map((entry: ServerSyncBufferEntry) => {
                if (entry.method === RestRequestMethod.PATCH) {
                  return this.applyPatch(entry.href);
                } else {
                  /* TODO implement for other request method types */
                }
              });

            /* Add extra action to array, to make sure the ServerSyncBuffer is emptied afterwards */
            if (isNotEmpty(actions) && isNotUndefined(actions[0])) {
              return observableCombineLatest(...actions).pipe(
              switchMap((array) => [...array, new EmptySSBAction(action.payload)])
            );
            } else {
              return observableOf(new NoOpAction());
            }
          })
        );
      })
    ));

  /**
   * private method to create an ApplyPatchObjectCacheAction based on a cache entry
   * and to do the actual patch request to the server
   * @param {string} href The self link of the cache entry
   * @returns {Observable<Action>} ApplyPatchObjectCacheAction to be dispatched
   */
  private applyPatch(href: string): Observable<Action> {
    const patchObject = this.objectCache.getByHref(href).pipe(
      take(1)
    );

    return patchObject.pipe(
      map((entry: ObjectCacheEntry) => {
        if (isNotEmpty(entry.patches)) {
          const flatPatch: Operation[] = [].concat(...entry.patches.map((patch) => patch.operations));
          if (isNotEmpty(flatPatch)) {
            this.requestService.send(new PatchRequest(this.requestService.generateRequestId(), href, flatPatch));
          }
        }
        return new ApplyPatchObjectCacheAction(href);
      })
    );
  }

  constructor(private actions$: Actions,
              private store: Store<CoreState>,
              private requestService: RequestService,
              private objectCache: ObjectCacheService) {

  }
}

export function serverSyncBufferSelector(): MemoizedSelector<CoreState, ServerSyncBufferState> {
  return createSelector(coreSelector, (state: CoreState) => state['cache/syncbuffer']);
}
