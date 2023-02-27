import { Injectable } from '@angular/core';

import { ResponseParsingService } from '../data/parsing.service';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';

import { BaseResponseParsingService } from '../data/base-response-parsing.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { ParsedResponse } from '../cache/response.models';
import { RestRequest } from '../data/rest-request.model';

/**
 * Provides methods to parse response for a task request.
 */
@Injectable()
export class TaskResponseParsingService extends BaseResponseParsingService implements ResponseParsingService {

  protected toCache = false;

  /**
   * Initialize instance variables
   *
   * @param {ObjectCacheService} objectCache
   */
  constructor(protected objectCache: ObjectCacheService) {
    super();
  }

  /**
   * Parses data from the tasks endpoints
   *
   * @param {RestRequest} request
   * @param {RawRestResponse} data
   * @returns {RestResponse}
   */
  parse(request: RestRequest, data: RawRestResponse): ParsedResponse {
    if (this.isSuccessStatus(data.statusCode)) {
      return new ParsedResponse(data.statusCode);
    } else {
      throw new Error('Unexpected response from server');
    }
  }

}
