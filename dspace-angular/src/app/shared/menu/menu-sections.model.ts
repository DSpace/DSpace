import { MenuSection } from './menu-section.model';

/**
 * Represents the state of all menu sections in the store
 */
export interface MenuSections {
  [id: string]: MenuSection;
}
