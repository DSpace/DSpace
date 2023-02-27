import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { RemoteData } from '../core/data/remote-data';
import { Observable } from 'rxjs';
import { Bitstream } from '../core/shared/bitstream.model';
import { BitstreamDataService } from '../core/data/bitstream-data.service';
import { followLink, FollowLinkConfig } from '../shared/utils/follow-link-config.model';
import { getFirstCompletedRemoteData } from '../core/shared/operators';

/**
 * The self links defined in this list are expected to be requested somewhere in the near future
 * Requesting them as embeds will limit the number of requests
 */
 export const BITSTREAM_PAGE_LINKS_TO_FOLLOW: FollowLinkConfig<Bitstream>[] = [
  followLink('bundle', {}, followLink('item')),
  followLink('format')
];

/**
 * This class represents a resolver that requests a specific bitstream before the route is activated
 */
@Injectable()
export class BitstreamPageResolver implements Resolve<RemoteData<Bitstream>> {
  constructor(private bitstreamService: BitstreamDataService) {
  }

  /**
   * Method for resolving a bitstream based on the parameters in the current route
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns Observable<<RemoteData<Item>> Emits the found bitstream based on the parameters in the current route,
   * or an error if something went wrong
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<RemoteData<Bitstream>> {
    return this.bitstreamService.findById(route.params.id, true, false, ...this.followLinks)
      .pipe(
        getFirstCompletedRemoteData(),
      );
  }
    /**
     * Method that returns the follow links to already resolve
     * The self links defined in this list are expected to be requested somewhere in the near future
     * Requesting them as embeds will limit the number of requests
     */
    get followLinks(): FollowLinkConfig<Bitstream>[] {
      return BITSTREAM_PAGE_LINKS_TO_FOLLOW;
    }
}
