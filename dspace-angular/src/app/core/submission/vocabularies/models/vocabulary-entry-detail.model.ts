import { autoserialize, deserialize, inheritSerialization } from 'cerialize';

import { HALLink } from '../../../shared/hal-link.model';
import { VOCABULARY_ENTRY_DETAIL } from './vocabularies.resource-type';
import { typedObject } from '../../../cache/builders/build-decorators';
import { VocabularyEntry } from './vocabulary-entry.model';

/**
 * Model class for a VocabularyEntryDetail
 */
@typedObject
@inheritSerialization(VocabularyEntry)
export class VocabularyEntryDetail extends VocabularyEntry {
  static type = VOCABULARY_ENTRY_DETAIL;

  /**
   * The unique id of the entry
   */
  @autoserialize
  id: string;

  /**
   * In an hierarchical vocabulary representing if entry is selectable as value
   */
  @autoserialize
  selectable: boolean;

  /**
   * The {@link HALLink}s for this ExternalSourceEntry
   */
  @deserialize
  _links: {
    self: HALLink;
    vocabulary: HALLink;
    parent: HALLink;
    children
  };

}
