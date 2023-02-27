import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { mergeMap, take } from 'rxjs/operators';
import { ComColDataService } from '../../../../core/data/comcol-data.service';
import { CommunityDataService } from '../../../../core/data/community-data.service';
import { RemoteData } from '../../../../core/data/remote-data';
import { RouteService } from '../../../../core/services/route.service';
import { Community } from '../../../../core/shared/community.model';
import { getFirstSucceededRemoteDataPayload, } from '../../../../core/shared/operators';
import { ResourceType } from '../../../../core/shared/resource-type';
import { hasValue, isNotEmpty, isNotUndefined } from '../../../empty.util';
import { NotificationsService } from '../../../notifications/notifications.service';
import { RequestParam } from '../../../../core/cache/models/request-param.model';
import { RequestService } from '../../../../core/data/request.service';
import { Collection } from '../../../../core/shared/collection.model';

/**
 * Component representing the create page for communities and collections
 */
@Component({
  selector: 'ds-create-comcol',
  template: ''
})
export class CreateComColPageComponent<TDomain extends Collection | Community> implements OnInit {
  /**
   * Frontend endpoint for this type of DSO
   */
  protected frontendURL: string;

  /**
   * The provided UUID for the parent community
   */
  public parentUUID$: Observable<string>;

  /**
   * The parent community of the object that is to be created
   */
  public parentRD$: Observable<RemoteData<Community>>;

  /**
   * The UUID of the newly created object
   */
  private newUUID: string;

  /**
   * The type of the dso
   */
  protected type: ResourceType;

  public constructor(
    protected dsoDataService: ComColDataService<TDomain>,
    protected parentDataService: CommunityDataService,
    protected routeService: RouteService,
    protected router: Router,
    protected notificationsService: NotificationsService,
    protected translate: TranslateService,
    protected requestService: RequestService
  ) {

  }

  ngOnInit(): void {
    this.parentUUID$ = this.routeService.getQueryParameterValue('parent');
    this.parentUUID$.pipe(take(1)).subscribe((parentID: string) => {
      if (isNotEmpty(parentID)) {
        this.parentRD$ = this.parentDataService.findById(parentID);
      }
    });
  }

  /**
   * Creates a new DSO based on the submitted user data and navigates to the new object's home page
   * @param event   The event returned by the community/collection form. Contains the new dso and logo uploader
   */
  onSubmit(event) {
    const dso = event.dso;
    const uploader = event.uploader;

    this.parentUUID$.pipe(
      take(1),
      mergeMap((uuid: string) => {
      const params = uuid ? [new RequestParam('parent', uuid)] : [];
      return this.dsoDataService.create(dso, ...params)
        .pipe(getFirstSucceededRemoteDataPayload()
        );
      }))
      .subscribe((dsoRD: TDomain) => {
        if (isNotUndefined(dsoRD)) {
          this.newUUID = dsoRD.uuid;
          if (uploader.queue.length > 0) {
            this.dsoDataService.getLogoEndpoint(this.newUUID).pipe(take(1)).subscribe((href: string) => {
              uploader.options.url = href;
              uploader.uploadAll();
            });
          } else {
            this.navigateToNewPage();
          }
          this.dsoDataService.refreshCache(dsoRD);
        }
        this.notificationsService.success(null, this.translate.get(this.type.value + '.create.notifications.success'));
      });
  }

  /**
   * Navigate to home page
   */
  navigateToHome() {
    this.router.navigate(['/home']);
  }

  /**
   * Navigate to the page of the newly created object
   */
  navigateToNewPage() {
    if (hasValue(this.newUUID)) {
      this.router.navigate([this.frontendURL + this.newUUID]);
    }
  }
}
