import { HALLink } from './hal-link.model';
import { deserialize } from 'cerialize';

/**
 * Represents HAL resources.
 *
 * A HAL resource has a _links section with at least a self link.
 */
export class HALResource {

  /**
   * The {@link HALLink}s for this {@link HALResource}
   */
  @deserialize
  _links: {

    /**
     * The {@link HALLink} that refers to this {@link HALResource}
     */
    self: HALLink

    /**
     * {@link HALLink}s to related {@link HALResource}s
     */
    [k: string]: HALLink | HALLink[];
  };
}
