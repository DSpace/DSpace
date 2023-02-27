import { Component, OnDestroy, OnInit } from '@angular/core';
import { filter, map, switchMap, take } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { hasValue, isNotEmpty } from '../../../shared/empty.util';
import { getFirstCompletedRemoteData, getFirstSucceededRemoteDataPayload } from '../../../core/shared/operators';
import { Bitstream } from '../../../core/shared/bitstream.model';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';
import { FeatureID } from '../../../core/data/feature-authorization/feature-id';
import { AuthService } from '../../../core/auth/auth.service';
import { combineLatest as observableCombineLatest, Observable, of as observableOf, Subscription } from 'rxjs';
import { getBitstreamDownloadRoute, getForbiddenRoute } from '../../../app-routing-paths';
import { TranslateService } from '@ngx-translate/core';
import { EPerson } from '../../../core/eperson/models/eperson.model';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ItemRequestDataService } from '../../../core/data/item-request-data.service';
import { ItemRequest } from '../../../core/shared/item-request.model';
import { Item } from '../../../core/shared/item.model';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { DSONameService } from '../../../core/breadcrumbs/dso-name.service';
import { Location } from '@angular/common';
import { BitstreamDataService } from '../../../core/data/bitstream-data.service';
import { getItemPageRoute } from '../../item-page-routing-paths';

@Component({
  selector: 'ds-bitstream-request-a-copy-page',
  templateUrl: './bitstream-request-a-copy-page.component.html'
})
/**
 * Page component for requesting a copy for a bitstream
 */
export class BitstreamRequestACopyPageComponent implements OnInit, OnDestroy {

  item$: Observable<Item>;

  canDownload$: Observable<boolean>;
  private subs: Subscription[] = [];
  requestCopyForm: FormGroup;

  item: Item;
  itemName: string;

  bitstream$: Observable<Bitstream>;
  bitstream: Bitstream;
  bitstreamName: string;

  constructor(private location: Location,
              private translateService: TranslateService,
              private route: ActivatedRoute,
              protected router: Router,
              private authorizationService: AuthorizationDataService,
              private auth: AuthService,
              private formBuilder: FormBuilder,
              private itemRequestDataService: ItemRequestDataService,
              private notificationsService: NotificationsService,
              private dsoNameService: DSONameService,
              private bitstreamService: BitstreamDataService,
  ) {
  }

  ngOnInit(): void {
    this.requestCopyForm = this.formBuilder.group({
      name: new FormControl('', {
        validators: [Validators.required],
      }),
      email: new FormControl('', {
        validators: [Validators.required,
        Validators.pattern('^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,4}$')]
      }),
      allfiles: new FormControl(''),
      message: new FormControl(''),
    });


    this.item$ = this.route.data.pipe(
      map((data) => data.dso),
      getFirstSucceededRemoteDataPayload()
    );

    this.subs.push(this.item$.subscribe((item) => {
      this.item = item;
      this.itemName = this.dsoNameService.getName(item);
    }));

    this.bitstream$ = this.route.queryParams.pipe(
      filter((params) => hasValue(params) && hasValue(params.bitstream)),
      switchMap((params) => this.bitstreamService.findById(params.bitstream)),
      getFirstSucceededRemoteDataPayload()
    );

    this.subs.push(this.bitstream$.subscribe((bitstream) => {
      this.bitstream = bitstream;
      this.bitstreamName = this.dsoNameService.getName(bitstream);
    }));

    this.canDownload$ = this.bitstream$.pipe(
      switchMap((bitstream) => this.authorizationService.isAuthorized(FeatureID.CanDownload, isNotEmpty(bitstream) ? bitstream.self : undefined))
    );
    const canRequestCopy$ = this.bitstream$.pipe(
      switchMap((bitstream) => this.authorizationService.isAuthorized(FeatureID.CanRequestACopy, isNotEmpty(bitstream) ? bitstream.self : undefined)),
    );

    this.subs.push(observableCombineLatest([this.canDownload$, canRequestCopy$]).subscribe(([canDownload, canRequestCopy]) => {
      if (!canDownload && !canRequestCopy) {
        this.router.navigateByUrl(getForbiddenRoute(), {skipLocationChange: true});
      }
    }));
    this.initValues();
  }

  get name() {
    return this.requestCopyForm.get('name');
  }

  get email() {
    return this.requestCopyForm.get('email');
  }

  get message() {
    return this.requestCopyForm.get('message');
  }

  get allfiles() {
    return this.requestCopyForm.get('allfiles');
  }

  /**
   * Initialise the form values based on the current user.
   */
  private initValues() {
    this.getCurrentUser().pipe(take(1)).subscribe((user) => {
      this.requestCopyForm.patchValue({allfiles: 'true'});
      if (hasValue(user)) {
        this.requestCopyForm.patchValue({name: user.name, email: user.email});
      }
    });
    this.bitstream$.pipe(take(1)).subscribe((bitstream) => {
      this.requestCopyForm.patchValue({allfiles: 'false'});
    });
  }

  /**
   * Retrieve the current user
   */
  private getCurrentUser(): Observable<EPerson> {
    return this.auth.isAuthenticated().pipe(
      switchMap((authenticated) => {
        if (authenticated) {
          return this.auth.getAuthenticatedUserFromStore();
        } else {
          return observableOf(undefined);
        }
      })
    );

  }

  /**
   * Submit the the form values as an item request to the server.
   * When the submission is successful, the user will be redirected to the item page and a success notification will be shown.
   * When the submission fails, the user will stay on the page and an error notification will be shown
   */
  onSubmit() {
    const itemRequest = new ItemRequest();
    if (hasValue(this.bitstream)) {
      itemRequest.bitstreamId = this.bitstream.uuid;
    }
    itemRequest.itemId = this.item.uuid;
    itemRequest.allfiles = this.allfiles.value;
    itemRequest.requestEmail = this.email.value;
    itemRequest.requestName = this.name.value;
    itemRequest.requestMessage = this.message.value;

    this.itemRequestDataService.requestACopy(itemRequest).pipe(
      getFirstCompletedRemoteData()
    ).subscribe((rd) => {
      if (rd.hasSucceeded) {
        this.notificationsService.success(this.translateService.get('bitstream-request-a-copy.submit.success'));
        this.navigateBack();
      } else {
        this.notificationsService.error(this.translateService.get('bitstream-request-a-copy.submit.error'));
      }
    });
  }

  ngOnDestroy(): void {
    if (hasValue(this.subs)) {
      this.subs.forEach((sub) => {
        if (hasValue(sub)) {
          sub.unsubscribe();
        }
      });
    }
  }

  /**
   * Navigates back to the user's previous location
   */
  navigateBack() {
    this.location.back();
  }

  getItemPath() {
    return [getItemPageRoute(this.item)];
  }

  /**
   * Retrieves the link to the bistream download page
   */
  getBitstreamLink() {
    return [getBitstreamDownloadRoute(this.bitstream)];
  }
}
