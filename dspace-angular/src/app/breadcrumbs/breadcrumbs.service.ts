import { Injectable } from '@angular/core';
import {combineLatest, Observable, of as observableOf, ReplaySubject} from 'rxjs';
import {Breadcrumb} from './breadcrumb/breadcrumb.model';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';
import {filter, map, switchMap, tap} from 'rxjs/operators';
import {hasNoValue, hasValue, isUndefined} from '../shared/empty.util';

@Injectable({
  providedIn: 'root'
})
export class BreadcrumbsService {

  /**
   * Observable of the list of breadcrumbs for this page
   */
  breadcrumbs$: ReplaySubject<Breadcrumb[]> = new ReplaySubject(1);

  /**
   * Whether or not to show breadcrumbs on this page
   */
  showBreadcrumbs$: ReplaySubject<boolean> = new ReplaySubject(1);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  /**
   * Called by {@link AppComponent#constructor} (i.e. before routing)
   * such that no routing events are missed.
   */
  listenForRouteChanges() {
    // supply events to this.breadcrumbs$
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      tap(() => this.reset()),
      switchMap(() => this.resolveBreadcrumbs(this.route.root)),
    ).subscribe(this.breadcrumbs$);
  }

  /**
   * Method that recursively resolves breadcrumbs
   * @param route The route to get the breadcrumb from
   */
  private resolveBreadcrumbs(route: ActivatedRoute): Observable<Breadcrumb[]> {
    const data = route.snapshot.data;
    const routeConfig = route.snapshot.routeConfig;

    const last: boolean = hasNoValue(route.firstChild);
    if (last) {
      if (hasValue(data.showBreadcrumbs)) {
        this.showBreadcrumbs$.next(data.showBreadcrumbs);
      } else if (isUndefined(data.breadcrumb)) {
        this.showBreadcrumbs$.next(false);
      }
    }

    if (
      hasValue(data) && hasValue(data.breadcrumb) &&
      hasValue(routeConfig) && hasValue(routeConfig.resolve) && hasValue(routeConfig.resolve.breadcrumb)
    ) {
      const { provider, key, url } = data.breadcrumb;
      if (!last) {
        return combineLatest(provider.getBreadcrumbs(key, url), this.resolveBreadcrumbs(route.firstChild))
          .pipe(map((crumbs) => [].concat.apply([], crumbs)));
      } else {
        return provider.getBreadcrumbs(key, url);
      }
    }
    return !last ? this.resolveBreadcrumbs(route.firstChild) : observableOf([]);
  }

  /**
   * Resets the state of the breadcrumbs
   */
  private reset() {
    this.showBreadcrumbs$.next(true);
  }

}
