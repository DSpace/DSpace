import { Pipe, PipeTransform } from '@angular/core';
import { hasValue } from '../empty.util';

/**
 * Pipe to truncate a value in Angular. (Take a substring, starting at 0)
 * Default value: 10
 */
@Pipe({
  name: 'dsTruncate'
})
export class TruncatePipe implements PipeTransform {

  /**
   *
   */
  transform(value: string, args: string[]): string {
    if (hasValue(value)) {
      const limit = (args && args.length > 0) ? parseInt(args[0], 10) : 10; // 10 as default truncate value
      return value.length > limit ? value.substring(0, limit) + '...' : value;
    } else {
      return value;
    }
  }

}
