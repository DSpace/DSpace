import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';

import { hasNoValue, hasValue } from '../../shared/empty.util';
import { CommunityDataService } from '../../core/data/community-data.service';
import { RemoteData } from '../../core/data/remote-data';
import { Community } from '../../core/shared/community.model';
import { map, tap } from 'rxjs/operators';
import { Observable, of as observableOf } from 'rxjs';
import { getFirstCompletedRemoteData } from '../../core/shared/operators';

/**
 * Prevent creation of a collection without a parent community provided
 * @class CreateCollectionPageGuard
 */
@Injectable()
export class CreateCollectionPageGuard implements CanActivate {
  public constructor(private router: Router, private communityService: CommunityDataService) {
  }

  /**
   * True when either a parent ID query parameter has been provided and the parent ID resolves to a valid parent community
   * Reroutes to a 404 page when the page cannot be activated
   * @method canActivate
   */
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    const parentID = route.queryParams.parent;
    if (hasNoValue(parentID)) {
      this.router.navigate(['/404']);
      return observableOf(false);
    }
    return this.communityService.findById(parentID)
      .pipe(
        getFirstCompletedRemoteData(),
        map((communityRD: RemoteData<Community>) => hasValue(communityRD) && communityRD.hasSucceeded && hasValue(communityRD.payload)),
        tap((isValid: boolean) => {
          if (!isValid) {
            this.router.navigate(['/404']);
          }
        })
    );
  }
}
