/**
 * Enumeration containing all possible types for filters
 */
export enum FilterType {
  /**
   * Represents authority facets
   */
  authority = 'authority',

  /**
   * Represents simple text facets
   */
  text = 'text',

  /**
   * Represents date facets
   */
  range = 'date',

  /**
   * Represents hierarchically structured facets
   */
  hierarchy = 'hierarchical',

  /**
   * Represents binary facets
   */
  boolean = 'standard'
}
