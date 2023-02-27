import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DeleteComColPageComponent } from '../../shared/comcol/comcol-forms/delete-comcol-page/delete-comcol-page.component';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { CollectionDataService } from '../../core/data/collection-data.service';
import { Collection } from '../../core/shared/collection.model';
import { TranslateService } from '@ngx-translate/core';

/**
 * Component that represents the page where a user can delete an existing Collection
 */
@Component({
  selector: 'ds-delete-collection',
  styleUrls: ['./delete-collection-page.component.scss'],
  templateUrl: './delete-collection-page.component.html'
})
export class DeleteCollectionPageComponent extends DeleteComColPageComponent<Collection> {
  protected frontendURL = '/collections/';

  public constructor(
    protected dsoDataService: CollectionDataService,
    protected router: Router,
    protected route: ActivatedRoute,
    protected notifications: NotificationsService,
    protected translate: TranslateService,
  ) {
    super(dsoDataService, router, route, notifications, translate);
  }
}
