import { RestRequest } from './rest-request.model';
import { GenericConstructor } from '../shared/generic-constructor';
import { ResponseParsingService } from './parsing.service';

/**
 * A RestRequest with a method to retrieve the ResponseParsingService needed for its response
 */
export abstract class RestRequestWithResponseParser extends RestRequest {

  /**
   * Get the ResponseParsingService needed to parse the response to this request
   */
  abstract getResponseParser(): GenericConstructor<ResponseParsingService>;
}
