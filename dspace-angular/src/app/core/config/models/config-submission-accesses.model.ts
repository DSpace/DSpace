import { inheritSerialization } from 'cerialize';
import { typedObject } from '../../cache/builders/build-decorators';
import { SUBMISSION_ACCESSES_TYPE } from './config-type';
import { SubmissionAccessModel } from './config-submission-access.model';

@typedObject
@inheritSerialization(SubmissionAccessModel)
export class SubmissionAccessesModel extends SubmissionAccessModel {
  static type = SUBMISSION_ACCESSES_TYPE;
}
