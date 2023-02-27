import { Component, OnInit } from '@angular/core';
import { DSpaceObject } from '../../../../../core/shared/dspace-object.model';
import { Observable } from 'rxjs';
import { RemoteData } from '../../../../../core/data/remote-data';
import { ActivatedRoute, Router } from '@angular/router';
import { first, map, take } from 'rxjs/operators';
import { getFirstCompletedRemoteData, getFirstSucceededRemoteData } from '../../../../../core/shared/operators';
import { hasValue, isEmpty } from '../../../../empty.util';
import { ResourceType } from '../../../../../core/shared/resource-type';
import { ComColDataService } from '../../../../../core/data/comcol-data.service';
import { NotificationsService } from '../../../../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { Community } from '../../../../../core/shared/community.model';
import { Collection } from '../../../../../core/shared/collection.model';

@Component({
  selector: 'ds-comcol-metadata',
  template: ''
})
export class ComcolMetadataComponent<TDomain extends Community | Collection> implements OnInit {
  /**
   * Frontend endpoint for this type of DSO
   */
  protected frontendURL: string;
  /**
   * The initial DSO object
   */
  public dsoRD$: Observable<RemoteData<TDomain>>;

  /**
   * The type of the dso
   */
  protected type: ResourceType;

  public constructor(
    protected dsoDataService: ComColDataService<TDomain>,
    protected router: Router,
    protected route: ActivatedRoute,
    protected notificationsService: NotificationsService,
    protected translate: TranslateService
  ) {
  }

  ngOnInit(): void {
    this.dsoRD$ = this.route.parent.data.pipe(first(), map((data) => data.dso));
  }

  /**
   * Updates an existing DSO based on the submitted user data and navigates to the edited object's home page
   * @param event   The event returned by the community/collection form. Contains the new dso and logo uploader
   */
  onSubmit(event) {

    const uploader = event.uploader;
    const deleteLogo = event.deleteLogo;

    const newLogo = hasValue(uploader) && uploader.queue.length > 0;
    if (newLogo) {
      this.dsoDataService.getLogoEndpoint(event.dso.uuid).pipe(take(1)).subscribe((href: string) => {
        uploader.options.url = href;
        uploader.uploadAll();
      });
    }

    if (!isEmpty(event.operations)) {
      this.dsoDataService.patch(event.dso, event.operations).pipe(getFirstCompletedRemoteData())
        .subscribe(async (response: RemoteData<DSpaceObject>) => {
          if (response.hasSucceeded) {
            if (!newLogo && !deleteLogo) {
              await this.router.navigate([this.frontendURL + event.dso.uuid]);
            }
            this.notificationsService.success(null, this.translate.get(`${this.type.value}.edit.notifications.success`));
          } else if (response.statusCode === 403) {
            this.notificationsService.error(null, this.translate.get(`${this.type.value}.edit.notifications.unauthorized`));
          } else {
            this.notificationsService.error(null, this.translate.get(`${this.type.value}.edit.notifications.error`));
          }
        });
    }
  }

  /**
   * Navigate to the home page of the object
   */
  navigateToHomePage() {
    this.dsoRD$.pipe(
      getFirstSucceededRemoteData(),
      take(1)
    ).subscribe((dsoRD: RemoteData<TDomain>) => {
      this.router.navigate([this.frontendURL + dsoRD.payload.id]);
      });
  }
}
