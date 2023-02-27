import { autoserialize, deserialize } from 'cerialize';

import { HALLink } from '../../../shared/hal-link.model';
import { VOCABULARY, VOCABULARY_ENTRY } from './vocabularies.resource-type';
import { typedObject, link } from '../../../cache/builders/build-decorators';
import { excludeFromEquals } from '../../../utilities/equals.decorators';
import { Observable } from 'rxjs';
import { RemoteData } from '../../../data/remote-data';
import { PaginatedList } from '../../../data/paginated-list.model';
import { VocabularyEntry } from './vocabulary-entry.model';
import { CacheableObject } from '../../../cache/cacheable-object.model';

/**
 * Model class for a Vocabulary
 */
@typedObject
export class Vocabulary implements CacheableObject {
  static type = VOCABULARY;
  /**
   * The identifier of this Vocabulary
   */
  @autoserialize
  id: string;

  /**
   * The name of this Vocabulary
   */
  @autoserialize
  name: string;

  /**
   * True if it is possible to scroll all the entries in the vocabulary without providing a filter parameter
   */
  @autoserialize
  scrollable: boolean;

  /**
   * True if the vocabulary exposes a tree structure where some entries are parent of others
   */
  @autoserialize
  hierarchical: boolean;

  /**
   * For hierarchical vocabularies express the preference to preload the tree at a specific
   * level of depth (0 only the top nodes are shown, 1 also their children are preloaded and so on)
   */
  @autoserialize
  preloadLevel: any;

  /**
   * A string representing the kind of Vocabulary model
   */
  @excludeFromEquals
  @autoserialize
  public type: any;

  @link(VOCABULARY_ENTRY, true)
  entries?: Observable<RemoteData<PaginatedList<VocabularyEntry>>>;

  /**
   * The {@link HALLink}s for this Vocabulary
   */
  @deserialize
  _links: {
    self: HALLink,
    entries: HALLink
  };
}
