import { Config } from './config.interface';

/**
 * Config that determines how the dropdown list of years are created for
 * browse-by-date components.
 */
export interface BrowseByConfig extends Config {
  /**
   * The max amount of years to display using jumps of one year
   * (current year - oneYearLimit)
   */
  oneYearLimit: number;

  /**
   * Limit for years to display using jumps of five years
   * (current year - fiveYearLimit)
   */
  fiveYearLimit: number;

  /**
   * The absolute lowest year to display in the dropdown when no lowest date can
   * be found for all items.
   */
  defaultLowerLimit: number;

  /**
   *  If true, thumbnail images for items will be added to BOTH search and browse result lists.
   */
  showThumbnails: boolean;

  /**
   * Number of entries in the viewport of a paginated browse-by list.
   * Rounded to the nearest size in the list of selectable sizes on the settings
   * menu.  See pageSizeOptions in 'pagination-component-options.model.ts'.
   */
   pageSize: number;

}
