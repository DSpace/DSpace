import { Injectable } from '@angular/core';
import { isNotEmpty, isEmpty } from './empty.util';

const PREFIX_REGEX = /handle\/([^\/]+\/[^\/]+)$/;
const NO_PREFIX_REGEX = /^([^\/]+\/[^\/]+)$/;

@Injectable({
  providedIn: 'root'
})
export class HandleService {


  /**
   * Turns a handle string into the default 123456789/12345 format
   *
   * @param handle the input handle
   *
   * normalizeHandle('123456789/123456')                                 // '123456789/123456'
   * normalizeHandle('12.3456.789/123456')                               // '12.3456.789/123456'
   * normalizeHandle('https://hdl.handle.net/handle/123456789/123456')   // '123456789/123456'
   * normalizeHandle('https://rest.api/server/handle/123456789/123456')  // '123456789/123456'
   * normalizeHandle('https://rest.api/server/handle/123456789')         // null
   */
  normalizeHandle(handle: string): string {
    let matches: string[];
    if (isNotEmpty(handle)) {
      matches = handle.match(PREFIX_REGEX);
    }

    if (isEmpty(matches) || matches.length < 2) {
      matches = handle.match(NO_PREFIX_REGEX);
    }

    if (isEmpty(matches) || matches.length < 2) {
      return null;
    } else {
      return matches[1];
    }
  }

}
