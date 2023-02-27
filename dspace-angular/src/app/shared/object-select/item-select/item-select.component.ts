import { Component, Input } from '@angular/core';
import { Item } from '../../../core/shared/item.model';
import { ObjectSelectService } from '../object-select.service';
import { ObjectSelectComponent } from '../object-select/object-select.component';
import { hasValueOperator, isNotEmpty } from '../../empty.util';
import { Observable } from 'rxjs';
import { getAllSucceededRemoteDataPayload } from '../../../core/shared/operators';
import { map } from 'rxjs/operators';
import { getItemPageRoute } from '../../../item-page/item-page-routing-paths';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';

@Component({
  selector: 'ds-item-select',
  templateUrl: './item-select.component.html'
})

/**
 * A component used to select items from a specific list and returning the UUIDs of the selected items
 */
export class ItemSelectComponent extends ObjectSelectComponent<Item> {

  /**
   * Whether or not to hide the collection column
   */
  @Input()
  hideCollection = false;

  /**
   * The routes to the items their pages
   * Key: Item ID
   * Value: Route to item page
   */
  itemPageRoutes$: Observable<{
    [itemId: string]: string
  }>;

  constructor(protected objectSelectService: ObjectSelectService,
              protected authorizationService: AuthorizationDataService ) {
    super(objectSelectService, authorizationService);
  }

  ngOnInit(): void {
    super.ngOnInit();
    if (!isNotEmpty(this.confirmButton)) {
      this.confirmButton = 'item.select.confirm';
    }
    this.itemPageRoutes$ = this.dsoRD$.pipe(
      hasValueOperator(),
      getAllSucceededRemoteDataPayload(),
      map((items) => {
        const itemPageRoutes = {};
        items.page.forEach((item) => itemPageRoutes[item.uuid] = getItemPageRoute(item));
        return itemPageRoutes;
      })
    );
  }

}
