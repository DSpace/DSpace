import { merge as observableMerge, Observable } from 'rxjs';
import { distinctUntilChanged, filter, find, map, mergeMap, partition, take, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';

import { hasValue, isEmpty, isNotEmpty, isNotUndefined, isUndefined } from '../../shared/empty.util';
import { PatchRequest } from '../data/request.models';
import { RequestService } from '../data/request.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { jsonPatchOperationsByResourceType } from './selectors';
import { JsonPatchOperationsResourceEntry } from './json-patch-operations.reducer';
import {
  CommitPatchOperationsAction, DeletePendingJsonPatchOperationsAction,
  RollbacktPatchOperationsAction,
  StartTransactionPatchOperationsAction
} from './json-patch-operations.actions';
import { JsonPatchOperationModel } from './json-patch.model';
import { getFirstCompletedRemoteData } from '../shared/operators';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { RemoteData } from '../data/remote-data';
import { CoreState } from '../core-state.model';

/**
 * An abstract class that provides methods to make JSON Patch requests.
 */
export abstract class JsonPatchOperationsService<ResponseDefinitionDomain, PatchRequestDefinition extends PatchRequest> {

  protected abstract requestService: RequestService;
  protected abstract store: Store<CoreState>;
  protected abstract linkPath: string;
  protected abstract halService: HALEndpointService;
  protected abstract rdbService: RemoteDataBuildService;
  protected abstract patchRequestConstructor: any;

  /**
   * Submit a new JSON Patch request with all operations stored in the state that are ready to be dispatched
   *
   * @param hrefObs
   *    Observable of request href
   * @param resourceType
   *    The resource type value
   * @param resourceId
   *    The resource id value
   * @return Observable<ResponseDefinitionDomain>
   *    observable of response
   */
  protected submitJsonPatchOperations(hrefObs: Observable<string>, resourceType: string, resourceId?: string): Observable<ResponseDefinitionDomain> {
    const requestId = this.requestService.generateRequestId();
    let startTransactionTime = null;
    const [patchRequest$, emptyRequest$] = partition((request: PatchRequestDefinition) => isNotEmpty(request.body))(hrefObs.pipe(
      mergeMap((endpointURL: string) => {
        return this.store.select(jsonPatchOperationsByResourceType(resourceType)).pipe(
          take(1),
          filter((operationsList: JsonPatchOperationsResourceEntry) => isUndefined(operationsList) || !(operationsList.commitPending)),
          tap(() => startTransactionTime = new Date().getTime()),
          map((operationsList: JsonPatchOperationsResourceEntry) => {
            const body: JsonPatchOperationModel[] = [];
            if (isNotEmpty(operationsList)) {
              if (isNotEmpty(resourceId)) {
                if (isNotUndefined(operationsList.children[resourceId]) && isNotEmpty(operationsList.children[resourceId].body)) {
                  operationsList.children[resourceId].body.forEach((entry) => {
                    body.push(entry.operation);
                  });
                }
              } else {
                Object.keys(operationsList.children)
                  .filter((key) => operationsList.children.hasOwnProperty(key))
                  .filter((key) => hasValue(operationsList.children[key]))
                  .filter((key) => hasValue(operationsList.children[key].body))
                  .forEach((key) => {
                    operationsList.children[key].body.forEach((entry) => {
                      body.push(entry.operation);
                    });
                  });
              }
            }
            return this.getRequestInstance(requestId, endpointURL, body);
          }));
      })));

    return observableMerge(
      emptyRequest$.pipe(
        filter((request: PatchRequestDefinition) => isEmpty(request.body)),
        tap(() => startTransactionTime = null),
        map(() => null)),
      patchRequest$.pipe(
        filter((request: PatchRequestDefinition) => isNotEmpty(request.body)),
        tap(() => this.store.dispatch(new StartTransactionPatchOperationsAction(resourceType, resourceId, startTransactionTime))),
        tap((request: PatchRequestDefinition) => this.requestService.send(request)),
        mergeMap(() => {
          return this.rdbService.buildFromRequestUUID(requestId).pipe(
            getFirstCompletedRemoteData(),
            find((rd: RemoteData<any>) => startTransactionTime < rd.timeCompleted),
            map((rd: RemoteData<any>) => {
              if (rd.hasFailed) {
                this.store.dispatch(new RollbacktPatchOperationsAction(resourceType, resourceId));
                throw new Error(rd.errorMessage);
              } else if (hasValue(rd.payload) && isNotEmpty(rd.payload.dataDefinition)) {
                this.store.dispatch(new CommitPatchOperationsAction(resourceType, resourceId));
                return rd.payload.dataDefinition;
              }
            }),
            distinctUntilChanged()
        );
        }))
    );
  }

  /**
   * Dispatch an action to delete all pending JSON patch Operations.
   */
  public deletePendingJsonPatchOperations() {
    this.store.dispatch(new DeletePendingJsonPatchOperationsAction());
  }

  /**
   * Return an instance for RestRequest class
   *
   * @param uuid
   *    The request uuid
   * @param href
   *    The request href
   * @param body
   *    The request body
   * @return Object<PatchRequestDefinition>
   *    instance of PatchRequestDefinition
   */
  protected getRequestInstance(uuid: string, href: string, body?: any): PatchRequestDefinition {
    return new this.patchRequestConstructor(uuid, href, body);
  }

  protected getEndpointByIDHref(endpoint, resourceID): string {
    return isNotEmpty(resourceID) ? `${endpoint}/${resourceID}` : `${endpoint}`;
  }

  /**
   * Make a new JSON Patch request with all operations related to the specified resource type
   *
   * @param linkPath
   *    The link path of the request
   * @param scopeId
   *    The scope id
   * @param resourceType
   *    The resource type value
   * @return Observable<ResponseDefinitionDomain>
   *    observable of response
   */
  public jsonPatchByResourceType(linkPath: string, scopeId: string, resourceType: string): Observable<ResponseDefinitionDomain> {
    const href$ = this.halService.getEndpoint(linkPath).pipe(
      filter((href: string) => isNotEmpty(href)),
      distinctUntilChanged(),
      map((endpointURL: string) => this.getEndpointByIDHref(endpointURL, scopeId)));

    return this.submitJsonPatchOperations(href$, resourceType);
  }

  /**
   * Select the jsonPatch operation related to the specified resource type.
   * @param resourceType
   */
  public hasPendingOperations(resourceType: string): Observable<boolean> {
    return this.store.select(jsonPatchOperationsByResourceType(resourceType)).pipe(
      map((val) =>  !isEmpty(val) && Object.values(val.children)
        .filter((section) => !isEmpty((section as any).body)).length > 0),
      distinctUntilChanged(),
    );
  }

  /**
   * Make a new JSON Patch request with all operations related to the specified resource id
   *
   * @param linkPath
   *    The link path of the request
   * @param scopeId
   *    The scope id
   * @param resourceType
   *    The resource type value
   * @param resourceId
   *    The resource id value
   * @return Observable<ResponseDefinitionDomain>
   *    observable of response
   */
  public jsonPatchByResourceID(linkPath: string, scopeId: string, resourceType: string, resourceId: string): Observable<ResponseDefinitionDomain> {
    const hrefObs = this.halService.getEndpoint(linkPath).pipe(
      filter((href: string) => isNotEmpty(href)),
      distinctUntilChanged(),
      map((endpointURL: string) => this.getEndpointByIDHref(endpointURL, scopeId)));

    return this.submitJsonPatchOperations(hrefObs, resourceType, resourceId);
  }
}
