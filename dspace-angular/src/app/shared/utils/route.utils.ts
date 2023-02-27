import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { hasValue } from '../empty.util';
import { URLCombiner } from '../../core/url-combiner/url-combiner';

/**
 * Util function to retrieve the current path (without query parameters) the user is on
 * @param router The router service
 */
export function currentPath(router: Router) {
  const urlTree = router.parseUrl(router.url);
  return '/' + urlTree.root.children.primary.segments.map((it) => it.path).join('/');
}

export function currentPathFromSnapshot(route: ActivatedRouteSnapshot): string {
  if (hasValue(route.parent)) {
    const parentRoute: string = currentPathFromSnapshot(route.parent);
    return new URLCombiner(parentRoute, route.routeConfig.path).toString();
  }
  return route.routeConfig ? route.routeConfig.path : '';
}
