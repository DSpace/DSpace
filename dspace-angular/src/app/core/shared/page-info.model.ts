import { autoserialize, autoserializeAs, deserialize } from 'cerialize';
import { hasValue } from '../../shared/empty.util';
import { HALLink } from './hal-link.model';
import { HALResource } from './hal-resource.model';

/**
 * Represents the state of a paginated response
 */
export class PageInfo implements HALResource {

  /**
   * The number of elements on a page
   */
  @autoserializeAs(Number, 'size')
  elementsPerPage: number;

  /**
   * The total number of elements in the entire set
   */
  @autoserialize
  totalElements: number;

  /**
   * The total number of pages
   */
  @autoserialize
  totalPages: number;

  /**
   * The number of the current page, zero-based
   */
  @autoserializeAs(Number, 'number')
  currentPage: number;

  /**
   * The {@link HALLink}s for this PageInfo
   */
  @deserialize
  _links: {
    first: HALLink;
    prev: HALLink;
    next: HALLink;
    last: HALLink;
    self: HALLink;
  };

  constructor(
    options?: {
      elementsPerPage: number,
      totalElements: number,
      totalPages: number,
      currentPage: number
    }
  ) {
    if (hasValue(options)) {
      this.elementsPerPage = options.elementsPerPage;
      this.totalElements = options.totalElements;
      this.totalPages = options.totalPages;
      this.currentPage = options.currentPage;
    }
  }

  get self() {
    return this._links.self.href;
  }

  get last(): string {
    if (hasValue(this._links) && hasValue(this._links.last)) {
      return this._links.last.href;
    } else {
      return undefined;
    }
  }

  get next(): string {
    if (hasValue(this._links) && hasValue(this._links.next)) {
      return this._links.next.href;
    } else {
      return undefined;
    }
  }

  get prev(): string {
    if (hasValue(this._links) && hasValue(this._links.prev)) {
      return this._links.prev.href;
    } else {
      return undefined;
    }
  }

  get first(): string {
    if (hasValue(this._links) && hasValue(this._links.first)) {
      return this._links.first.href;
    } else {
      return undefined;
    }
  }

}
