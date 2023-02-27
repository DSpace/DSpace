import { autoserialize, deserialize } from 'cerialize';
import { typedObject } from '../core/cache/builders/build-decorators';
import { CacheableObject } from '../core/cache/cacheable-object.model';
import { HALLink } from '../core/shared/hal-link.model';
import { ResourceType } from '../core/shared/resource-type';
import { excludeFromEquals } from '../core/utilities/equals.decorators';
import { SYSTEMWIDEALERT } from './system-wide-alert.resource-type';

/**
 * Object representing a system-wide alert
 */
@typedObject
export class SystemWideAlert implements CacheableObject {
  static type = SYSTEMWIDEALERT;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The identifier for this system-wide alert
   */
  @autoserialize
  alertId: string;

  /**
   * The message for this system-wide alert
   */
  @autoserialize
  message: string;

  /**
   * A string representation of the date to which this system-wide alert will count down when active
   */
  @autoserialize
  countdownTo: string;

  /**
   * Whether the system-wide alert is active
   */
  @autoserialize
  active: boolean;


  /**
   * The {@link HALLink}s for this system-wide alert
   */
  @deserialize
  _links: {
    self: HALLink,
  };
}
