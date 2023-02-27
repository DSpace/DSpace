import { Breadcrumb } from '../../breadcrumbs/breadcrumb/breadcrumb.model';
import { BreadcrumbsProviderService } from './breadcrumbsProviderService';
import { DSONameService } from './dso-name.service';
import { Observable, of as observableOf } from 'rxjs';
import { ChildHALResource } from '../shared/child-hal-resource.model';
import { LinkService } from '../cache/builders/link.service';
import { DSpaceObject } from '../shared/dspace-object.model';
import { followLink } from '../../shared/utils/follow-link-config.model';
import { find, map, switchMap } from 'rxjs/operators';
import { RemoteData } from '../data/remote-data';
import { hasValue } from '../../shared/empty.util';
import { Injectable } from '@angular/core';
import { getDSORoute } from '../../app-routing-paths';

/**
 * Service to calculate DSpaceObject breadcrumbs for a single part of the route
 */
@Injectable({
  providedIn: 'root'
})
export class DSOBreadcrumbsService implements BreadcrumbsProviderService<ChildHALResource & DSpaceObject> {
  constructor(
    protected linkService: LinkService,
    protected dsoNameService: DSONameService
  ) {

  }

  /**
   * Method to recursively calculate the breadcrumbs
   * This method returns the name and url of the key and all its parent DSOs recursively, top down
   * @param key The key (a DSpaceObject) used to resolve the breadcrumb
   * @param url The url to use as a link for this breadcrumb
   */
  getBreadcrumbs(key: ChildHALResource & DSpaceObject, url: string): Observable<Breadcrumb[]> {
    const label = this.dsoNameService.getName(key);
    const crumb = new Breadcrumb(label, url);
    const propertyName = key.getParentLinkKey();
    return this.linkService.resolveLink(key, followLink(propertyName))[propertyName].pipe(
      find((parentRD: RemoteData<ChildHALResource & DSpaceObject>) => parentRD.hasSucceeded || parentRD.statusCode === 204),
      switchMap((parentRD: RemoteData<ChildHALResource & DSpaceObject>) => {
        if (hasValue(parentRD) && hasValue(parentRD.payload)) {
          const parent = parentRD.payload;
          return this.getBreadcrumbs(parent, getDSORoute(parent));
        }
        return observableOf([]);

      }),
      map((breadcrumbs: Breadcrumb[]) => [...breadcrumbs, crumb])
    );
  }
}
