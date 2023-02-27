import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'dsCapitalize'
})

/**
 * Pipe for capizalizing a string
 */
export class CapitalizePipe implements PipeTransform {
  /**
   * @param {string} value String to be capitalized
   * @returns {string} Capitalized version of the input value
   */
  transform(value: string): string {
    if (value) {
      return value.charAt(0).toUpperCase() + value.slice(1);
    }
    return value;
  }

}
