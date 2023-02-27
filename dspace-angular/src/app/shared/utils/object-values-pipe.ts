import { PipeTransform, Pipe } from '@angular/core';
import { isNotEmpty } from '../empty.util';

@Pipe({
  name: 'dsObjectValues',
  pure: true
})
/**
 * Pipe for parsing all values of an object to an array of values
 */
export class ObjectValuesPipe implements PipeTransform {

  /**
   * @param value An object
   * @returns {any} Array with all values of the input object
   */
  transform(value): any {
    const values = [];
    if (isNotEmpty(value)) {
      Object.values(value).forEach((v) => values.push(v));
    }
    return values;
  }
}
