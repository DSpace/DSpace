import { Inject, Pipe, PipeTransform, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * A pipe that only returns its input when run in the browser.
 * Used to distinguish client-side rendered content from server-side rendered content.
 *
 * When used with attributes as in
 * ```
 *   [attr.data-test]="'something' | dsBrowserOnly"
 * ```
 * the server-side rendered HTML will not contain the `data-test` attribute.
 * When rendered client-side, the HTML will contain `data-test="something"`
 *
 * This can be useful for end-to-end testing elements that need JS (that isn't included in SSR HTML) to function:
 * By depending on `dsBrowserOnly` attributes in tests we can make sure we wait
 * until such components are fully interactive before trying to interact with them.
 */
@Pipe({
  name: 'dsBrowserOnly'
})
export class BrowserOnlyPipe implements PipeTransform {
  constructor(
    @Inject(PLATFORM_ID) private platformID: any,
  ) {
  }

  transform(value: string): string | undefined {
    if (isPlatformBrowser((this.platformID))) {
      return value;
    } else {
      return undefined;
    }
  }
}
