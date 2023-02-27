import { combineLatest as observableCombineLatest, Observable, interval } from 'rxjs';
import { filter, find, map, switchMap, take, takeWhile, debounce, debounceTime } from 'rxjs/operators';
import { hasNoValue, hasValue, hasValueOperator, isNotEmpty } from '../../shared/empty.util';
import { SearchResult } from '../../shared/search/models/search-result.model';
import { PaginatedList } from '../data/paginated-list.model';
import { RemoteData } from '../data/remote-data';
import { MetadataField } from '../metadata/metadata-field.model';
import { MetadataSchema } from '../metadata/metadata-schema.model';
import { BrowseDefinition } from './browse-definition.model';
import { DSpaceObject } from './dspace-object.model';
import { InjectionToken } from '@angular/core';
import { MonoTypeOperatorFunction, SchedulerLike } from 'rxjs/internal/types';

/**
 * Use this method instead of the RxJs debounceTime if you're waiting for debouncing in tests;
 * debounceTime doesn't work with fakeAsync/tick anymore as of Angular 13.2.6 & RxJs 7.5.5
 * Workaround suggested in https://github.com/angular/angular/issues/44351#issuecomment-1107454054
 * todo: remove once the above issue is fixed
 */
export const debounceTimeWorkaround = <T>(dueTime: number, scheduler?: SchedulerLike): MonoTypeOperatorFunction<T> => {
  return debounce(() => interval(dueTime, scheduler));
};

export const DEBOUNCE_TIME_OPERATOR = new InjectionToken<<T>(dueTime: number) => (source: Observable<T>) => Observable<T>>('debounceTime', {
  providedIn: 'root',
  factory: () => debounceTime
});

export const getRemoteDataPayload = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<T> =>
    source.pipe(map((remoteData: RemoteData<T>) => remoteData.payload));

export const getPaginatedListPayload = <T>() =>
  (source: Observable<PaginatedList<T>>): Observable<T[]> =>
    source.pipe(map((list: PaginatedList<T>) => list.page));

export const getAllCompletedRemoteData = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<RemoteData<T>> =>
    source.pipe(filter((rd: RemoteData<T>) => hasValue(rd) && rd.hasCompleted));

export const getFirstCompletedRemoteData = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<RemoteData<T>> =>
    source.pipe(getAllCompletedRemoteData(), take(1));

export const takeUntilCompletedRemoteData = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<RemoteData<T>> =>
    source.pipe(takeWhile((rd: RemoteData<T>) => hasNoValue(rd) || rd.isLoading, true));

export const getFirstSucceededRemoteData = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<RemoteData<T>> =>
    source.pipe(filter((rd: RemoteData<T>) => rd.hasSucceeded), take(1));

export const getFirstSucceededRemoteWithNotEmptyData = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<RemoteData<T>> =>
    source.pipe(find((rd: RemoteData<T>) => rd.hasSucceeded && isNotEmpty(rd.payload)));

/**
 * Get the first successful remotely retrieved object
 *
 * You usually don't want to use this, it is a code smell.
 * Work with the RemoteData object instead, that way you can
 * handle loading and errors correctly.
 *
 * These operators were created as a first step in refactoring
 * out all the instances where this is used incorrectly.
 */
export const getFirstSucceededRemoteDataPayload = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<T> =>
    source.pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload()
    );

/**
 * Get the first successful remotely retrieved object with not empty payload
 *
 * You usually don't want to use this, it is a code smell.
 * Work with the RemoteData object instead, that way you can
 * handle loading and errors correctly.
 *
 * These operators were created as a first step in refactoring
 * out all the instances where this is used incorrectly.
 */
export const getFirstSucceededRemoteDataWithNotEmptyPayload = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<T> =>
    source.pipe(
      getFirstSucceededRemoteWithNotEmptyData(),
      getRemoteDataPayload()
    );

/**
 * Get the all successful remotely retrieved objects
 *
 * You usually don't want to use this, it is a code smell.
 * Work with the RemoteData object instead, that way you can
 * handle loading and errors correctly.
 *
 * These operators were created as a first step in refactoring
 * out all the instances where this is used incorrectly.
 */
export const getAllSucceededRemoteDataPayload = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<T> =>
    source.pipe(
      getAllSucceededRemoteData(),
      getRemoteDataPayload()
    );

/**
 * Get the first successful remotely retrieved paginated list
 * as an array
 *
 * You usually don't want to use this, it is a code smell.
 * Work with the RemoteData object instead, that way you can
 * handle loading and errors correctly.
 *
 * You also don't want to ignore pagination and simply use the
 * page as an array.
 *
 * These operators were created as a first step in refactoring
 * out all the instances where this is used incorrectly.
 */
export const getFirstSucceededRemoteListPayload = <T>() =>
  (source: Observable<RemoteData<PaginatedList<T>>>): Observable<T[]> =>
    source.pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
      getPaginatedListPayload()
    );

/**
 * Get all successful remotely retrieved paginated lists
 * as arrays
 *
 * You usually don't want to use this, it is a code smell.
 * Work with the RemoteData object instead, that way you can
 * handle loading and errors correctly.
 *
 * You also don't want to ignore pagination and simply use the
 * page as an array.
 *
 * These operators were created as a first step in refactoring
 * out all the instances where this is used incorrectly.
 */
export const getAllSucceededRemoteListPayload = <T>() =>
  (source: Observable<RemoteData<PaginatedList<T>>>): Observable<T[]> =>
    source.pipe(
      getAllSucceededRemoteData(),
      getRemoteDataPayload(),
      getPaginatedListPayload()
    );

export const getFinishedRemoteData = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<RemoteData<T>> =>
    source.pipe(find((rd: RemoteData<T>) => !rd.isLoading));

export const getAllSucceededRemoteData = <T>() =>
  (source: Observable<RemoteData<T>>): Observable<RemoteData<T>> =>
    source.pipe(filter((rd: RemoteData<T>) => rd.hasSucceeded));

export const toDSpaceObjectListRD = <T extends DSpaceObject>() =>
  (source: Observable<RemoteData<PaginatedList<SearchResult<T>>>>): Observable<RemoteData<PaginatedList<T>>> =>
    source.pipe(
      filter((rd: RemoteData<PaginatedList<SearchResult<T>>>) => rd.hasSucceeded),
      map((rd: RemoteData<PaginatedList<SearchResult<T>>>) => {
        const dsoPage: T[] = rd.payload.page.filter((result) => hasValue(result)).map((searchResult: SearchResult<T>) => searchResult.indexableObject);
        const payload = Object.assign(rd.payload, { page: dsoPage }) as PaginatedList<T>;
        return Object.assign(rd, { payload: payload });
      })
    );

/**
 * Get the browse links from a definition by ID given an array of all definitions
 * @param {string} definitionID
 * @returns {(source: Observable<RemoteData<BrowseDefinition[]>>) => Observable<any>}
 */
export const getBrowseDefinitionLinks = (definitionID: string) =>
  (source: Observable<RemoteData<PaginatedList<BrowseDefinition>>>): Observable<any> =>
    source.pipe(
      getRemoteDataPayload(),
      getPaginatedListPayload(),
      map((browseDefinitions: BrowseDefinition[]) => browseDefinitions
        .find((def: BrowseDefinition) => def.id === definitionID)
      ),
      map((def: BrowseDefinition) => {
        if (isNotEmpty(def)) {
          return def._links;
        } else {
          throw new Error(`No metadata browse definition could be found for id '${definitionID}'`);
        }
      })
    );

/**
 * Get the first occurrence of an object within a paginated list
 */
export const getFirstOccurrence = () =>
  <T extends DSpaceObject>(source: Observable<RemoteData<PaginatedList<T>>>): Observable<RemoteData<T>> =>
    source.pipe(
      map((rd) => Object.assign(rd, { payload: rd.payload.page.length > 0 ? rd.payload.page[0] : undefined }))
    );

/**
 * Operator for turning the current page of bitstreams into an array
 */
export const paginatedListToArray = () =>
  <T extends DSpaceObject>(source: Observable<RemoteData<PaginatedList<T>>>): Observable<T[]> =>
    source.pipe(
      hasValueOperator(),
      map((objectRD: RemoteData<PaginatedList<T>>) => objectRD.payload.page.filter((object: T) => hasValue(object)))
    );

/**
 * Operator for turning a list of metadata fields into an array of string representing their schema.element.qualifier string
 */
export const metadataFieldsToString = () =>
  (source: Observable<RemoteData<PaginatedList<MetadataField>>>): Observable<string[]> =>
    source.pipe(
      hasValueOperator(),
      map((fieldRD: RemoteData<PaginatedList<MetadataField>>) => {
        return fieldRD.payload.page.filter((object: MetadataField) => hasValue(object));
      }),
      switchMap((fields: MetadataField[]) => {
        const fieldSchemaArray = fields.map((field: MetadataField) => {
          return field.schema.pipe(
            getFirstSucceededRemoteDataPayload(),
            map((schema: MetadataSchema) => ({ field, schema }))
          );
        });
        return isNotEmpty(fieldSchemaArray) ? observableCombineLatest(fieldSchemaArray) : [[]];
      }),
      map((fieldSchemaArray: { field: MetadataField, schema: MetadataSchema }[]): string[] => {
        return fieldSchemaArray.map((fieldSchema: { field: MetadataField, schema: MetadataSchema }) => fieldSchema.schema.prefix + '.' + fieldSchema.field.toString());
      })
    );

