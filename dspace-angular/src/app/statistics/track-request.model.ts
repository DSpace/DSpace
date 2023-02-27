import { ResponseParsingService } from '../core/data/parsing.service';
import { PostRequest } from '../core/data/request.models';
import { StatusCodeOnlyResponseParsingService } from '../core/data/status-code-only-response-parsing.service';
import { GenericConstructor } from '../core/shared/generic-constructor';

export class TrackRequest extends PostRequest {

  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return StatusCodeOnlyResponseParsingService;
  }
}
