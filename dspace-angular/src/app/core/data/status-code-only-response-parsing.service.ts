import { Injectable } from '@angular/core';
import { RestResponse } from '../cache/response.models';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { ResponseParsingService } from './parsing.service';
import { RestRequest } from './rest-request.model';

/**
 * A responseparser that will only look at the status code and status
 * text of the response, and ignore anything else that might be there
 */
@Injectable({
  providedIn: 'root'
})
export class StatusCodeOnlyResponseParsingService implements ResponseParsingService {

  /**
   * Parse the response and only extract the status code and status text
   *
   * @param request The request that was sent to the server
   * @param data The response to parse
   */
  parse(request: RestRequest, data: RawRestResponse): RestResponse {
    const isSuccessful = data.statusCode >= 200 && data.statusCode < 300;
    return new RestResponse(isSuccessful, data.statusCode, data.statusText);
  }
}
