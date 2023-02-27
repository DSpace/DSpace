/**
 * An interface to represent objects returned by the REST API
 * that don't have a self link and therefore can't be cached
 */
export interface UnCacheableObject {
  [key: string]: any;
  _links?: {
    [key: string]: any;
    self?: never; // UnCacheableObjects can't have a self link
  };
}
