import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { ParsedResponse } from '../cache/response.models';
import { RestRequest } from './rest-request.model';

export interface ResponseParsingService {
  parse(request: RestRequest, data: RawRestResponse): ParsedResponse;
}
