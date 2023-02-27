import { autoserialize, deserialize, deserializeAs } from 'cerialize';
import { Observable } from 'rxjs';
import { link, typedObject } from '../../cache/builders/build-decorators';
import { IDToUUIDSerializer } from '../../cache/id-to-uuid-serializer';
import { RemoteData } from '../../data/remote-data';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { HALLink } from '../hal-link.model';
import { ResourceType } from '../resource-type';
import { ItemType } from './item-type.model';
import { ITEM_TYPE } from './item-type.resource-type';
import { RELATIONSHIP_TYPE } from './relationship-type.resource-type';
import { CacheableObject } from '../../cache/cacheable-object.model';

/**
 * Describes a type of Relationship between multiple possible Items
 */
@typedObject
export class RelationshipType implements CacheableObject {
  static type = RELATIONSHIP_TYPE;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The label that describes this RelationshipType
   */
  @autoserialize
  label: string;

  /**
   * The identifier of this RelationshipType
   */
  @autoserialize
  id: string;

  /**
   * The universally unique identifier of this RelationshipType
   * This UUID is generated client-side and isn't used by the backend.
   * It is based on the ID, so it will be the same for each refresh.
   */
  @deserializeAs(new IDToUUIDSerializer(RelationshipType.type.value), 'id')
  uuid: string;

  /**
   * The label that describes the Relation to the left of this RelationshipType
   */
  @autoserialize
  leftwardType: string;

  /**
   * The maximum amount of Relationships allowed to the left of this RelationshipType
   */
  @autoserialize
  leftMaxCardinality: number;

  /**
   * The minimum amount of Relationships allowed to the left of this RelationshipType
   */
  @autoserialize
  leftMinCardinality: number;

  /**
   * The label that describes the Relation to the right of this RelationshipType
   */
  @autoserialize
  rightwardType: string;

  /**
   * The maximum amount of Relationships allowed to the right of this RelationshipType
   */
  @autoserialize
  rightMaxCardinality: number;

  /**
   * The minimum amount of Relationships allowed to the right of this RelationshipType
   */
  @autoserialize
  rightMinCardinality: number;

  /**
   * The {@link HALLink}s for this RelationshipType
   */
  @deserialize
  _links: {
    self: HALLink;
    leftType: HALLink;
    rightType: HALLink;
  };

  /**
   * The type of Item found on the left side of this RelationshipType
   * Will be undefined unless the leftType {@link HALLink} has been resolved.
   */
  @link(ITEM_TYPE)
  leftType?: Observable<RemoteData<ItemType>>;

  /**
   * The type of Item found on the right side of this RelationshipType
   * Will be undefined unless the rightType {@link HALLink} has been resolved.
   */
  @link(ITEM_TYPE)
  rightType?: Observable<RemoteData<ItemType>>;
}
