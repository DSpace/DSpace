import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Collection } from '../core/shared/collection.model';
import { Observable } from 'rxjs';
import { CollectionDataService } from '../core/data/collection-data.service';
import { RemoteData } from '../core/data/remote-data';
import { followLink, FollowLinkConfig } from '../shared/utils/follow-link-config.model';
import { getFirstCompletedRemoteData } from '../core/shared/operators';
import { Store } from '@ngrx/store';
import { ResolvedAction } from '../core/resolving/resolver.actions';

/**
 * The self links defined in this list are expected to be requested somewhere in the near future
 * Requesting them as embeds will limit the number of requests
 */
export const COLLECTION_PAGE_LINKS_TO_FOLLOW: FollowLinkConfig<Collection>[] = [
  followLink('parentCommunity', {},
    followLink('parentCommunity')
  ),
  followLink('logo'),
];

/**
 * This class represents a resolver that requests a specific collection before the route is activated
 */
@Injectable()
export class CollectionPageResolver implements Resolve<RemoteData<Collection>> {
  constructor(
    private collectionService: CollectionDataService,
    private store: Store<any>
  ) {
  }

  /**
   * Method for resolving a collection based on the parameters in the current route
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns Observable<<RemoteData<Collection>> Emits the found collection based on the parameters in the current route,
   * or an error if something went wrong
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<RemoteData<Collection>> {
    const collectionRD$ = this.collectionService.findById(
      route.params.id,
      true,
      false,
      ...COLLECTION_PAGE_LINKS_TO_FOLLOW
    ).pipe(
      getFirstCompletedRemoteData()
    );

    collectionRD$.subscribe((collectionRD: RemoteData<Collection>) => {
      this.store.dispatch(new ResolvedAction(state.url, collectionRD.payload));
    });

    return collectionRD$;
  }
}
