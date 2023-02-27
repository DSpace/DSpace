import { of as observableOf } from 'rxjs';
import { Store } from '@ngrx/store';

import { RequestService } from '../../core/data/request.service';
import { HALEndpointService } from '../../core/shared/hal-endpoint.service';
import { CoreState } from '../../core/core-state.model';

export class SubmissionRestServiceStub {
  protected linkPath = 'workspaceitems';
  protected requestService: RequestService;
  protected store: Store<CoreState>;
  protected halService: HALEndpointService;

  deleteById = jasmine.createSpy('deleteById');
  fetchRequest = jasmine.createSpy('fetchRequest');
  getDataById = jasmine.createSpy('getDataById');
  getDataByHref = jasmine.createSpy('getDataByHref');
  getEndpointByIDHref = jasmine.createSpy('getEndpointByIDHref');
  patchToEndpoint = jasmine.createSpy('patchToEndpoint');
  postToEndpoint = jasmine.createSpy('postToEndpoint').and.returnValue(observableOf({}));
  submitData = jasmine.createSpy('submitData');
}
