import { ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthorizationDataService } from '../data/feature-authorization/authorization-data.service';
import { FeatureID } from '../data/feature-authorization/feature-id';
import { Injectable } from '@angular/core';

/**
 * An guard for redirecting users to the feedback page if user is authorized
 */
@Injectable()
export class FeedbackGuard implements CanActivate {

  constructor(private authorizationService: AuthorizationDataService) {
  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
    return this.authorizationService.isAuthorized(FeatureID.CanSendFeedback);
  }

}
