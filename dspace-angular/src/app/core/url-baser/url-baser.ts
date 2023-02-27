import { isEmpty } from '../../shared/empty.util';

/**
 * Extracts the base URL
 * from a URL with query parameters
 */
export class URLBaser {
  private original: string;

  /**
   * Creates a new URLBaser
   *
   * @param originalURL
   *      a string representing the original URL with possible query parameters
   */
  constructor(originalURL: string) {
    this.original = originalURL;
  }

  /**
   * Removes the query parameters from the original URL of this URLBaser
   *
   * @return {string}
   *      The base URL
   */
  toString(): string {
    if (isEmpty(this.original)) {
      return '';
    } else {
      const index = this.original.indexOf('?');
      if (index < 0) {
        return this.original;
      } else {
        return this.original.substring(0, index);
      }
    }
  }

}
