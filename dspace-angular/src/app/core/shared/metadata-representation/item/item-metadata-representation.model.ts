import { Item } from '../../item.model';
import { MetadataRepresentation, MetadataRepresentationType } from '../metadata-representation.model';
import { MetadataValue } from '../../metadata.models';

/**
 * This class determines which fields to use when rendering an Item as a metadata value.
 */
export class ItemMetadataRepresentation extends Item implements MetadataRepresentation {

  /**
   * The virtual metadata value representing this item
   */
  virtualMetadata: MetadataValue;

  constructor(virtualMetadata: MetadataValue) {
    super();
    this.virtualMetadata = virtualMetadata;
  }

  /**
   * The type of item this item can be represented as
   */
  get itemType(): string {
    return this.firstMetadataValue('dspace.entity.type');
  }

  /**
   * Fetch the way this item should be rendered as in a list
   */
  get representationType(): MetadataRepresentationType {
    return MetadataRepresentationType.Item;
  }

  /**
   * Get the value to display, depending on the itemType
   */
  getValue(): string {
    return this.virtualMetadata.value;
  }

}
