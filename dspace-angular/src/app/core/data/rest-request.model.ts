import { environment } from '../../../environments/environment';
import { RestRequestMethod } from './rest-request-method';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';

/**
 * A request to the DSpace REST API
 */
export abstract class RestRequest {
    public responseMsToLive = environment.cache.msToLive.default;
    public isMultipart = false;

    constructor(
        public uuid: string,
        public href: string,
        public method: RestRequestMethod = RestRequestMethod.GET,
        public body?: any,
        public options?: HttpOptions,
    ) {
    }
}
