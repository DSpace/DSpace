import { autoserialize, deserialize, deserializeAs } from 'cerialize';
import { typedObject } from '../cache/builders/build-decorators';
import { IDToUUIDSerializer } from '../cache/id-to-uuid-serializer';
import { excludeFromEquals } from '../utilities/equals.decorators';
import { BitstreamFormatSupportLevel } from './bitstream-format-support-level';
import { BITSTREAM_FORMAT } from './bitstream-format.resource-type';
import { HALLink } from './hal-link.model';
import { ResourceType } from './resource-type';
import { CacheableObject } from '../cache/cacheable-object.model';

/**
 * Model class for a Bitstream Format
 */
@typedObject
export class BitstreamFormat implements CacheableObject {
  static type = BITSTREAM_FORMAT;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * Short description of this Bitstream Format
   */
  @autoserialize
  shortDescription: string;

  /**
   * Description of this Bitstream Format
   */
  @autoserialize
  description: string;

  /**
   * String representing the MIME type of this Bitstream Format
   */
  @autoserialize
  mimetype: string;

  /**
   * The level of support the system offers for this Bitstream Format
   */
  @autoserialize
  supportLevel: BitstreamFormatSupportLevel;

  /**
   * True if the Bitstream Format is used to store system information, rather than the content of items in the system
   */
  @autoserialize
  internal: boolean;

  /**
   * String representing this Bitstream Format's file extension
   */
  @autoserialize
  extensions: string[];

  /**
   * Universally unique identifier for this Bitstream Format
   * This UUID is generated client-side and isn't used by the backend.
   * It is based on the ID, so it will be the same for each refresh.
   */
  @deserializeAs(new IDToUUIDSerializer('bitstream-format'), 'id')
  uuid: string;

  /**
   * Identifier for this Bitstream Format
   * Note that this ID is unique for bitstream formats,
   * but might not be unique across different object types
   */
  @autoserialize
  id: string;

  /**
   * The {@link HALLink}s for this BitstreamFormat
   */
  @deserialize
  _links: {
    self: HALLink;
  };
}
