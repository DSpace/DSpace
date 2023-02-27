import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';

@Injectable()
/**
 * Assemble the correct i18n key for the configuration search page's title depending on the current route's configuration parameter.
 * The format of the key will be "{configuration}.search.title" with:
 * - configuration: The current configuration stored in route.params
 */
export class ConfigurationSearchPageGuard implements CanActivate {
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    const configuration = route.params.configuration;

    const newTitle = configuration + '.search.title';

    route.data = { title: newTitle };
    return true;
  }
}
