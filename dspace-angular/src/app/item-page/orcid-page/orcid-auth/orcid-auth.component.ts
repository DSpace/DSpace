import { Component, EventEmitter, Inject, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';

import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { NativeWindowRef, NativeWindowService } from '../../../core/services/window.service';
import { Item } from '../../../core/shared/item.model';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { RemoteData } from '../../../core/data/remote-data';
import { ResearcherProfile } from '../../../core/profile/model/researcher-profile.model';
import { getFirstCompletedRemoteData } from '../../../core/shared/operators';
import { OrcidAuthService } from '../../../core/orcid/orcid-auth.service';

@Component({
  selector: 'ds-orcid-auth',
  templateUrl: './orcid-auth.component.html',
  styleUrls: ['./orcid-auth.component.scss']
})
export class OrcidAuthComponent implements OnInit, OnChanges {

  /**
   * The item for which showing the orcid settings
   */
  @Input() item: Item;

  /**
   * The list of exposed orcid authorization scopes for the orcid profile
   */
  profileAuthorizationScopes: BehaviorSubject<string[]> = new BehaviorSubject<string[]>([]);

  /**
   * The list of all orcid authorization scopes missing in the orcid profile
   */
  missingAuthorizationScopes: BehaviorSubject<string[]> = new BehaviorSubject<string[]>([]);

  /**
   * The list of all orcid authorization scopes available
   */
  orcidAuthorizationScopes: BehaviorSubject<string[]> = new BehaviorSubject<string[]>([]);

  /**
   * A boolean representing if unlink operation is processing
   */
  unlinkProcessing: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  /**
   * A boolean representing if orcid profile is linked
   */
  private isOrcidLinked$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  /**
   * A boolean representing if only admin can disconnect orcid profile
   */
  private onlyAdminCanDisconnectProfileFromOrcid$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  /**
   * A boolean representing if owner can disconnect orcid profile
   */
  private ownerCanDisconnectProfileFromOrcid$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  /**
   * An event emitted when orcid profile is unliked successfully
   */
  @Output() unlink: EventEmitter<void> = new EventEmitter<void>();

  constructor(
    private orcidAuthService: OrcidAuthService,
    private translateService: TranslateService,
    private notificationsService: NotificationsService,
    @Inject(NativeWindowService) private _window: NativeWindowRef,
  ) {
  }

  ngOnInit() {
    this.orcidAuthService.getOrcidAuthorizationScopes().subscribe((scopes: string[]) => {
      this.orcidAuthorizationScopes.next(scopes);
      this.initOrcidAuthSettings();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes.item.isFirstChange() && changes.item.currentValue !== changes.item.previousValue) {
      this.initOrcidAuthSettings();
    }
  }

  /**
   * Check if the list of exposed orcid authorization scopes for the orcid profile has values
   */
  hasOrcidAuthorizations(): Observable<boolean> {
    return this.profileAuthorizationScopes.asObservable().pipe(
      map((scopes: string[]) => scopes.length > 0)
    );
  }

  /**
   * Return the list of exposed orcid authorization scopes for the orcid profile
   */
  getOrcidAuthorizations(): Observable<string[]> {
    return this.profileAuthorizationScopes.asObservable();
  }

  /**
   * Check if the list of exposed orcid authorization scopes for the orcid profile has values
   */
  hasMissingOrcidAuthorizations(): Observable<boolean> {
    return this.missingAuthorizationScopes.asObservable().pipe(
      map((scopes: string[]) => scopes.length > 0)
    );
  }

  /**
   * Return the list of exposed orcid authorization scopes for the orcid profile
   */
  getMissingOrcidAuthorizations(): Observable<string[]> {
    return this.profileAuthorizationScopes.asObservable();
  }

  /**
   * Return a boolean representing if orcid profile is linked
   */
  isLinkedToOrcid(): Observable<boolean> {
    return this.isOrcidLinked$.asObservable();
  }

  getOrcidNotLinkedMessage(): Observable<string> {
    const orcid = this.item.firstMetadataValue('person.identifier.orcid');
    if (orcid) {
      return this.translateService.get('person.page.orcid.orcid-not-linked-message', { 'orcid': orcid });
    } else {
      return this.translateService.get('person.page.orcid.no-orcid-message');
    }
  }

  /**
   * Get label for a given orcid authorization scope
   *
   * @param scope
   */
  getAuthorizationDescription(scope: string) {
    return 'person.page.orcid.scope.' + scope.substring(1).replace('/', '-');
  }

  /**
   * Return a boolean representing if only admin can disconnect orcid profile
   */
  onlyAdminCanDisconnectProfileFromOrcid(): Observable<boolean> {
    return this.onlyAdminCanDisconnectProfileFromOrcid$.asObservable();
  }

  /**
   * Return a boolean representing if owner can disconnect orcid profile
   */
  ownerCanDisconnectProfileFromOrcid(): Observable<boolean> {
    return this.ownerCanDisconnectProfileFromOrcid$.asObservable();
  }

  /**
   * Link existing person profile with orcid
   */
  linkOrcid(): void {
    this.orcidAuthService.getOrcidAuthorizeUrl(this.item).subscribe((authorizeUrl) => {
      this._window.nativeWindow.location.href = authorizeUrl;
    });
  }

  /**
   * Unlink existing person profile from orcid
   */
  unlinkOrcid(): void {
    this.unlinkProcessing.next(true);
    this.orcidAuthService.unlinkOrcidByItem(this.item).pipe(
      getFirstCompletedRemoteData()
    ).subscribe((remoteData: RemoteData<ResearcherProfile>) => {
      this.unlinkProcessing.next(false);
      if (remoteData.isSuccess) {
        this.notificationsService.success(this.translateService.get('person.page.orcid.unlink.success'));
        this.unlink.emit();
      } else {
        this.notificationsService.error(this.translateService.get('person.page.orcid.unlink.error'));
      }
    });
  }

  /**
   * initialize all Orcid authentication settings
   * @private
   */
  private initOrcidAuthSettings(): void {

    this.setOrcidAuthorizationsFromItem();

    this.setMissingOrcidAuthorizations();

    this.orcidAuthService.onlyAdminCanDisconnectProfileFromOrcid().subscribe((result) => {
      this.onlyAdminCanDisconnectProfileFromOrcid$.next(result);
    });

    this.orcidAuthService.ownerCanDisconnectProfileFromOrcid().subscribe((result) => {
      this.ownerCanDisconnectProfileFromOrcid$.next(result);
    });

    this.isOrcidLinked$.next(this.orcidAuthService.isLinkedToOrcid(this.item));
  }

  private setMissingOrcidAuthorizations(): void {
    const profileScopes = this.orcidAuthService.getOrcidAuthorizationScopesByItem(this.item);
    const orcidScopes = this.orcidAuthorizationScopes.value;
    const missingScopes = orcidScopes.filter((scope) => !profileScopes.includes(scope));

    this.missingAuthorizationScopes.next(missingScopes);
  }

  private setOrcidAuthorizationsFromItem(): void {
    this.profileAuthorizationScopes.next(this.orcidAuthService.getOrcidAuthorizationScopesByItem(this.item));
  }

}
