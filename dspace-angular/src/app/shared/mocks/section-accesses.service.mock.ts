import { SubmissionFormsModel } from '../../core/config/models/config-submission-forms.model';
import { of as observableOf } from 'rxjs';

const dataRes = Object.assign(new SubmissionFormsModel(), {
  'id': 'AccessConditionDefaultConfiguration',
  'accessConditions': [],
});

export function getSectionAccessesService() {
  return jasmine.createSpyObj('SectionAccessesService', {
    getAccessesData: observableOf(dataRes),
  });
}
