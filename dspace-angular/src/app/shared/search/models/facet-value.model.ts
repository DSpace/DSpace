import { autoserialize, autoserializeAs, deserialize } from 'cerialize';
import { HALLink } from '../../../core/shared/hal-link.model';
import { HALResource } from '../../../core/shared/hal-resource.model';

/**
 * Class representing possible values for a certain filter
 */
export class FacetValue implements HALResource {
  /**
   * The display label of the facet value
   */
  @autoserialize
  label: string;

  /**
   * The value of the facet value
   */
  @autoserializeAs(String, 'label')
  value: string;

  /**
   * The number of results this facet value would have if selected
   */
  @autoserialize
  count: number;

  /**
   * The Authority Value for this facet
   */
  @autoserialize
  authorityKey?: string;

  /**
   * The {@link HALLink}s for this FacetValue
   */
  @deserialize
  _links: {
    self: HALLink
    search: HALLink
  };
}
