import { autoserialize, deserialize, deserializeAs } from 'cerialize';
import { link, typedObject } from '../../cache/builders/build-decorators';
import { IDToUUIDSerializer } from '../../cache/id-to-uuid-serializer';
import { ActionType } from './action-type.model';
import { HALLink } from '../../shared/hal-link.model';
import { RESOURCE_POLICY } from './resource-policy.resource-type';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { ResourceType } from '../../shared/resource-type';
import { PolicyType } from './policy-type.model';
import { Observable } from 'rxjs';
import { RemoteData } from '../../data/remote-data';
import { GROUP } from '../../eperson/models/group.resource-type';
import { Group } from '../../eperson/models/group.model';
import { EPERSON } from '../../eperson/models/eperson.resource-type';
import { EPerson } from '../../eperson/models/eperson.model';
import { CacheableObject } from '../../cache/cacheable-object.model';

/**
 * Model class for a Resource Policy
 */
@typedObject
export class ResourcePolicy implements CacheableObject {
  static type = RESOURCE_POLICY;

  /**
   * The identifier for this Resource Policy
   */
  @autoserialize
  id: string;

  /**
   * The name for this Resource Policy
   */
  @autoserialize
  name: string;

  /**
   * The description for this Resource Policy
   */
  @autoserialize
  description: string;

  /**
   * The classification or this Resource Policy
   */
  @autoserialize
  policyType: PolicyType;

  /**
   * The action that is allowed by this Resource Policy
   */
  @autoserialize
  action: ActionType;

  /**
   * The first day of validity of the policy (format YYYY-MM-DD)
   */
  @autoserialize
  startDate: string;

  /**
   * The last day of validity of the policy (format YYYY-MM-DD)
   */
  @autoserialize
  endDate: string;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The universally unique identifier for this Resource Policy
   * This UUID is generated client-side and isn't used by the backend.
   * It is based on the ID, so it will be the same for each refresh.
   */
  @deserializeAs(new IDToUUIDSerializer('resource-policy'), 'id')
  uuid: string;

  /**
   * The {@link HALLink}s for this ResourcePolicy
   */
  @deserialize
  _links: {
    eperson: HALLink,
    group: HALLink,
    self: HALLink,
  };

  /**
   * The eperson linked by this resource policy
   * Will be undefined unless the version {@link HALLink} has been resolved.
   */
  @link(EPERSON)
  eperson?: Observable<RemoteData<EPerson>>;

  /**
   * The group linked by this resource policy
   * Will be undefined unless the version {@link HALLink} has been resolved.
   */
  @link(GROUP)
  group?: Observable<RemoteData<Group>>;
}
