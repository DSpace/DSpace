import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, CanActivateChild, RouterStateSnapshot } from '@angular/router';

import { hasValue } from '../empty.util';

@Injectable()
export class MockAdminGuard implements CanActivate, CanActivateChild {

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    // if being run in browser, enforce 'isAdmin' requirement
    if (typeof window === 'object' && hasValue(window.localStorage)) {
      if (window.localStorage.getItem('isAdmin') === 'true') {
        return true;
      }
      return false;
    }
    return true;
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    return this.canActivate(route, state);
  }
}
