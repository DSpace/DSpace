import { autoserialize, deserialize } from 'cerialize';
import { typedObject } from 'src/app/core/cache/builders/build-decorators';
import { CacheableObject } from 'src/app/core/cache/cacheable-object.model';
import { HALLink } from 'src/app/core/shared/hal-link.model';
import { ResourceType } from 'src/app/core/shared/resource-type';
import { excludeFromEquals } from 'src/app/core/utilities/equals.decorators';
import { ACCESS_STATUS } from './access-status.resource-type';

@typedObject
export class AccessStatusObject implements CacheableObject {
  static type = ACCESS_STATUS;

  /**
   * The type for this AccessStatusObject
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The access status value
   */
  @autoserialize
  status: string;

  /**
   * The {@link HALLink}s for this AccessStatusObject
   */
   @deserialize
   _links: {
     self: HALLink;
   };
}
