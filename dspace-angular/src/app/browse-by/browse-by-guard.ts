import { ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot } from '@angular/router';
import { Injectable } from '@angular/core';
import { DSpaceObjectDataService } from '../core/data/dspace-object-data.service';
import { hasNoValue, hasValue } from '../shared/empty.util';
import { map, switchMap } from 'rxjs/operators';
import { getFirstSucceededRemoteData, getFirstSucceededRemoteDataPayload } from '../core/shared/operators';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of as observableOf } from 'rxjs';
import { BrowseDefinitionDataService } from '../core/browse/browse-definition-data.service';
import { BrowseDefinition } from '../core/shared/browse-definition.model';

@Injectable()
/**
 * A guard taking care of the correct route.data being set for the Browse-By components
 */
export class BrowseByGuard implements CanActivate {

  constructor(protected dsoService: DSpaceObjectDataService,
              protected translate: TranslateService,
              protected browseDefinitionService: BrowseDefinitionDataService) {
  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    const title = route.data.title;
    const id = route.params.id || route.queryParams.id || route.data.id;
    let browseDefinition$: Observable<BrowseDefinition>;
    if (hasNoValue(route.data.browseDefinition) && hasValue(id)) {
      browseDefinition$ = this.browseDefinitionService.findById(id).pipe(getFirstSucceededRemoteDataPayload());
    } else {
      browseDefinition$ = observableOf(route.data.browseDefinition);
    }
    const scope = route.queryParams.scope;
    const value = route.queryParams.value;
    const metadataTranslated = this.translate.instant('browse.metadata.' + id);
    return browseDefinition$.pipe(
      switchMap((browseDefinition) => {
        if (hasValue(scope)) {
          const dsoAndMetadata$ = this.dsoService.findById(scope).pipe(getFirstSucceededRemoteData());
          return dsoAndMetadata$.pipe(
            map((dsoRD) => {
              const name = dsoRD.payload.name;
              route.data = this.createData(title, id, browseDefinition, name, metadataTranslated, value, route);
              return true;
            })
          );
        } else {
          route.data = this.createData(title, id, browseDefinition, '', metadataTranslated, value, route);
          return observableOf(true);
        }
      })
    );
  }

  private createData(title, id, browseDefinition, collection, field, value, route) {
    return Object.assign({}, route.data, {
      title: title,
      id: id,
      browseDefinition: browseDefinition,
      collection: collection,
      field: field,
      value: hasValue(value) ? `"${value}"` : ''
    });
  }
}
