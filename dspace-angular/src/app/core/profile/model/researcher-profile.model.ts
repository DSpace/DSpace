import { Observable } from 'rxjs';
import { autoserialize, deserialize, deserializeAs } from 'cerialize';

import { link, typedObject } from '../../cache/builders/build-decorators';
import { HALLink } from '../../shared/hal-link.model';
import { ResourceType } from '../../shared/resource-type';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { RESEARCHER_PROFILE } from './researcher-profile.resource-type';
import { CacheableObject } from '../../cache/cacheable-object.model';
import { RemoteData } from '../../data/remote-data';
import { ITEM } from '../../shared/item.resource-type';
import { Item } from '../../shared/item.model';

/**
 * Class the represents a Researcher Profile.
 */
@typedObject
export class ResearcherProfile extends CacheableObject {

  static type = RESEARCHER_PROFILE;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The identifier of this Researcher Profile
   */
  @autoserialize
  id: string;

  @deserializeAs('id')
  uuid: string;

  /**
   * The visibility of this Researcher Profile
   */
  @autoserialize
  visible: boolean;

  /**
   * The {@link HALLink}s for this Researcher Profile
   */
  @deserialize
  _links: {
    self: HALLink,
    item: HALLink,
    eperson: HALLink
  };

  /**
   * The related person Item
   * Will be undefined unless the item {@link HALLink} has been resolved.
   */
  @link(ITEM)
  item?: Observable<RemoteData<Item>>;

}
