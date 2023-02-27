import { autoserialize, deserialize } from 'cerialize';
import { typedObject } from 'src/app/core/cache/builders/build-decorators';
import { CacheableObject } from 'src/app/core/cache/cacheable-object.model';
import { HALLink } from 'src/app/core/shared/hal-link.model';
import { ResourceType } from 'src/app/core/shared/resource-type';
import { excludeFromEquals } from 'src/app/core/utilities/equals.decorators';
import { IDENTIFIERS } from './identifier-data.resource-type';
import {Identifier} from './identifier.model';

@typedObject
export class IdentifierData implements CacheableObject {
  static type = IDENTIFIERS;
  /**
   * The type for this IdentifierData
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The
   */
  @autoserialize
  identifiers: Identifier[];

  /**
   * The {@link HALLink}s for this IdentifierData
   */
   @deserialize
   _links: {
     self: HALLink;
   };
}
