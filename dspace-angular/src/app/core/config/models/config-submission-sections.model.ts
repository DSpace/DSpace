import { inheritSerialization } from 'cerialize';
import { typedObject } from '../../cache/builders/build-decorators';
import { SubmissionSectionModel } from './config-submission-section.model';
import { SUBMISSION_SECTIONS_TYPE } from './config-type';

@typedObject
@inheritSerialization(SubmissionSectionModel)
export class SubmissionSectionsModel extends SubmissionSectionModel {
  static type = SUBMISSION_SECTIONS_TYPE;
}
