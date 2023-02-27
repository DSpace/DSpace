import { BehaviorSubject, combineLatest as observableCombineLatest, Observable } from 'rxjs';

import { ChangeDetectionStrategy, Component, Inject, OnInit, ViewChild } from '@angular/core';
import { DSONameService } from '../../core/breadcrumbs/dso-name.service';
import { fadeIn, fadeInOut } from '../../shared/animations/fade';
import { ActivatedRoute, Router } from '@angular/router';
import { RemoteData } from '../../core/data/remote-data';
import { Collection } from '../../core/shared/collection.model';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { map, startWith, switchMap, take } from 'rxjs/operators';
import {
  getAllSucceededRemoteData,
  getFirstCompletedRemoteData,
  getFirstSucceededRemoteData,
  getRemoteDataPayload,
  toDSpaceObjectListRD
} from '../../core/shared/operators';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { DSpaceObjectType } from '../../core/shared/dspace-object-type.model';
import { SortDirection, SortOptions } from '../../core/cache/models/sort-options.model';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { ItemDataService } from '../../core/data/item-data.service';
import { TranslateService } from '@ngx-translate/core';
import { CollectionDataService } from '../../core/data/collection-data.service';
import { isNotEmpty } from '../../shared/empty.util';
import { SEARCH_CONFIG_SERVICE } from '../../my-dspace-page/my-dspace-page.component';
import { SearchConfigurationService } from '../../core/shared/search/search-configuration.service';
import { PaginatedSearchOptions } from '../../shared/search/models/paginated-search-options.model';
import { SearchService } from '../../core/shared/search/search.service';
import { followLink } from '../../shared/utils/follow-link-config.model';
import { NoContent } from '../../core/shared/NoContent.model';
import { FeatureID } from '../../core/data/feature-authorization/feature-id';

@Component({
  selector: 'ds-collection-item-mapper',
  styleUrls: ['./collection-item-mapper.component.scss'],
  templateUrl: './collection-item-mapper.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    fadeIn,
    fadeInOut
  ],
  providers: [
    {
      provide: SEARCH_CONFIG_SERVICE,
      useClass: SearchConfigurationService
    }
  ]
})
/**
 * Component used to map items to a collection
 */
export class CollectionItemMapperComponent implements OnInit {

  FeatureIds = FeatureID;

  /**
   * A view on the tabset element
   * Used to switch tabs programmatically
   */
  @ViewChild('tabs', {static: false}) tabs;

  /**
   * The collection to map items to
   */
  collectionRD$: Observable<RemoteData<Collection>>;
  collectionName$: Observable<string>;

  /**
   * Search options
   */
  searchOptions$: Observable<PaginatedSearchOptions>;

  /**
   * List of items to show under the "Browse" tab
   * Items inside the collection
   */
  collectionItemsRD$: Observable<RemoteData<PaginatedList<DSpaceObject>>>;

  /**
   * List of items to show under the "Map" tab
   * Items outside the collection
   */
  mappedItemsRD$: Observable<RemoteData<PaginatedList<DSpaceObject>>>;

  /**
   * Sort on title ASC by default
   * @type {SortOptions}
   */
  defaultSortOptions: SortOptions = new SortOptions('dc.title', SortDirection.ASC);

  /**
   * Firing this observable (shouldUpdate$.next(true)) forces the two lists to reload themselves
   * Usually fired after the lists their cache is cleared (to force a new request to the REST API)
   */
  shouldUpdate$: BehaviorSubject<boolean>;

  /**
   * Track whether at least one search has been performed or not
   * As soon as at least one search has been performed, we display the search results
   */
  performedSearch = false;

  constructor(private route: ActivatedRoute,
              private router: Router,
              @Inject(SEARCH_CONFIG_SERVICE) private searchConfigService: SearchConfigurationService,
              private searchService: SearchService,
              private notificationsService: NotificationsService,
              private itemDataService: ItemDataService,
              private collectionDataService: CollectionDataService,
              private translateService: TranslateService,
              private dsoNameService: DSONameService) {
  }

  ngOnInit(): void {
    this.collectionRD$ = this.route.parent.data.pipe(
      map((data) => data.dso as RemoteData<Collection>),
      getFirstSucceededRemoteData()
    );

    this.collectionName$ = this.collectionRD$.pipe(
      map((rd: RemoteData<Collection>) => {
        return this.dsoNameService.getName(rd.payload);
      })
    );
    this.searchOptions$ = this.searchConfigService.paginatedSearchOptions;
    this.loadItemLists();
  }

  /**
   * Load collectionItemsRD$ with a fixed scope to only obtain the items this collection owns
   * Load mappedItemsRD$ to only obtain items this collection doesn't own
   */
  loadItemLists() {
    this.shouldUpdate$ = new BehaviorSubject<boolean>(true);
    const collectionAndOptions$ = observableCombineLatest(
      this.collectionRD$,
      this.searchOptions$,
      this.shouldUpdate$
    );
    this.collectionItemsRD$ = collectionAndOptions$.pipe(
      switchMap(([collectionRD, options, shouldUpdate]) => {
        if (shouldUpdate === true) {
          this.shouldUpdate$.next(false);
        }
        return this.itemDataService.findListByHref(collectionRD.payload._links.mappedItems.href, Object.assign(options, {
          sort: this.defaultSortOptions
        }),!shouldUpdate, false, followLink('owningCollection')).pipe(
          getAllSucceededRemoteData()
        );
      })
    );
    this.mappedItemsRD$ = collectionAndOptions$.pipe(
      switchMap(([collectionRD, options, shouldUpdate]) => {
        return this.searchService.search(Object.assign(new PaginatedSearchOptions(options), {
          query: this.buildQuery(collectionRD.payload.id, options.query),
          scope: undefined,
          dsoTypes: [DSpaceObjectType.ITEM],
          sort: this.defaultSortOptions
        }), 10000).pipe(
          toDSpaceObjectListRD(),
          startWith(undefined)
        );
      })
    );
  }

  /**
   * Map/Unmap the selected items to the collection and display notifications
   * @param ids         The list of item UUID's to map/unmap to the collection
   * @param remove      Whether or not it's supposed to remove mappings
   */
  mapItems(ids: string[], remove?: boolean) {
    const responses$ = this.collectionRD$.pipe(
      getFirstSucceededRemoteData(),
      map((collectionRD: RemoteData<Collection>) => collectionRD.payload),
      switchMap((collection: Collection) =>
        observableCombineLatest(ids.map((id: string) => {
            if (remove) {
              return this.itemDataService.removeMappingFromCollection(id, collection.id).pipe(
                getFirstCompletedRemoteData()
              );
            } else {
              return this.itemDataService.mapToCollection(id, collection._links.self.href).pipe(
                getFirstCompletedRemoteData()
              );
            }
          }
        ))
      )
    );

    this.showNotifications(responses$, remove);
  }

  /**
   * Display notifications
   * @param {Observable<RestResponse[]>} responses$   The responses after adding/removing a mapping
   * @param {boolean} remove                          Whether or not the goal was to remove mappings
   */
  private showNotifications(responses$: Observable<RemoteData<NoContent>[]>, remove?: boolean) {
    const messageInsertion = remove ? 'unmap' : 'map';

    responses$.subscribe((responses: RemoteData<NoContent>[]) => {
      const successful = responses.filter((response: RemoteData<any>) => response.hasSucceeded);
      const unsuccessful = responses.filter((response: RemoteData<any>) => response.hasFailed);
      if (successful.length > 0) {
        const successMessages = observableCombineLatest(
          this.translateService.get(`collection.edit.item-mapper.notifications.${messageInsertion}.success.head`),
          this.translateService.get(`collection.edit.item-mapper.notifications.${messageInsertion}.success.content`, { amount: successful.length })
        );

        successMessages.subscribe(([head, content]) => {
          this.notificationsService.success(head, content);
        });
        this.shouldUpdate$.next(true);
      }
      if (unsuccessful.length > 0) {
        const unsuccessMessages = observableCombineLatest(
          this.translateService.get(`collection.edit.item-mapper.notifications.${messageInsertion}.error.head`),
          this.translateService.get(`collection.edit.item-mapper.notifications.${messageInsertion}.error.content`, { amount: unsuccessful.length })
        );

        unsuccessMessages.subscribe(([head, content]) => {
          this.notificationsService.error(head, content);
        });
      }
      this.switchToFirstTab();
    });
  }

  /**
   * Clear url parameters on tab change (temporary fix until pagination is improved)
   * @param event
   */
  tabChange(event) {
    this.performedSearch = false;
    this.router.navigateByUrl(this.getCurrentUrl());
  }

  /**
   * Get current url without parameters
   * @returns {string}
   */
  getCurrentUrl(): string {
    if (this.router.url.indexOf('?') > -1) {
      return this.router.url.substring(0, this.router.url.indexOf('?'));
    }
    return this.router.url;
  }

  /**
   * Build a query where items that are already mapped to a collection are excluded from
   * @param collectionId    The collection's UUID
   * @param query           The query to add to it
   */
  buildQuery(collectionId: string, query: string): string {
    const excludeColQuery = `-location.coll:\"${collectionId}\"`;
    if (isNotEmpty(query)) {
      return `${excludeColQuery} AND ${query}`;
    } else {
      return excludeColQuery;
    }
  }

  /**
   * Switch the view to focus on the first tab
   */
  switchToFirstTab() {
    this.tabs.select('browseTab');
  }

  /**
   * When a cancel event is fired, return to the collection page
   */
  onCancel() {
    this.collectionRD$.pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
      take(1)
    ).subscribe((collection: Collection) => {
      this.router.navigate(['/collections/', collection.id]);
    });
  }

}
