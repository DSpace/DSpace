import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'dsEmphasize' })
/**
 * Pipe for emphasizing a part of a string by surrounding it with <em> tags
 */
export class EmphasizePipe implements PipeTransform {
  /**
   * Characters that should be escaped
   */
  specials = [
    // order matters for these
    '-'
    , '['
    , ']'
    // order doesn't matter for any of these
    , '/'
    , '{'
    , '}'
    , '('
    , ')'
    , '*'
    , '+'
    , '?'
    , '.'
    , '\\'
    , '^'
    , '$'
    , '|'
  ];
  /**
   * Regular expression for escaping the string we're trying to find
   */
  regex = RegExp('[' + this.specials.join('\\') + ']', 'g');

  /**
   *
   * @param haystack The string which we want to partly highlight
   * @param needle The string that should become emphasized in the haystack string
   * @returns {any} Transformed haystack with the needle emphasized
   */
  transform(haystack, needle): any {
    const escaped = this.escapeRegExp(needle);
    const reg = new RegExp(escaped, 'gi');
    return haystack.replace(reg, '<em>$&</em>');
  }

  /**
   *
   * @param str Escape special characters in the string we're looking for
   * @returns {any} The escaped version of the input string
   */
   escapeRegExp(str) {
    return str.replace(this.regex, '\\$&');
  }
}
