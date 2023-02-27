import { inheritSerialization } from 'cerialize';
import { typedObject } from '../../cache/builders/build-decorators';
import { SubmissionDefinitionModel } from './config-submission-definition.model';
import { SUBMISSION_DEFINITIONS_TYPE } from './config-type';

@typedObject
@inheritSerialization(SubmissionDefinitionModel)
export class SubmissionDefinitionsModel extends SubmissionDefinitionModel {
  static type = SUBMISSION_DEFINITIONS_TYPE;

}
