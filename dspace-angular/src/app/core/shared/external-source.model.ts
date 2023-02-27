import { autoserialize, deserialize } from 'cerialize';
import { link, typedObject } from '../cache/builders/build-decorators';
import { excludeFromEquals } from '../utilities/equals.decorators';
import { EXTERNAL_SOURCE } from './external-source.resource-type';
import { HALLink } from './hal-link.model';
import { ResourceType } from './resource-type';
import { CacheableObject } from '../cache/cacheable-object.model';
import { RemoteData } from '../data/remote-data';
import { PaginatedList } from '../data/paginated-list.model';
import { Observable } from 'rxjs';
import { ITEM_TYPE } from './item-relationships/item-type.resource-type';
import { ItemType } from './item-relationships/item-type.model';

/**
 * Model class for an external source
 */
@typedObject
export class ExternalSource extends CacheableObject {
  static type = EXTERNAL_SOURCE;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * Unique identifier
   */
  @autoserialize
  id: string;

  /**
   * The name of this external source
   */
  @autoserialize
  name: string;

  /**
   * Is the source hierarchical?
   */
  @autoserialize
  hierarchical: boolean;

  /**
   * The list of entity types that are compatible with this external source
   * Will be undefined unless the entityTypes {@link HALLink} has been resolved.
   */
  @link(ITEM_TYPE, true)
  entityTypes?: Observable<RemoteData<PaginatedList<ItemType>>>;

  /**
   * The {@link HALLink}s for this ExternalSource
   */
  @deserialize
  _links: {
    self: HALLink;
    entries: HALLink;
    entityTypes: HALLink;
  };
}
