import { autoserialize, inheritSerialization, deserialize } from 'cerialize';
import { typedObject, link } from '../../cache/builders/build-decorators';
import { ConfigObject } from './config.model';
import { AccessConditionOption } from './config-access-condition-option.model';
import { SubmissionFormsModel } from './config-submission-forms.model';
import { SUBMISSION_UPLOAD_TYPE, SUBMISSION_FORMS_TYPE } from './config-type';
import { HALLink } from '../../shared/hal-link.model';
import { RemoteData } from '../../data/remote-data';
import { Observable } from 'rxjs';

@typedObject
@inheritSerialization(ConfigObject)
export class SubmissionUploadModel extends ConfigObject {
  static type =  SUBMISSION_UPLOAD_TYPE;
  /**
   * A list of available bitstream access conditions
   */
  @autoserialize
  accessConditionOptions: AccessConditionOption[];

  /**
   * An object representing the configuration describing the bitstream metadata form
   */
  @link(SUBMISSION_FORMS_TYPE)
  metadata?: Observable<RemoteData<SubmissionFormsModel>>;

  @autoserialize
  required: boolean;

  @autoserialize
  maxSize: number;

  @deserialize
  _links: {
    metadata: HALLink
    self: HALLink
  };

}
