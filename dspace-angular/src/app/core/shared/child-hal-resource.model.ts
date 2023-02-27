import { HALResource } from './hal-resource.model';

/**
 * Interface for HALResources with a parent object link
 */
export interface ChildHALResource extends HALResource {

  /**
   * Returns the key of the parent link
   */
  getParentLinkKey(): keyof this['_links'];
}
