import { followLink } from '../../../shared/utils/follow-link-config.model';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { Store } from '@ngrx/store';
import { switchMap } from 'rxjs/operators';
import { RemoteData } from '../../data/remote-data';
import { getFirstCompletedRemoteData } from '../../shared/operators';
import { IdentifiableDataService } from '../../data/base/identifiable-data.service';

/**
 * This class represents a resolver that requests a specific item before the route is activated
 */
@Injectable()
export class SubmissionObjectResolver<T> implements Resolve<RemoteData<T>> {
    constructor(
      protected dataService: IdentifiableDataService<any>,
      protected store: Store<any>,
    ) {
    }

    /**
     * Method for resolving an item based on the parameters in the current route
     * @param {ActivatedRouteSnapshot} route The current ActivatedRouteSnapshot
     * @param {RouterStateSnapshot} state The current RouterStateSnapshot
     * @returns Observable<<RemoteData<Item>> Emits the found item based on the parameters in the current route,
     * or an error if something went wrong
     */
    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<RemoteData<T>> {
        const itemRD$ = this.dataService.findById(route.params.id,
            true,
            false,
            followLink('item'),
        ).pipe(
            getFirstCompletedRemoteData(),
            switchMap((wfiRD: RemoteData<any>) => wfiRD.payload.item as Observable<RemoteData<T>>),
            getFirstCompletedRemoteData()
        );
        return itemRD$;
    }
}
