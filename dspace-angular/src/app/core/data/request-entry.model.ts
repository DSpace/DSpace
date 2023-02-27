import { RestRequestWithResponseParser } from './rest-request-with-response-parser.model';
import { RequestEntryState } from './request-entry-state.model';
import { ResponseState } from './response-state.model';

/**
 * An entry for a request in the NgRx store
 */
export class RequestEntry {
    request: RestRequestWithResponseParser;
    state: RequestEntryState;
    response: ResponseState;
    lastUpdated: number;
}
