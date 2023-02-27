/**
 * An Enum defining the representation type of metadata
 */
import { BrowseDefinition } from '../browse-definition.model';

export enum MetadataRepresentationType {
  None = 'none',
  Item = 'item',
  AuthorityControlled = 'authority_controlled',
  PlainText = 'plain_text',
  BrowseLink = 'browse_link'
}

/**
 * An interface containing information about how we should represent certain metadata
 */
export interface MetadataRepresentation {
  /**
   * The type of item this metadata is representing
   * e.g. 'Person'
   * This can be used for template matching
   */
  itemType: string;

  /**
   * How we should render the metadata in a list
   */
  representationType: MetadataRepresentationType;

  /**
   * The browse definition (optional)
   */
  browseDefinition?: BrowseDefinition;

  /**
   * Fetches the value to be displayed
   */
  getValue(): string;

}
