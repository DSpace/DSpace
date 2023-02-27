import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { RemoteData } from '../core/data/remote-data';
import { Community } from '../core/shared/community.model';
import { CommunityDataService } from '../core/data/community-data.service';
import { followLink, FollowLinkConfig } from '../shared/utils/follow-link-config.model';
import { getFirstCompletedRemoteData } from '../core/shared/operators';
import { ResolvedAction } from '../core/resolving/resolver.actions';
import { Store } from '@ngrx/store';

/**
 * The self links defined in this list are expected to be requested somewhere in the near future
 * Requesting them as embeds will limit the number of requests
 */
export const COMMUNITY_PAGE_LINKS_TO_FOLLOW: FollowLinkConfig<Community>[] = [
  followLink('logo'),
  followLink('subcommunities'),
  followLink('collections'),
  followLink('parentCommunity')
];

/**
 * This class represents a resolver that requests a specific community before the route is activated
 */
@Injectable()
export class CommunityPageResolver implements Resolve<RemoteData<Community>> {
  constructor(
    private communityService: CommunityDataService,
    private store: Store<any>
  ) {
  }

  /**
   * Method for resolving a community based on the parameters in the current route
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns Observable<<RemoteData<Community>> Emits the found community based on the parameters in the current route,
   * or an error if something went wrong
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<RemoteData<Community>> {
    const communityRD$ = this.communityService.findById(
      route.params.id,
      true,
      false,
      ...COMMUNITY_PAGE_LINKS_TO_FOLLOW
    ).pipe(
      getFirstCompletedRemoteData(),
    );

    communityRD$.subscribe((communityRD: RemoteData<Community>) => {
      this.store.dispatch(new ResolvedAction(state.url, communityRD.payload));
    });

    return communityRD$;
  }
}
