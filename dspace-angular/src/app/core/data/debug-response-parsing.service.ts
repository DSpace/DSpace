import { Injectable } from '@angular/core';
import { RestResponse } from '../cache/response.models';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { ResponseParsingService } from './parsing.service';
import { RestRequest } from './rest-request.model';

@Injectable()
export class DebugResponseParsingService implements ResponseParsingService {
  parse(request: RestRequest, data: RawRestResponse): RestResponse {
    console.log('request', request, 'data', data);
    return undefined;
  }
}
