import { autoserialize, deserialize, inheritSerialization } from 'cerialize';
import { typedObject } from '../../cache/builders/build-decorators';
import { ConfigObject } from './config.model';
import { AccessesConditionOption } from './config-accesses-conditions-options.model';
import { SUBMISSION_ACCESSES_TYPE } from './config-type';
import { HALLink } from '../../shared/hal-link.model';

/**
 * Class for the configuration describing the item accesses condition
 */
@typedObject
@inheritSerialization(ConfigObject)
export class SubmissionAccessModel extends ConfigObject {
  static type = SUBMISSION_ACCESSES_TYPE;

  /**
   * A list of available item access conditions
   */
  @autoserialize
  accessConditionOptions: AccessesConditionOption[];

  /**
   * Boolean that indicates whether the current item must be findable via search or browse.
   */
  @autoserialize
  discoverable: boolean;

  /**
   * Boolean that indicates whether or not the user can change the discoverable flag.
   */
  @autoserialize
  canChangeDiscoverable: boolean;

  /**
   * The links to all related resources returned by the rest api.
   */
  @deserialize
  _links: {
    self: HALLink
  };

}
