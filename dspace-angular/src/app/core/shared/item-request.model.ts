import { autoserialize, deserialize } from 'cerialize';
import { typedObject } from '../cache/builders/build-decorators';
import { excludeFromEquals } from '../utilities/equals.decorators';
import { ResourceType } from './resource-type';
import { ITEM_REQUEST } from './item-request.resource-type';
import { HALLink } from './hal-link.model';
import { CacheableObject } from '../cache/cacheable-object.model';

/**
 * Model class for an ItemRequest
 */
@typedObject
export class ItemRequest implements CacheableObject {
  static type = ITEM_REQUEST;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * opaque string which uniquely identifies this request
   */
  @autoserialize
  token: string;

  /**
   * true if the request is for all bitstreams of the item.
   */
  @autoserialize
  allfiles: boolean;
  /**
   * email address of the person requesting the files.
   */
  @autoserialize
  requestEmail: string;
  /**
   * Human-readable name of the person requesting the files.
   */
  @autoserialize
  requestName: string;
  /**
   * arbitrary message provided by the person requesting the files.
   */
  @autoserialize
  requestMessage: string;
  /**
   * date that the request was recorded.
   */
  @autoserialize
  requestDate: string;
  /**
   * true if the request has been granted.
   */
  @autoserialize
  acceptRequest: boolean;
  /**
   * date that the request was granted or denied.
   */
  @autoserialize
  decisionDate: string;
  /**
   * date on which the request is considered expired.
   */
  @autoserialize
  expires: string;
  /**
   * UUID of the requested Item.
   */
  @autoserialize
  itemId: string;
  /**
   * UUID of the requested bitstream.
   */
  @autoserialize
  bitstreamId: string;

  /**
   * The {@link HALLink}s for this ItemRequest
   */
  @deserialize
  _links: {
    self: HALLink;
    item: HALLink;
    bitstream: HALLink;
  };

}
