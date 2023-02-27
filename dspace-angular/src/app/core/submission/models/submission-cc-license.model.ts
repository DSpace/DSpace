import { autoserialize, inheritSerialization } from 'cerialize';
import { typedObject } from '../../cache/builders/build-decorators';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { ResourceType } from '../../shared/resource-type';
import { HALResource } from '../../shared/hal-resource.model';
import { SUBMISSION_CC_LICENSE } from './submission-cc-licence.resource-type';

@typedObject
@inheritSerialization(HALResource)
export class SubmissionCcLicence extends HALResource {

  static type = SUBMISSION_CC_LICENSE;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  @autoserialize
  id: string;

  @autoserialize
  name: string;

  @autoserialize
  fields: Field[];
}

export interface Field {
  id: string;
  label: string;
  description: string;
  enums: Option[];
}

export interface Option {
  id: string;
  label: string;
  description: string;
}
