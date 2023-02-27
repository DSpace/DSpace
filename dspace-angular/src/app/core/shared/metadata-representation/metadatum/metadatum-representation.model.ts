import { MetadataRepresentation, MetadataRepresentationType } from '../metadata-representation.model';
import { hasValue } from '../../../../shared/empty.util';
import { MetadataValue } from '../../metadata.models';
import { BrowseDefinition } from '../../browse-definition.model';

/**
 * This class defines the way the metadatum it extends should be represented
 */
export class MetadatumRepresentation extends MetadataValue implements MetadataRepresentation {

  /**
   * The type of item this metadatum can be represented as
   */
  itemType: string;

  /**
   * The browse definition ID passed in with the metadatum, if any
   */
  browseDefinition?: BrowseDefinition;

  constructor(itemType: string, browseDefinition?: BrowseDefinition) {
    super();
    this.itemType = itemType;
    this.browseDefinition = browseDefinition;
  }

  /**
   * Fetch the way this metadatum should be rendered as in a list
   */
  get representationType(): MetadataRepresentationType {
    if (hasValue(this.authority)) {
      return MetadataRepresentationType.AuthorityControlled;
    } else if (hasValue(this.browseDefinition)) {
      return MetadataRepresentationType.BrowseLink;
    } else {
      return MetadataRepresentationType.PlainText;
    }
  }

  /**
   * Get the value to display
   */
  getValue(): string {
    return this.value;
  }

}
