import { inheritSerialization } from 'cerialize';
import { typedObject } from '../../cache/builders/build-decorators';
import { SUBMISSION_UPLOADS_TYPE } from './config-type';
import { SubmissionUploadModel } from './config-submission-upload.model';

@typedObject
@inheritSerialization(SubmissionUploadModel)
export class SubmissionUploadsModel extends SubmissionUploadModel {
  static type =  SUBMISSION_UPLOADS_TYPE;
}
