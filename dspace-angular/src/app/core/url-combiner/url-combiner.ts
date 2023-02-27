import { isEmpty } from '../../shared/empty.util';

/**
 * Combines a variable number of strings representing parts
 * of a URL in to a single, normalized URL
 */
export class URLCombiner {
  private parts: string[];

  /**
   * Creates a new URLCombiner
   *
   * @param parts
   *      a variable number of strings representing parts of a URL
   */
  constructor(...parts: string[]) {
    // can't do this in the constructor signature,
    // because of the spread operator
    this.parts = parts;
  }

  /**
   * Combines the parts of this URLCombiner in to a single,
   * normalized URL
   *
   * e.g.  new URLCombiner('http:/foo.com/', '/bar', 'id', '5').toString()
   * returns: http://foo.com/bar/id/5
   *
   * @return {string}
   *      The combined URL
   */
  toString(): string {
    if (isEmpty(this.parts)) {
      return '';
    } else {
      let url = this.parts.join('/');

      // make sure protocol is followed by two slashes
      url = url.replace(/:\//g, '://');

      // remove consecutive slashes
      url = url.replace(/([^:\s])\/+/g, '$1/');

      // remove trailing slash
      url = url.replace(/\/($|\?|&|#[^!])/g, '$1');

      // replace ? in parameters with &
      url = url.replace(/(\?.+)\?/g, '$1&');

      return url;
    }
  }

}
