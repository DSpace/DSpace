import { Injectable } from '@angular/core';

import { Store } from '@ngrx/store';

import { RequestService } from '../data/request.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { JsonPatchOperationsService } from '../json-patch/json-patch-operations.service';
import { SubmitDataResponseDefinitionObject } from '../shared/submit-data-response-definition.model';
import { SubmissionPatchRequest } from '../data/request.models';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { CoreState } from '../core-state.model';

/**
 * A service that provides methods to make JSON Patch requests.
 */
@Injectable()
export class SubmissionJsonPatchOperationsService extends JsonPatchOperationsService<SubmitDataResponseDefinitionObject, SubmissionPatchRequest> {
  protected linkPath = '';
  protected patchRequestConstructor = SubmissionPatchRequest;

  constructor(
    protected requestService: RequestService,
    protected store: Store<CoreState>,
    protected rdbService: RemoteDataBuildService,
    protected halService: HALEndpointService) {

    super();
  }

}
