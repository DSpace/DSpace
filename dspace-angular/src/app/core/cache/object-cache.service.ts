import { Injectable } from '@angular/core';
import { createSelector, MemoizedSelector, select, Store } from '@ngrx/store';
import { applyPatch, Operation } from 'fast-json-patch';
import { combineLatest as observableCombineLatest, Observable, of as observableOf } from 'rxjs';

import { distinctUntilChanged, filter, map, mergeMap, switchMap, take } from 'rxjs/operators';
import { hasNoValue, hasValue, isEmpty, isNotEmpty } from '../../shared/empty.util';
import { CoreState } from '../core-state.model';
import { coreSelector } from '../core.selectors';
import { RestRequestMethod } from '../data/rest-request-method';
import { selfLinkFromAlternativeLinkSelector, selfLinkFromUuidSelector } from '../index/index.selectors';
import { GenericConstructor } from '../shared/generic-constructor';
import { getClassForType } from './builders/build-decorators';
import { LinkService } from './builders/link.service';
import { AddDependentsObjectCacheAction, AddPatchObjectCacheAction, AddToObjectCacheAction, ApplyPatchObjectCacheAction, RemoveDependentsObjectCacheAction, RemoveFromObjectCacheAction } from './object-cache.actions';

import { ObjectCacheEntry, ObjectCacheState } from './object-cache.reducer';
import { AddToSSBAction } from './server-sync-buffer.actions';
import { RemoveFromIndexBySubstringAction } from '../index/index.actions';
import { HALLink } from '../shared/hal-link.model';
import { CacheableObject } from './cacheable-object.model';
import { IndexName } from '../index/index-name.model';

/**
 * The base selector function to select the object cache in the store
 */
const objectCacheSelector = createSelector(
  coreSelector,
  (state: CoreState) => state['cache/object']
);

/**
 * Selector function to select an object entry by self link from the cache
 * @param selfLink The self link of the object
 */
const entryFromSelfLinkSelector =
  (selfLink: string): MemoizedSelector<CoreState, ObjectCacheEntry> => createSelector(
    objectCacheSelector,
    (state: ObjectCacheState) => state[selfLink],
  );

/**
 * A service to interact with the object cache
 */
@Injectable()
export class ObjectCacheService {
  constructor(
    private store: Store<CoreState>,
    private linkService: LinkService
  ) {
  }

  /**
   * Add an object to the cache
   *
   * @param object
   *    The object to add
   * @param msToLive
   *    The number of milliseconds it should be cached for
   * @param requestUUID
   *    The UUID of the request that resulted in this object
   * @param alternativeLink
   *    An optional alternative link to this object
   */
  add(object: CacheableObject, msToLive: number, requestUUID: string, alternativeLink?: string): void {
    object = this.linkService.removeResolvedLinks(object); // Ensure the object we're storing has no resolved links
    this.store.dispatch(new AddToObjectCacheAction(object, new Date().getTime(), msToLive, requestUUID, alternativeLink));
  }

  /**
   * Remove the object with the supplied href from the cache
   *
   * @param href
   *    The unique href of the object to be removed
   */
  remove(href: string): void {
    this.removeRelatedLinksFromIndex(href);
    this.store.dispatch(new RemoveFromObjectCacheAction(href));
  }

  private removeRelatedLinksFromIndex(href: string) {
    const cacheEntry$ = this.getByHref(href);
    const altLinks$ = cacheEntry$.pipe(map((entry: ObjectCacheEntry) => entry.alternativeLinks), take(1));
    const childLinks$ = cacheEntry$.pipe(map((entry: ObjectCacheEntry) => {
        return Object
          .entries(entry.data._links)
          .filter(([key, value]: [string, HALLink]) => key !== 'self')
          .map(([key, value]: [string, HALLink]) => value.href);
      }),
      take(1)
    );
    this.removeLinksFromAlternativeLinkIndex(altLinks$);
    this.removeLinksFromAlternativeLinkIndex(childLinks$);

  }

  private removeLinksFromAlternativeLinkIndex(links$: Observable<string[]>) {
    links$.subscribe((links: string[]) => links.forEach((link: string) => {
        this.store.dispatch(new RemoveFromIndexBySubstringAction(IndexName.ALTERNATIVE_OBJECT_LINK, link));
      }
    ));
  }

  /**
   * Get an observable of the object with the specified UUID
   *
   * @param uuid
   *    The UUID of the object to get
   * @return Observable<T>
   *    An observable of the requested object
   */
  getObjectByUUID<T extends CacheableObject>(uuid: string):
    Observable<T> {
    return this.store.pipe(
      select(selfLinkFromUuidSelector(uuid)),
      mergeMap((selfLink: string) => this.getObjectByHref<T>(selfLink)
      )
    );
  }

  /**
   * Get an observable of the object with the specified selfLink
   *
   * @param href
   *    The href of the object to get
   * @return Observable<T>
   *    An observable of the requested object
   */
  getObjectByHref<T extends CacheableObject>(href: string): Observable<T> {
    return this.getByHref(href).pipe(
      map((entry: ObjectCacheEntry) => {
          if (isNotEmpty(entry.patches)) {
            const flatPatch: Operation[] = [].concat(...entry.patches.map((patch) => patch.operations));
            const patchedData = applyPatch(entry.data, flatPatch, undefined, false).newDocument;
            return Object.assign({}, entry, { data: patchedData });
          } else {
            return entry;
          }
        }
      ),
      map((entry: ObjectCacheEntry) => {
        const type: GenericConstructor<T> = getClassForType((entry.data as any).type);
        if (typeof type !== 'function') {
          throw new Error(`${type} is not a valid constructor for ${JSON.stringify(entry.data)}`);
        }
        return Object.assign(new type(), entry.data) as T;
      })
    );
  }

  /**
   * Get an observable of the object cache entry with the specified selfLink
   *
   * @param href
   *    The href of the object to get
   * @return Observable<ObjectCacheEntry>
   *    An observable of the requested object cache entry
   */
  getByHref(href: string): Observable<ObjectCacheEntry> {
    return observableCombineLatest([
      this.getByAlternativeLink(href),
      this.getBySelfLink(href),
    ]).pipe(
      map((results: ObjectCacheEntry[]) => results.find((entry: ObjectCacheEntry) => hasValue(entry))),
      filter((entry: ObjectCacheEntry) => hasValue(entry))
    );
  }

  private getBySelfLink(selfLink: string): Observable<ObjectCacheEntry> {
    return this.store.pipe(
      select(entryFromSelfLinkSelector(selfLink))
    );
  }

  private getByAlternativeLink(alternativeLink: string): Observable<ObjectCacheEntry> {
    return this.store.pipe(
      select(selfLinkFromAlternativeLinkSelector(alternativeLink)),
      switchMap((selfLink) => this.getBySelfLink(selfLink)),
    );
  }

  /**
   * Get an observable of the request's uuid with the specified selfLink
   *
   * @param selfLink
   *    The selfLink of the object to get
   * @return Observable<string>
   *    An observable of the request's uuid
   */
  getRequestUUIDBySelfLink(selfLink: string): Observable<string> {
    return this.getByHref(selfLink).pipe(
      map((entry: ObjectCacheEntry) => entry.requestUUIDs[0]),
      distinctUntilChanged());
  }

  /**
   * Get an observable of the request's uuid with the specified uuid
   *
   * @param uuid
   *    The uuid of the object to get
   * @return Observable<string>
   *    An observable of the request's uuid
   */
  getRequestUUIDByObjectUUID(uuid: string): Observable<string> {
    return this.store.pipe(
      select(selfLinkFromUuidSelector(uuid)),
      mergeMap((selfLink: string) => this.getRequestUUIDBySelfLink(selfLink))
    );
  }

  /**
   * Get an observable for an array of objects of the same type
   * with the specified self links
   *
   * The type needs to be specified as well, in order to turn
   * the cached plain javascript object in to an instance of
   * a class.
   *
   * e.g. getList([
   *        'http://localhost:8080/api/core/collections/c96588c6-72d3-425d-9d47-fa896255a695',
   *        'http://localhost:8080/api/core/collections/cff860da-cf5f-4fda-b8c9-afb7ec0b2d9e'
   *      ], Collection)
   *
   * @param selfLinks
   *    An array of self links of the objects to get
   * @param type
   *    The type of the objects to get
   * @return Observable<Array<T>>
   */
  getList<T extends CacheableObject>(selfLinks: string[]): Observable<T[]> {
    if (isEmpty(selfLinks)) {
      return observableOf([]);
    } else {
      return observableCombineLatest(
        selfLinks.map((selfLink: string) => this.getObjectByHref<T>(selfLink))
      );
    }
  }

  /**
   * Check whether the object with the specified UUID is cached
   *
   * @param uuid
   *    The UUID of the object to check
   * @return boolean
   *    true if the object with the specified UUID is cached,
   *    false otherwise
   */
  hasByUUID(uuid: string): boolean {
    let result = false;

    /* NB: that this is only a solution because the select method is synchronous, see: https://github.com/ngrx/store/issues/296#issuecomment-269032571*/
    this.store.pipe(
      select(selfLinkFromUuidSelector(uuid)),
      take(1)
    ).subscribe((selfLink: string) => result = this.hasByHref(selfLink));

    return result;
  }

  /**
   * Check whether the object with the specified self link is cached. Note that it doesn't check
   * whether the response this object came in on is still valid. Just whether it exists.
   *
   * @param href
   *    The href of the object to check
   * @param requestUUID
   *    Optional. If the object exists, check whether it links back to this requestUUID
   * @return boolean
   *    true if the object with the specified href is cached,
   *    false otherwise
   */
  hasByHref(href: string, requestUUID?: string): boolean {
    let result = false;
    this.getByHref(href).subscribe((entry: ObjectCacheEntry) => {
      if (isNotEmpty(requestUUID)) {
        result = entry.requestUUIDs.includes(requestUUID);
      } else {
        result = true;
      }
    }).unsubscribe();
    return result;
  }

  /**
   * Create an observable that emits a new value whenever the availability of the cached object changes.
   * The value it emits is a boolean stating if the object exists in cache or not.
   * @param href The self link of the object to observe
   */
  hasByHref$(href: string): Observable<boolean> {
    return observableCombineLatest(
      this.getBySelfLink(href),
      this.getByAlternativeLink(href)
    ).pipe(
      map((entries: ObjectCacheEntry[]) => entries.some((entry) => hasValue(entry)))
    );
  }

  /**
   * Add operations to the existing list of operations for an ObjectCacheEntry
   * Makes sure the ServerSyncBuffer for this ObjectCacheEntry is updated
   * @param selfLink
   *     the uuid of the ObjectCacheEntry
   * @param {Operation[]} patch
   *     list of operations to perform
   */
  public addPatch(selfLink: string, patch: Operation[]) {
    this.store.dispatch(new AddPatchObjectCacheAction(selfLink, patch));
    this.store.dispatch(new AddToSSBAction(selfLink, RestRequestMethod.PATCH));
  }

  /**
   * Check whether there are any unperformed operations for an ObjectCacheEntry
   *
   * @param entry
   *    the entry to check
   * @return boolean
   *    false if the entry is there are no operations left in the ObjectCacheEntry, true otherwise
   */
  private isDirty(entry: ObjectCacheEntry): boolean {
    return isNotEmpty(entry.patches);
  }

  /**
   * Apply the existing operations on an ObjectCacheEntry in the store
   * NB: this does not make any server side changes
   * @param selfLink
   *     the link of the ObjectCacheEntry
   */
  private applyPatchesToCachedObject(selfLink: string) {
    this.store.dispatch(new ApplyPatchObjectCacheAction(selfLink));
  }

  /**
   * Add a new dependency between two cached objects.
   * When {@link dependsOnHref$} is invalidated, {@link href$} will be invalidated as well.
   *
   * This method should be called _after_ requests have been sent;
   * it will only work if both objects are already present in the cache.
   *
   * If either object is undefined, the dependency will not be added
   *
   * @param href$          the href of an object to add a dependency to
   * @param dependsOnHref$ the href of the new dependency
   */
  addDependency(href$: string | Observable<string>, dependsOnHref$: string | Observable<string>) {
    if (hasNoValue(href$) || hasNoValue(dependsOnHref$)) {
      return;
    }

    if (typeof href$ === 'string') {
      href$ = observableOf(href$);
    }
    if (typeof dependsOnHref$ === 'string') {
      dependsOnHref$ = observableOf(dependsOnHref$);
    }

    observableCombineLatest([
      href$,
      dependsOnHref$.pipe(
        switchMap(dependsOnHref => this.resolveSelfLink(dependsOnHref))
      ),
    ]).pipe(
      switchMap(([href, dependsOnSelfLink]: [string, string]) => {
        const dependsOnSelfLink$ = observableOf(dependsOnSelfLink);

        return observableCombineLatest([
          dependsOnSelfLink$,
          dependsOnSelfLink$.pipe(
            switchMap(selfLink => this.getBySelfLink(selfLink)),
            map(oce => oce?.dependentRequestUUIDs || []),
          ),
          this.getByHref(href).pipe(
            // only add the latest request to keep dependency index from growing indefinitely
            map((entry: ObjectCacheEntry) => entry?.requestUUIDs?.[0]),
          )
        ]);
      }),
      take(1),
    ).subscribe(([dependsOnSelfLink, currentDependents, newDependent]: [string, string[], string]) => {
      // don't dispatch if either href is invalid or if the new dependency already exists
      if (hasValue(dependsOnSelfLink) && hasValue(newDependent) && !currentDependents.includes(newDependent)) {
        this.store.dispatch(new AddDependentsObjectCacheAction(dependsOnSelfLink, [newDependent]));
      }
    });
  }

  /**
   * Clear all dependent requests associated with a cache entry.
   *
   * @href  the href of a cached object
   */
  removeDependents(href: string) {
    this.resolveSelfLink(href).pipe(
      take(1),
    ).subscribe((selfLink: string) => {
      if (hasValue(selfLink)) {
        this.store.dispatch(new RemoveDependentsObjectCacheAction(selfLink));
      }
    });
  }


  /**
   * Resolve the self link of an existing cached object from an arbitrary href
   *
   * @param href  any href
   * @return      an observable of the self link corresponding to the given href.
   *              Will emit the given href if it was a self link, another href
   *              if the given href was an alt link, or undefined if there is no
   *              cached object for this href.
   */
  private resolveSelfLink(href: string): Observable<string> {
    return this.getBySelfLink(href).pipe(
      switchMap((oce: ObjectCacheEntry) => {
        if (isNotEmpty(oce)) {
          return [href];
        } else {
          return this.store.pipe(
            select(selfLinkFromAlternativeLinkSelector(href)),
          );
        }
      }),
    );
  }

}
