import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'dsKeys' })
/**
 * Pipe for parsing all values of an enumeration to an array of key-value pairs
 */
export class EnumKeysPipe implements PipeTransform {

  /**
   * @param value An enumeration
   * @returns {any} Array with all keys and values of the input enumeration
   */
  transform(value): any {
    const keys = [];
    for (const enumMember in value) {
      if (!isNaN(parseInt(enumMember, 10))) {
        keys.push({ key: +enumMember, value: value[enumMember] });
      } else {
        keys.push({ key: enumMember, value: value[enumMember] });
      }
    }
    return keys;
  }
}
