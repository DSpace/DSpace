import { Observable, of as observableOf } from 'rxjs';
import { Injectable } from '@angular/core';
import { BreadcrumbsProviderService } from '../core/breadcrumbs/breadcrumbsProviderService';
import { Breadcrumb } from '../breadcrumbs/breadcrumb/breadcrumb.model';
import { Process } from './processes/process.model';

/**
 * Service to calculate process breadcrumbs for a single part of the route
 */
@Injectable()
export class ProcessBreadcrumbsService implements BreadcrumbsProviderService<Process> {

  /**
   * Method to calculate the breadcrumbs
   * @param key The key used to resolve the breadcrumb
   * @param url The url to use as a link for this breadcrumb
   */
  getBreadcrumbs(key: Process, url: string): Observable<Breadcrumb[]> {
    return observableOf([new Breadcrumb(key.processId + ' - ' + key.scriptName, url)]);
  }
}
