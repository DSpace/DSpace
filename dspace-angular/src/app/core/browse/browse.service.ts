import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map, startWith } from 'rxjs/operators';
import { hasValue, hasValueOperator, isEmpty, isNotEmpty } from '../../shared/empty.util';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { PaginatedList } from '../data/paginated-list.model';
import { RemoteData } from '../data/remote-data';
import { RequestService } from '../data/request.service';
import { BrowseDefinition } from '../shared/browse-definition.model';
import { BrowseEntry } from '../shared/browse-entry.model';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { Item } from '../shared/item.model';
import {
  getBrowseDefinitionLinks,
  getFirstOccurrence,
  getRemoteDataPayload,
  getFirstSucceededRemoteData,
  getPaginatedListPayload
} from '../shared/operators';
import { URLCombiner } from '../url-combiner/url-combiner';
import { BrowseEntrySearchOptions } from './browse-entry-search-options.model';
import { HrefOnlyDataService } from '../data/href-only-data.service';
import { followLink, FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { BrowseDefinitionDataService } from './browse-definition-data.service';


export const BROWSE_LINKS_TO_FOLLOW: FollowLinkConfig<BrowseEntry | Item>[] = [
  followLink('thumbnail')
];

/**
 * The service handling all browse requests
 */
@Injectable()
export class BrowseService {
  protected linkPath = 'browses';

  public static toSearchKeyArray(metadataKey: string): string[] {
    const keyParts = metadataKey.split('.');
    const searchFor = [];
    searchFor.push('*');
    for (let i = 0; i < keyParts.length - 1; i++) {
      const prevParts = keyParts.slice(0, i + 1);
      const nextPart = [...prevParts, '*'].join('.');
      searchFor.push(nextPart);
    }
    searchFor.push(metadataKey);
    return searchFor;
  }

  constructor(
    protected requestService: RequestService,
    protected halService: HALEndpointService,
    private browseDefinitionDataService: BrowseDefinitionDataService,
    private hrefOnlyDataService: HrefOnlyDataService,
    private rdb: RemoteDataBuildService,
  ) {
  }

  /**
   * Get all BrowseDefinitions
   */
  getBrowseDefinitions(): Observable<RemoteData<PaginatedList<BrowseDefinition>>> {
    // TODO properly support pagination
    return this.browseDefinitionDataService.findAll({ elementsPerPage: 9999 }).pipe(
      getFirstSucceededRemoteData(),
    );
  }

  /**
   * Get all BrowseEntries filtered or modified by BrowseEntrySearchOptions
   * @param options
   */
  getBrowseEntriesFor(options: BrowseEntrySearchOptions): Observable<RemoteData<PaginatedList<BrowseEntry>>> {
    const href$ = this.getBrowseDefinitions().pipe(
      getBrowseDefinitionLinks(options.metadataDefinition),
      hasValueOperator(),
      map((_links: any) => {
        const entriesLink = _links.entries.href || _links.entries;
        return entriesLink;
      }),
      hasValueOperator(),
      map((href: string) => {
        // TODO nearly identical to PaginatedSearchOptions => refactor
        const args = [];
        if (isNotEmpty(options.scope)) {
          args.push(`scope=${options.scope}`);
        }
        if (isNotEmpty(options.sort)) {
          args.push(`sort=${options.sort.field},${options.sort.direction}`);
        }
        if (isNotEmpty(options.pagination)) {
          args.push(`page=${options.pagination.currentPage - 1}`);
          args.push(`size=${options.pagination.pageSize}`);
        }
        if (isNotEmpty(options.startsWith)) {
          args.push(`startsWith=${options.startsWith}`);
        }
        if (isNotEmpty(args)) {
          href = new URLCombiner(href, `?${args.join('&')}`).toString();
        }
        return href;
      })
    );
    if (options.fetchThumbnail ) {
      return this.hrefOnlyDataService.findListByHref<BrowseEntry>(href$, {}, null, null, ...BROWSE_LINKS_TO_FOLLOW);
    }
    return this.hrefOnlyDataService.findListByHref<BrowseEntry>(href$);
  }

  /**
   * Get all items linked to a certain metadata value
   * @param {string} filterValue      metadata value to filter by (e.g. author's name)
   * @param options                   Options to narrow down your search
   * @returns {Observable<RemoteData<PaginatedList<Item>>>}
   */
  getBrowseItemsFor(filterValue: string, filterAuthority: string, options: BrowseEntrySearchOptions): Observable<RemoteData<PaginatedList<Item>>> {
    const href$ = this.getBrowseDefinitions().pipe(
      getBrowseDefinitionLinks(options.metadataDefinition),
      hasValueOperator(),
      map((_links: any) => {
        const itemsLink = _links.items.href || _links.items;
        return itemsLink;
      }),
      hasValueOperator(),
      map((href: string) => {
        const args = [];
        if (isNotEmpty(options.scope)) {
          args.push(`scope=${options.scope}`);
        }
        if (isNotEmpty(options.sort)) {
          args.push(`sort=${options.sort.field},${options.sort.direction}`);
        }
        if (isNotEmpty(options.pagination)) {
          args.push(`page=${options.pagination.currentPage - 1}`);
          args.push(`size=${options.pagination.pageSize}`);
        }
        if (isNotEmpty(options.startsWith)) {
          args.push(`startsWith=${options.startsWith}`);
        }
        if (isNotEmpty(filterValue)) {
          args.push(`filterValue=${encodeURIComponent(filterValue)}`);
        }
        if (isNotEmpty(filterAuthority)) {
          args.push(`filterAuthority=${encodeURIComponent(filterAuthority)}`);
        }
        if (isNotEmpty(args)) {
          href = new URLCombiner(href, `?${args.join('&')}`).toString();
        }
        return href;
      }),
    );
    if (options.fetchThumbnail) {
      return this.hrefOnlyDataService.findListByHref<Item>(href$, {}, null, null, ...BROWSE_LINKS_TO_FOLLOW);
    }
    return this.hrefOnlyDataService.findListByHref<Item>(href$);
  }

  /**
   * Get the first item for a metadata definition in an optional scope
   * @param definition
   * @param scope
   */
  getFirstItemFor(definition: string, scope?: string): Observable<RemoteData<Item>> {
    const href$ = this.getBrowseDefinitions().pipe(
      getBrowseDefinitionLinks(definition),
      hasValueOperator(),
      map((_links: any) => {
        const itemsLink = _links.items.href || _links.items;
        return itemsLink;
      }),
      hasValueOperator(),
      map((href: string) => {
        const args = [];
        if (hasValue(scope)) {
          args.push(`scope=${scope}`);
        }
        args.push('page=0');
        args.push('size=1');
        if (isNotEmpty(args)) {
          href = new URLCombiner(href, `?${args.join('&')}`).toString();
        }
        return href;
      })
    );

    return this.hrefOnlyDataService.findListByHref<Item>(href$).pipe(
      getFirstSucceededRemoteData(),
      getFirstOccurrence()
    );

  }

  /**
   * Get the previous page of items using the paginated list's prev link
   * @param items
   */
  getPrevBrowseItems(items: RemoteData<PaginatedList<Item>>): Observable<RemoteData<PaginatedList<Item>>> {
    return this.hrefOnlyDataService.findListByHref<Item>(items.payload.prev);
  }

  /**
   * Get the next page of items using the paginated list's next link
   * @param items
   */
  getNextBrowseItems(items: RemoteData<PaginatedList<Item>>): Observable<RemoteData<PaginatedList<Item>>> {
    return this.hrefOnlyDataService.findListByHref<Item>(items.payload.next);
  }

  /**
   * Get the previous page of browse-entries using the paginated list's prev link
   * @param entries
   */
  getPrevBrowseEntries(entries: RemoteData<PaginatedList<BrowseEntry>>): Observable<RemoteData<PaginatedList<BrowseEntry>>> {
    return this.hrefOnlyDataService.findListByHref<BrowseEntry>(entries.payload.prev);
  }

  /**
   * Get the next page of browse-entries using the paginated list's next link
   * @param entries
   */
  getNextBrowseEntries(entries: RemoteData<PaginatedList<BrowseEntry>>): Observable<RemoteData<PaginatedList<BrowseEntry>>> {
    return this.hrefOnlyDataService.findListByHref<BrowseEntry>(entries.payload.next);
  }

  /**
   * Get the browse URL by providing a metadatum key and linkPath
   * @param metadatumKey
   * @param linkPath
   */
  getBrowseURLFor(metadataKey: string, linkPath: string): Observable<string> {
    const searchKeyArray = BrowseService.toSearchKeyArray(metadataKey);
    return this.getBrowseDefinitions().pipe(
      getRemoteDataPayload(),
      getPaginatedListPayload(),
      map((browseDefinitions: BrowseDefinition[]) => browseDefinitions
        .find((def: BrowseDefinition) => {
          const matchingKeys = def.metadataKeys.find((key: string) => searchKeyArray.indexOf(key) >= 0);
          return isNotEmpty(matchingKeys);
        })
      ),
      map((def: BrowseDefinition) => {
        if (isEmpty(def) || isEmpty(def._links) || isEmpty(def._links[linkPath])) {
          throw new Error(`A browse endpoint for ${linkPath} on ${metadataKey} isn't configured`);
        } else {
          return def._links[linkPath] || def._links[linkPath].href;
        }
      }),
      startWith(undefined),
      distinctUntilChanged()
    );
  }

}
