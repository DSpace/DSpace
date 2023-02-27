import { PipeTransform, Pipe } from '@angular/core';

@Pipe({name: 'dsObjectKeys'})
/**
 * Pipe for parsing all keys of an object to an array of key-value pairs
 */
export class ObjectKeysPipe implements PipeTransform {

  /**
   * @param value An object
   * @returns {any} Array with all keys the input object
   */
  transform(value): any {
    const keys = [];
    Object.keys(value).forEach((k) => keys.push(k));
    return keys;
  }
}
