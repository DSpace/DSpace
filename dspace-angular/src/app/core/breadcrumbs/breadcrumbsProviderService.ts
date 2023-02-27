import { Breadcrumb } from '../../breadcrumbs/breadcrumb/breadcrumb.model';
import { Observable } from 'rxjs';

/**
 * Service to calculate breadcrumbs for a single part of the route
 */
export interface BreadcrumbsProviderService<T> {

  /**
   * Method to calculate the breadcrumbs for a part of the route
   * @param key The key used to resolve the breadcrumb
   * @param url The url to use as a link for this breadcrumb
   */
  getBreadcrumbs(key: T, url: string): Observable<Breadcrumb[]>;
}
