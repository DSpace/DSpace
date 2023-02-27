import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { first, map } from 'rxjs/operators';
import { Community } from '../../../core/shared/community.model';
import { getRemoteDataPayload, getFirstSucceededRemoteData } from '../../../core/shared/operators';
import { RemoteData } from '../../../core/data/remote-data';
import { HALLink } from '../../../core/shared/hal-link.model';

/**
 * Component for managing a community's roles
 */
@Component({
  selector: 'ds-community-roles',
  templateUrl: './community-roles.component.html',
})
export class CommunityRolesComponent implements OnInit {

  dsoRD$: Observable<RemoteData<Community>>;

  /**
   * The different roles for the community, as an observable.
   */
  comcolRoles$: Observable<HALLink[]>;

  /**
   * The community to manage, as an observable.
   */
  community$: Observable<Community>;

  constructor(
    protected route: ActivatedRoute,
  ) {
  }

  ngOnInit(): void {
    this.dsoRD$ = this.route.parent.data.pipe(
      first(),
      map((data) => data.dso),
    );

    this.community$ = this.dsoRD$.pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
    );

    /**
     * The different roles for the community.
     */
    this.comcolRoles$ = this.community$.pipe(
      map((community) => [
        {
          name: 'community-admin',
          href: community._links.adminGroup.href,
        },
      ]),
    );
  }
}
