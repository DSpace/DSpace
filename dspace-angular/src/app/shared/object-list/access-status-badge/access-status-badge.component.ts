import { Component, Input } from '@angular/core';
import { catchError, map } from 'rxjs/operators';
import { Observable, of as observableOf } from 'rxjs';
import { AccessStatusObject } from './access-status.model';
import { hasValue } from '../../empty.util';
import { environment } from 'src/environments/environment';
import { Item } from 'src/app/core/shared/item.model';
import { AccessStatusDataService } from 'src/app/core/data/access-status-data.service';

@Component({
  selector: 'ds-access-status-badge',
  templateUrl: './access-status-badge.component.html'
})
/**
 * Component rendering the access status of an item as a badge
 */
export class AccessStatusBadgeComponent {

  @Input() item: Item;
  accessStatus$: Observable<string>;

  /**
   * Whether to show the access status badge or not
   */
  showAccessStatus: boolean;

  /**
   * Initialize instance variables
   *
   * @param {AccessStatusDataService} accessStatusDataService
   */
  constructor(private accessStatusDataService: AccessStatusDataService) { }

  ngOnInit(): void {
    this.showAccessStatus = environment.item.showAccessStatuses;
    if (!this.showAccessStatus || this.item == null) {
      // Do not show the badge if the feature is inactive or if the item is null.
      return;
    }
    if (this.item.accessStatus == null) {
      // In case the access status has not been loaded, do it individually.
      this.item.accessStatus = this.accessStatusDataService.findAccessStatusFor(this.item);
    }
    this.accessStatus$ = this.item.accessStatus.pipe(
      map((accessStatusRD) => {
        if (accessStatusRD.statusCode !== 401 && hasValue(accessStatusRD.payload)) {
          return accessStatusRD.payload;
        } else {
          return [];
        }
      }),
      map((accessStatus: AccessStatusObject) => hasValue(accessStatus.status) ? accessStatus.status : 'unknown'),
      map((status: string) => `access-status.${status.toLowerCase()}.listelement.badge`),
      catchError(() => observableOf('access-status.unknown.listelement.badge'))
    );
  }
}
