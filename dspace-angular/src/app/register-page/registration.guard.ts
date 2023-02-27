import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { EpersonRegistrationService } from '../core/data/eperson-registration.service';
import { AuthService } from '../core/auth/auth.service';
import { map } from 'rxjs/operators';
import { getFirstCompletedRemoteData } from '../core/shared/operators';
import { redirectOn4xx } from '../core/shared/authorized.operators';

@Injectable({
  providedIn: 'root'
})
/**
 * A guard responsible for redirecting to 4xx pages upon retrieving a Registration object
 * The guard also adds the resulting RemoteData<Registration> object to the route's data for further usage in components
 * The reason this is a guard and not a resolver, is because it has to run before the EndUserAgreementCookieGuard
 */
export class RegistrationGuard implements CanActivate {
  constructor(private epersonRegistrationService: EpersonRegistrationService,
              private router: Router,
              private authService: AuthService) {
  }

  /**
   * Can the user activate the route? Returns true if the provided token resolves to an existing Registration, false if
   * not. Redirects to 4xx page on 4xx error. Adds the resulting RemoteData<Registration> object to the route's
   * data.registration property
   * @param route
   * @param state
   */
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    const token = route.params.token;
    return this.epersonRegistrationService.searchByToken(token).pipe(
      getFirstCompletedRemoteData(),
      redirectOn4xx(this.router, this.authService),
      map((rd) => {
        route.data = { ...route.data, registration: rd };
        return rd.hasSucceeded;
      }),
    );
  }

}
