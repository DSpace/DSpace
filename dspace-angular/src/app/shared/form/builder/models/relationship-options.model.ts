const RELATION_METADATA_PREFIX = 'relation.';

/**
 * The submission options for fields that can represent relationships
 */
export class RelationshipOptions {
  relationshipType: string;
  filter: string;
  searchConfiguration: string;
  nameVariants: string;
  externalSources: string[];

  get metadataField() {
    return RELATION_METADATA_PREFIX + this.relationshipType;
  }
}
