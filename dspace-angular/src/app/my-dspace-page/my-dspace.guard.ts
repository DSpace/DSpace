import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, NavigationExtras, Router, RouterStateSnapshot } from '@angular/router';

import { Observable } from 'rxjs';
import { first, map } from 'rxjs/operators';
import { isEmpty } from '../shared/empty.util';
import { MYDSPACE_ROUTE } from './my-dspace-page.component';
import { MyDSpaceConfigurationValueType } from './my-dspace-configuration-value-type';
import { MyDSpaceConfigurationService } from './my-dspace-configuration.service';

/**
 * Prevent unauthorized activating and loading of mydspace configuration
 * @class MyDSpaceGuard
 */
@Injectable()
export class MyDSpaceGuard implements CanActivate {

  /**
   * @constructor
   */
  constructor(private configurationService: MyDSpaceConfigurationService, private router: Router) {
  }

  /**
   * True when configuration is valid
   * @method canActivate
   */
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.configurationService.getAvailableConfigurationTypes().pipe(
      first(),
      map((configurationList) => this.validateConfigurationParam(route.queryParamMap.get('configuration'), configurationList)));
  }

  /**
   * Check if the given configuration is present in the list of those available
   *
   * @param configuration
   *    the configuration to validate
   * @param configurationList
   *    the list of available configuration
   *
   */
  private validateConfigurationParam(configuration: string, configurationList: MyDSpaceConfigurationValueType[]): boolean {
    const configurationDefault: string = configurationList[0];
    if (isEmpty(configuration) || !configurationList.includes(configuration as MyDSpaceConfigurationValueType)) {
      // If configuration param is empty or is not included in available configurations redirect to a default configuration value
      const navigationExtras: NavigationExtras = {
        queryParams: {configuration: configurationDefault}
      };

      this.router.navigate([MYDSPACE_ROUTE], navigationExtras);
      return false;
    } else {
      return true;
    }
  }
}
