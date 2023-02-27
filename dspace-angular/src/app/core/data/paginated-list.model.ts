import { PageInfo } from '../shared/page-info.model';
import { hasValue, isEmpty, hasNoValue, isUndefined } from '../../shared/empty.util';
import { HALResource } from '../shared/hal-resource.model';
import { HALLink } from '../shared/hal-link.model';
import { typedObject } from '../cache/builders/build-decorators';
import { PAGINATED_LIST } from './paginated-list.resource-type';
import { ResourceType } from '../shared/resource-type';
import { excludeFromEquals } from '../utilities/equals.decorators';
import { autoserialize, deserialize } from 'cerialize';
import { CacheableObject } from '../cache/cacheable-object.model';

/**
 * Factory function for a paginated list
 *
 * @param pageInfo    The PageInfo for the new PaginatedList
 * @param page        The list of objects on the current page
 * @param normalized  Set to true if the list should only contain the links to the page objects,
 *                    not the objects themselves
 * @param _links      Optional HALLinks to attach to the new PaginatedList
 */
export const buildPaginatedList = <T>(pageInfo: PageInfo, page: T[], normalized = false, _links?: { [k: string]: HALLink | HALLink[] }): PaginatedList<T> => {
  const result = new PaginatedList<T>();

  if (hasNoValue(pageInfo)) {
    pageInfo = new PageInfo();
  }

  result.pageInfo = pageInfo;

  let pageLinks: HALLink[];
  if (isEmpty(page)) {
    pageLinks = [];
  } else {
    pageLinks = page.map((element: any) => {
      if (hasValue(element) && hasValue(element._links) && hasValue(element._links.self)) {
        return (element as HALResource)._links.self;
      } else {
        return null;
      }
    });
    // if none of the objects in page are HALResources, don't set a page link
    if (pageLinks.every((link: HALLink) => hasNoValue(link))) {
      pageLinks = undefined;
    }
  }

  result._links = Object.assign({}, _links, pageInfo._links, {
    page: pageLinks
  });

  if (!normalized || isUndefined(pageLinks)) {
    result.page = page;
  }

  return result;
};

@typedObject
export class PaginatedList<T> extends CacheableObject {

  static type = PAGINATED_LIST;

  /**
   * The type of the list
   */
  @excludeFromEquals
  type = PAGINATED_LIST;

  /**
   * The type of objects in the list
   */
  @autoserialize
  objectType?: ResourceType;

  /**
   * The list of objects that represents the current page
   */
  page?: T[];

  /**
   * the {@link PageInfo} object
   */
  @autoserialize
  pageInfo?: PageInfo;

  /**
   * The {@link HALLink}s for this PaginatedList
   */
  @deserialize
  _links: {
    self: HALLink;
    page: HALLink[];
    first?: HALLink;
    prev?: HALLink;
    next?: HALLink;
    last?: HALLink;
  };

  get elementsPerPage(): number {
    if (hasValue(this.pageInfo) && hasValue(this.pageInfo.elementsPerPage)) {
      return this.pageInfo.elementsPerPage;
    }
    return this.getPageLength();
  }

  set elementsPerPage(value: number) {
    this.pageInfo.elementsPerPage = value;
  }

  get totalElements(): number {
    if (hasValue(this.pageInfo) && hasValue(this.pageInfo.totalElements)) {
      return this.pageInfo.totalElements;
    }
    return this.getPageLength();
  }

  set totalElements(value: number) {
    this.pageInfo.totalElements = value;
  }

  get totalPages(): number {
    if (hasValue(this.pageInfo) && hasValue(this.pageInfo.totalPages)) {
      return this.pageInfo.totalPages;
    }
    return 1;
  }

  set totalPages(value: number) {
    this.pageInfo.totalPages = value;
  }

  get currentPage(): number {
    if (hasValue(this.pageInfo) && hasValue(this.pageInfo.currentPage)) {
      return this.pageInfo.currentPage;
    }
    return 1;
  }

  set currentPage(value: number) {
    this.pageInfo.currentPage = value;
  }

  get first(): string {
    if (hasValue(this._links.first) && hasValue(this._links.first.href)) {
      return this._links.first.href;
    }
  }

  set first(first: string) {
    this._links.first = { href: first };
    this.pageInfo._links.first = { href: first };
  }

  get prev(): string {
    if (hasValue(this._links.prev) && hasValue(this._links.prev.href)) {
      return this._links.prev.href;
    }
  }

  set prev(prev: string) {
    this._links.prev = { href: prev };
    this.pageInfo._links.prev = { href: prev };
  }

  get next(): string {
    if (hasValue(this._links.next) && hasValue(this._links.next.href)) {
      return this._links.next.href;
    }
  }

  set next(next: string) {
    this._links.next = { href: next };
    this.pageInfo._links.next = { href: next };
  }

  get last(): string {
    if (hasValue(this._links.last) && hasValue(this._links.last.href)) {
      return this._links.last.href;
    }
  }

  set last(last: string) {
    this._links.last = { href: last };
    this.pageInfo._links.last = { href: last };
  }

  get self(): string {
    return this._links.self.href;
  }

  set self(self: string) {
    this._links.self = { href: self };
    this.pageInfo._links.self = { href: self };
  }

  protected getPageLength() {
    return (Array.isArray(this.page)) ? this.page.length : 0;
  }
}
