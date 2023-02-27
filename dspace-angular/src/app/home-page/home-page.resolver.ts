import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { SiteDataService } from '../core/data/site-data.service';
import { Site } from '../core/shared/site.model';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';

/**
 * The class that resolve the Site object for a route
 */
@Injectable()
export class HomePageResolver implements Resolve<Site> {
  constructor(private siteService: SiteDataService) {
  }

  /**
   * Method for resolving a site object
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns Observable<Site> Emits the found Site object, or an error if something went wrong
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Site> | Promise<Site> | Site {
    return this.siteService.find().pipe(take(1));
  }
}
