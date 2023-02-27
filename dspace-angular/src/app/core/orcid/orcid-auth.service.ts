import { Inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';

import { combineLatest, Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { AddOperation, RemoveOperation } from 'fast-json-patch';

import { ResearcherProfileDataService } from '../profile/researcher-profile-data.service';
import { Item } from '../shared/item.model';
import { isNotEmpty } from '../../shared/empty.util';
import { getFirstCompletedRemoteData, getFirstSucceededRemoteDataPayload } from '../shared/operators';
import { RemoteData } from '../data/remote-data';
import { ConfigurationProperty } from '../shared/configuration-property.model';
import { ConfigurationDataService } from '../data/configuration-data.service';
import { ResearcherProfile } from '../profile/model/researcher-profile.model';
import { URLCombiner } from '../url-combiner/url-combiner';
import { NativeWindowRef, NativeWindowService } from '../services/window.service';

@Injectable()
export class OrcidAuthService {

  constructor(
    @Inject(NativeWindowService) protected _window: NativeWindowRef,
    private configurationService: ConfigurationDataService,
    private researcherProfileService: ResearcherProfileDataService,
    private router: Router) {
  }

  /**
   * Check if the given item is linked to an ORCID profile.
   *
   * @param item the item to check
   * @returns the check result
   */
  public isLinkedToOrcid(item: Item): boolean {
    return item.hasMetadata('dspace.orcid.authenticated');
  }

  /**
   * Returns true if only the admin users can disconnect a researcher profile from ORCID.
   *
   * @returns the check result
   */
  public onlyAdminCanDisconnectProfileFromOrcid(): Observable<boolean> {
    return this.getOrcidDisconnectionAllowedUsersConfiguration().pipe(
      map((propertyRD: RemoteData<ConfigurationProperty>) => {
        return propertyRD.hasSucceeded && propertyRD.payload.values.map((value) => value.toLowerCase()).includes('only_admin');
      })
    );
  }

  /**
   * Returns true if the profile's owner can disconnect that profile from ORCID.
   *
   * @returns the check result
   */
  public ownerCanDisconnectProfileFromOrcid(): Observable<boolean> {
    return this.getOrcidDisconnectionAllowedUsersConfiguration().pipe(
      map((propertyRD: RemoteData<ConfigurationProperty>) => {
        return propertyRD.hasSucceeded && propertyRD.payload.values.map( (value) => value.toLowerCase()).includes('admin_and_owner');
      })
    );
  }

  /**
   * Perform a link operation to ORCID profile.
   *
   * @param person The person item related to the researcher profile
   * @param code The auth-code received from orcid
   */
  public linkOrcidByItem(person: Item, code: string): Observable<RemoteData<ResearcherProfile>> {
    const operations: AddOperation<string>[] = [{
      path: '/orcid',
      op: 'add',
      value: code
    }];

    return this.researcherProfileService.findById(person.firstMetadata('dspace.object.owner').authority).pipe(
      getFirstCompletedRemoteData(),
      switchMap((profileRD) => this.researcherProfileService.patch(profileRD.payload, operations)),
    );
  }

  /**
   * Perform unlink operation from ORCID profile.
   *
   * @param person The person item related to the researcher profile
   */
  public unlinkOrcidByItem(person: Item): Observable<RemoteData<ResearcherProfile>> {
    const operations: RemoveOperation[] = [{
      path:'/orcid',
      op:'remove'
    }];

    return this.researcherProfileService.findById(person.firstMetadata('dspace.object.owner').authority).pipe(
      getFirstCompletedRemoteData(),
      switchMap((profileRD) => this.researcherProfileService.patch(profileRD.payload, operations)),
    );
  }

  /**
   * Build and return the url to authenticate with orcid
   *
   * @param profile
   */
  public getOrcidAuthorizeUrl(profile: Item): Observable<string> {
    return combineLatest([
      this.configurationService.findByPropertyName('orcid.authorize-url').pipe(getFirstSucceededRemoteDataPayload()),
      this.configurationService.findByPropertyName('orcid.application-client-id').pipe(getFirstSucceededRemoteDataPayload()),
      this.configurationService.findByPropertyName('orcid.scope').pipe(getFirstSucceededRemoteDataPayload())]
    ).pipe(
      map(([authorizeUrl, clientId, scopes]) => {
        const redirectUri = new URLCombiner(this._window.nativeWindow.origin, encodeURIComponent(this.router.url.split('?')[0]));
        return authorizeUrl.values[0] + '?client_id=' + clientId.values[0]   + '&redirect_uri=' + redirectUri + '&response_type=code&scope='
          + scopes.values.join(' ');
      }));
  }

  /**
   * Return all orcid authorization scopes saved in the given item
   *
   * @param item
   */
  public getOrcidAuthorizationScopesByItem(item: Item): string[] {
    return isNotEmpty(item) ? item.allMetadataValues('dspace.orcid.scope') : [];
  }

  /**
   * Return all orcid authorization scopes available by configuration
   */
  public getOrcidAuthorizationScopes(): Observable<string[]> {
    return this.configurationService.findByPropertyName('orcid.scope').pipe(
      getFirstCompletedRemoteData(),
      map((propertyRD: RemoteData<ConfigurationProperty>) => propertyRD.hasSucceeded ? propertyRD.payload.values : [])
    );
  }

  private getOrcidDisconnectionAllowedUsersConfiguration(): Observable<RemoteData<ConfigurationProperty>> {
    return this.configurationService.findByPropertyName('orcid.disconnection.allowed-users').pipe(
      getFirstCompletedRemoteData()
    );
  }

}
