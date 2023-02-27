/**
 * Sets the class to be used for the "no thumbnail"
 * placeholder font size in lists.
 */
export function  setPlaceHolderAttributes(width: number): string {
  if (width < 400) {
    return 'thumb-font-0';
  } else if (width < 750) {
    return 'thumb-font-1';
  } else if (width < 1000) {
    return 'thumb-font-2';
  } else {
    return 'thumb-font-3';
  }
}
