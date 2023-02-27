import { HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { RemoteData } from '../data/remote-data';
import { PostRequest } from '../data/request.models';
import { RequestService } from '../data/request.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { OrcidHistory } from './model/orcid-history.model';
import { ORCID_HISTORY } from './model/orcid-history.resource-type';
import { OrcidQueue } from './model/orcid-queue.model';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import { RestRequest } from '../data/rest-request.model';
import { sendRequest } from '../shared/request.operators';
import { IdentifiableDataService } from '../data/base/identifiable-data.service';
import { dataService } from '../data/base/data-service.decorator';

/**
 * A service that provides methods to make REST requests with Orcid History endpoint.
 */
@Injectable()
@dataService(ORCID_HISTORY)
export class OrcidHistoryDataService extends IdentifiableDataService<OrcidHistory> {

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
  ) {
    super('orcidhistories', requestService, rdbService, objectCache, halService, 10 * 1000);
  }

  sendToORCID(orcidQueue: OrcidQueue): Observable<RemoteData<OrcidHistory>> {
    const requestId = this.requestService.generateRequestId();
    return this.getEndpoint().pipe(
      map((endpointURL: string) => {
        const options: HttpOptions = Object.create({});
        let headers = new HttpHeaders();
        headers = headers.append('Content-Type', 'text/uri-list');
        options.headers = headers;
        return new PostRequest(requestId, endpointURL, orcidQueue._links.self.href, options);
      }),
      sendRequest(this.requestService),
      switchMap((request: RestRequest) => this.rdbService.buildFromRequestUUID(request.uuid)  as Observable<RemoteData<OrcidHistory>>)
    );
  }
}
