/**
 * Whether a value is a Number or numeric string.
 *
 * Taken from RxJs 6.x (licensed under Apache 2.0)
 * This function was removed from RxJs 7.x onwards.
 *
 * @param val: any value
 * @returns whether this value is numeric
 */
export function isNumeric(val: any): val is number | string {
  // parseFloat NaNs numeric-cast false positives (null|true|false|"")
  // ...but misinterprets leading-number strings, particularly hex literals ("0x...")
  // subtraction forces infinities to NaN
  // adding 1 corrects loss of precision from parseFloat (#15100)
  return !Array.isArray(val) && (val - parseFloat(val) + 1) >= 0;
}
