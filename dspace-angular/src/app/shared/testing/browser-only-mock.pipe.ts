import { Pipe, PipeTransform } from '@angular/core';

/**
 * Support dsBrowserOnly in unit tests.
 */
@Pipe({
  name: 'dsBrowserOnly'
})
export class BrowserOnlyMockPipe implements PipeTransform {
  transform(value: string): string | undefined {
    return value;
  }
}
