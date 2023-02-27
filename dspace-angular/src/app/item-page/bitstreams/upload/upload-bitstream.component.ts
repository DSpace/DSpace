import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Observable, Subscription, of as observableOf } from 'rxjs';
import { RemoteData } from '../../../core/data/remote-data';
import { Item } from '../../../core/shared/item.model';
import { map, take, switchMap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { UploaderOptions } from '../../../shared/upload/uploader/uploader-options.model';
import { hasValue, isEmpty, isNotEmpty } from '../../../shared/empty.util';
import { ItemDataService } from '../../../core/data/item-data.service';
import { AuthService } from '../../../core/auth/auth.service';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { PaginatedList } from '../../../core/data/paginated-list.model';
import { Bundle } from '../../../core/shared/bundle.model';
import { BundleDataService } from '../../../core/data/bundle-data.service';
import { getFirstSucceededRemoteDataPayload, getFirstCompletedRemoteData } from '../../../core/shared/operators';
import { UploaderComponent } from '../../../shared/upload/uploader/uploader.component';
import { RequestService } from '../../../core/data/request.service';
import { getBitstreamModuleRoute } from '../../../app-routing-paths';
import { getEntityEditRoute } from '../../item-page-routing-paths';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'ds-upload-bitstream',
  templateUrl: './upload-bitstream.component.html'
})
/**
 * Page component for uploading a bitstream to an item
 */
export class UploadBitstreamComponent implements OnInit, OnDestroy {
  /**
   * The file uploader component
   */
  @ViewChild(UploaderComponent) uploaderComponent: UploaderComponent;

  /**
   * The ID of the item to upload a bitstream to
   */
  itemId: string;

  /**
   * The entity type of the item
   * This is fetched from the current URL and will determine the item's page route
   */
  entityType: string;

  /**
   * The item to upload a bitstream to
   */
  itemRD$: Observable<RemoteData<Item>>;

  /**
   * The item's bundles and default the default bundles that should be suggested (defined in environment)
   */
  bundles: Bundle[] = [];

  /**
   * The ID of the currently selected bundle to upload a bitstream to
   */
  selectedBundleId: string;

  /**
   * The name of the currently selected bundle to upload a bitstream to
   */
  selectedBundleName: string;

  /**
   * The uploader configuration options
   * @type {UploaderOptions}
   */
  uploadFilesOptions: UploaderOptions = Object.assign(new UploaderOptions(), {
    // URL needs to contain something to not produce any errors. This will be replaced once a bundle has been selected.
    url: 'placeholder',
    authToken: null,
    disableMultipart: false,
    itemAlias: null
  });

  /**
   * The prefix for all i18n notification messages within this component
   */
  NOTIFICATIONS_PREFIX = 'item.bitstreams.upload.notifications.';

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  subs: Subscription[] = [];

  constructor(protected route: ActivatedRoute,
              protected router: Router,
              protected itemService: ItemDataService,
              protected bundleService: BundleDataService,
              protected authService: AuthService,
              protected notificationsService: NotificationsService,
              protected translate: TranslateService,
              protected requestService: RequestService) {
  }

  /**
   * Initialize component properties:
   * itemRD$          Fetched from the current route data (populated by BitstreamPageResolver)
   * selectedBundleId Starts off by checking if the route's queryParams contain a "bundle" parameter. If none is found,
   *                  the ID of the first bundle in the list is selected.
   * Calls setUploadUrl after setting the selected bundle
   */
  ngOnInit(): void {
    this.itemId = this.route.snapshot.params.id;
    this.entityType = this.route.snapshot.params['entity-type'];
    this.itemRD$ = this.route.data.pipe(map((data) => data.dso));
    const bundlesRD$ = this.itemService.getBundles(this.itemId).pipe(
      getFirstCompletedRemoteData(),
      switchMap((remoteData: RemoteData<PaginatedList<Bundle>>) => {
        if (remoteData.hasSucceeded) {
          if (remoteData.payload.page) {
            this.bundles = remoteData.payload.page;
            for (const defaultBundle of environment.bundle.standardBundles) {
              let check = true;
              remoteData.payload.page.forEach((bundle: Bundle) => {
                // noinspection JSDeprecatedSymbols
                if (defaultBundle === bundle.name) {
                  check = false;
                }
              });
              if (check) {
                this.bundles.push(Object.assign(new Bundle(), {
                  _name: defaultBundle,
                  type: 'bundle'
                }));
              }
            }
          } else {
            this.bundles = environment.bundle.standardBundles.map((defaultBundle: string) => Object.assign(new Bundle(), {
                _name: defaultBundle,
                type: 'bundle'
              })
            );
          }
          return observableOf(remoteData.payload.page);
        }
      }));
    this.selectedBundleId = this.route.snapshot.queryParams.bundle;
    if (isNotEmpty(this.selectedBundleId)) {
      this.subs.push(this.bundleService.findById(this.selectedBundleId).pipe(
        getFirstSucceededRemoteDataPayload()
      ).subscribe((bundle: Bundle) => {
        this.selectedBundleName = bundle.name;
      }));
      this.setUploadUrl();
    }
    this.subs.push(bundlesRD$.subscribe());
  }

  /**
   * Create a new bundle with the filled in name on the current item
   */
  createBundle() {
    this.itemService.createBundle(this.itemId, this.selectedBundleName).pipe(
      getFirstSucceededRemoteDataPayload()
    ).subscribe((bundle: Bundle) => {
      this.selectedBundleId = bundle.id;
      this.notificationsService.success(
        this.translate.instant(this.NOTIFICATIONS_PREFIX + 'bundle.created.title'),
        this.translate.instant(this.NOTIFICATIONS_PREFIX + 'bundle.created.content')
      );
      this.setUploadUrl();
    });
  }

  /**
   * The user changed the bundle name
   * Reset the bundle ID
   */
  bundleNameChange() {
    this.selectedBundleId = undefined;
    for (const bundle of this.bundles) {
      // noinspection JSDeprecatedSymbols
      if (this.selectedBundleName === bundle.name) {
        this.selectedBundleId = bundle.id;
        break;
      }
    }
  }

  /**
   * Set the upload url to match the selected bundle ID
   */
  setUploadUrl() {
    this.bundleService.getBitstreamsEndpoint(this.selectedBundleId).pipe(take(1)).subscribe((href: string) => {
      this.uploadFilesOptions.url = href;
      if (isEmpty(this.uploadFilesOptions.authToken)) {
        this.uploadFilesOptions.authToken = this.authService.buildAuthHeader();
      }
      // Re-initialize the uploader component to ensure the latest changes to the options are applied
      if (this.uploaderComponent) {
        this.uploaderComponent.ngOnInit();
        this.uploaderComponent.ngAfterViewInit();
      }
    });
  }

  /**
   * The request was successful, redirect the user to the new bitstream's edit page
   * @param bitstream
   */
  public onCompleteItem(bitstream) {
    // Clear cached requests for this bundle's bitstreams to ensure lists on all pages are up-to-date
    this.bundleService.getBitstreamsEndpoint(this.selectedBundleId).pipe(take(1)).subscribe((href: string) => {
      this.requestService.removeByHrefSubstring(href);
    });

    // Bring over the item ID as a query parameter
    const queryParams = { itemId: this.itemId, entityType: this.entityType };
    this.router.navigate([getBitstreamModuleRoute(), bitstream.id, 'edit'], { queryParams: queryParams });
  }

  /**
   * The request was unsuccessful, display an error notification
   */
  public onUploadError() {
    this.notificationsService.error(null, this.translate.get(this.NOTIFICATIONS_PREFIX + 'upload.failed'));
  }

  /**
   * The user selected a bundle from the input suggestions
   * Set the bundle ID and Name properties, as well as the upload URL
   * @param bundle
   */
  onClick(bundle: Bundle) {
    this.selectedBundleId = bundle.id;
    this.selectedBundleName = bundle.name;
    if (bundle.id) {
      this.setUploadUrl();
    }
  }

  /**
   * When cancel is clicked, navigate back to the item's edit bitstreams page
   */
  onCancel() {
    this.router.navigate([getEntityEditRoute(this.entityType, this.itemId), 'bitstreams']);
  }

  /**
   * @returns {string} the current URL
   */
  getCurrentUrl() {
    return this.router.url;
  }

  /**
   * Unsubscribe from all open subscriptions when the component is destroyed
   */
  ngOnDestroy(): void {
    this.subs
      .filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
  }

}
