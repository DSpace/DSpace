import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { first, map } from 'rxjs/operators';
import { RemoteData } from '../../../core/data/remote-data';
import { Collection } from '../../../core/shared/collection.model';
import { getRemoteDataPayload, getFirstSucceededRemoteData } from '../../../core/shared/operators';
import { HALLink } from '../../../core/shared/hal-link.model';
import { hasValue } from '../../../shared/empty.util';

/**
 * Component for managing a collection's roles
 */
@Component({
  selector: 'ds-collection-roles',
  templateUrl: './collection-roles.component.html',
})
export class CollectionRolesComponent implements OnInit {

  dsoRD$: Observable<RemoteData<Collection>>;

  /**
   * The different roles for the collection, as an observable.
   */
  comcolRoles$: Observable<HALLink[]>;

  /**
   * The collection to manage, as an observable.
   */
  collection$: Observable<Collection>;

  constructor(
    protected route: ActivatedRoute,
  ) {
  }

  ngOnInit(): void {
    this.dsoRD$ = this.route.parent.data.pipe(
      first(),
      map((data) => data.dso),
    );

    this.collection$ = this.dsoRD$.pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
    );

    this.comcolRoles$ = this.collection$.pipe(
      map((collection) => {
        let workflowGroups: HALLink[] | HALLink = hasValue(collection._links.workflowGroups) ? collection._links.workflowGroups : [];
        if (!Array.isArray(workflowGroups)) {
          workflowGroups = [workflowGroups];
        }
        return [
          {
            name: 'collection-admin',
            href: collection._links.adminGroup.href,
          },
          {
            name: 'submitters',
            href: collection._links.submittersGroup.href,
          },
          {
            name: 'item_read',
            href: collection._links.itemReadGroup.href,
          },
          {
            name: 'bitstream_read',
            href: collection._links.bitstreamReadGroup.href,
          },
          ...workflowGroups,
        ];
      }),
    );
  }
}
