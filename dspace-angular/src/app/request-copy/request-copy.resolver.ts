import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { RemoteData } from '../core/data/remote-data';
import { ItemRequest } from '../core/shared/item-request.model';
import { Observable } from 'rxjs';
import { ItemRequestDataService } from '../core/data/item-request-data.service';
import { Injectable } from '@angular/core';
import { getFirstCompletedRemoteData } from '../core/shared/operators';

/**
 * Resolves an {@link ItemRequest} from the token found in the route's parameters
 */
@Injectable()
export class RequestCopyResolver implements Resolve<RemoteData<ItemRequest>> {

  constructor(
    private itemRequestDataService: ItemRequestDataService,
  ) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<RemoteData<ItemRequest>> | Promise<RemoteData<ItemRequest>> | RemoteData<ItemRequest> {
    return this.itemRequestDataService.findById(route.params.token).pipe(
      getFirstCompletedRemoteData(),
    );
  }

}
