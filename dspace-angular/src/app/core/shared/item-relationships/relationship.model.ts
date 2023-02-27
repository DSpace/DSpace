import { autoserialize, deserialize, deserializeAs } from 'cerialize';
import { Observable } from 'rxjs';
import { link, typedObject } from '../../cache/builders/build-decorators';
import { IDToUUIDSerializer } from '../../cache/id-to-uuid-serializer';
import { RemoteData } from '../../data/remote-data';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { HALLink } from '../hal-link.model';
import { Item } from '../item.model';
import { ITEM } from '../item.resource-type';
import { ResourceType } from '../resource-type';
import { RelationshipType } from './relationship-type.model';
import { RELATIONSHIP_TYPE } from './relationship-type.resource-type';
import { RELATIONSHIP } from './relationship.resource-type';
import { CacheableObject } from '../../cache/cacheable-object.model';

/**
 * Describes a Relationship between two Items
 */
@typedObject
export class Relationship implements CacheableObject {
  static type = RELATIONSHIP;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The universally unique identifier of this Relationship
   * This UUID is generated client-side and isn't used by the backend.
   * It is based on the ID, so it will be the same for each refresh.
   */
  @deserializeAs(new IDToUUIDSerializer(Relationship.type.value), 'id')
  uuid: string;

  /**
   * The identifier of this Relationship
   */
  @autoserialize
  id: string;

  /**
   * The place of the Item to the left side of this Relationship
   */
  @autoserialize
  leftPlace: number;

  /**
   * The place of the Item to the right side of this Relationship
   */
  @autoserialize
  rightPlace: number;

  /**
   * The name variant of the Item to the left side of this Relationship
   */
  @autoserialize
  leftwardValue: string;

  /**
   * The name variant of the Item to the right side of this Relationship
   */
  @autoserialize
  rightwardValue: string;

  /**
   * The {@link HALLink}s for this Relationship
   */
  @deserialize
  _links: {
    self: HALLink;
    leftItem: HALLink;
    rightItem: HALLink;
    relationshipType: HALLink;
  };

  /**
   * The item on the left side of this relationship
   * Will be undefined unless the leftItem {@link HALLink} has been resolved.
   */
  @link(ITEM)
  leftItem?: Observable<RemoteData<Item>>;

  /**
   * The item on the right side of this relationship
   * Will be undefined unless the rightItem {@link HALLink} has been resolved.
   */
  @link(ITEM)
  rightItem?: Observable<RemoteData<Item>>;

  /**
   * The RelationshipType for this Relationship
   * Will be undefined unless the relationshipType {@link HALLink} has been resolved.
   */
  @link(RELATIONSHIP_TYPE)
  relationshipType?: Observable<RemoteData<RelationshipType>>;

}
