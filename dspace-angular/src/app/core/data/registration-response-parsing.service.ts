import { Injectable } from '@angular/core';
import {
  ParsedResponse
} from '../cache/response.models';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { ResponseParsingService } from './parsing.service';
import { Registration } from '../shared/registration.model';
import { RestRequest } from './rest-request.model';

@Injectable({
  providedIn: 'root',
})
/**
 * Parsing service responsible for parsing a Registration response
 */
export class RegistrationResponseParsingService implements ResponseParsingService {

  parse(request: RestRequest, data: RawRestResponse): ParsedResponse {
    const payload = data.payload;

    const registration = Object.assign(new Registration(), payload);

    return new ParsedResponse(data.statusCode, undefined, registration);
  }

}
