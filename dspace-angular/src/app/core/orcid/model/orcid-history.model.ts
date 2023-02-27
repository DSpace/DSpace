import { autoserialize, deserialize } from 'cerialize';
import { typedObject } from '../../cache/builders/build-decorators';
import { HALLink } from '../../shared/hal-link.model';
import { ResourceType } from '../../shared/resource-type';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { ORCID_HISTORY } from './orcid-history.resource-type';
import { CacheableObject } from '../../cache/cacheable-object.model';

/**
 * Class the represents a Orcid History.
 */
@typedObject
export class OrcidHistory extends CacheableObject {

  static type = ORCID_HISTORY;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The identifier of this Orcid History record
   */
  @autoserialize
  id: number;

  /**
   * The name of the related entity
   */
  @autoserialize
  entityName: string;

  /**
   * The identifier of the profileItem of this Orcid History record.
   */
  @autoserialize
  profileItemId: string;

  /**
   * The identifier of the entity related to this Orcid History record.
   */
  @autoserialize
  entityId: string;

  /**
   * The type of the entity related to this Orcid History record.
   */
  @autoserialize
  entityType: string;

  /**
   * The response status coming from ORCID api.
   */
  @autoserialize
  status: number;

  /**
   * The putCode assigned by ORCID to the entity.
   */
  @autoserialize
  putCode: string;

  /**
   * The last send attempt timestamp.
   */
  lastAttempt: string;

  /**
   * The success send attempt timestamp.
   */
  successAttempt: string;

  /**
   * The response coming from ORCID.
   */
  responseMessage: string;

  /**
   * The {@link HALLink}s for this Orcid History record
   */
  @deserialize
  _links: {
    self: HALLink,
  };

}
