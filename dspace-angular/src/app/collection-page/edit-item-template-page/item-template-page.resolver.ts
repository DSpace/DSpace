import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { RemoteData } from '../../core/data/remote-data';
import { Item } from '../../core/shared/item.model';
import { ItemTemplateDataService } from '../../core/data/item-template-data.service';
import { Observable } from 'rxjs';
import { followLink } from '../../shared/utils/follow-link-config.model';
import { getFirstCompletedRemoteData } from '../../core/shared/operators';

/**
 * This class represents a resolver that requests a specific collection's item template before the route is activated
 */
@Injectable()
export class ItemTemplatePageResolver implements Resolve<RemoteData<Item>> {
  constructor(private itemTemplateService: ItemTemplateDataService) {
  }

  /**
   * Method for resolving a collection's item template based on the parameters in the current route
   * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
   * @param {RouterStateSnapshot} state The current RouterStateSnapshot
   * @returns Observable<<RemoteData<Collection>> Emits the found item template based on the parameters in the current route,
   * or an error if something went wrong
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<RemoteData<Item>> {
    return this.itemTemplateService.findByCollectionID(route.params.id, true, false, followLink('templateItemOf')).pipe(
      getFirstCompletedRemoteData(),
    );
  }
}
