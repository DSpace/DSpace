import { URLCombiner } from './url-combiner';
import { environment } from '../../../environments/environment';

/**
 * Combines a variable number of strings representing parts
 * of a relative REST URL in to a single, absolute REST URL
 *
 * TODO write tests once GlobalConfig becomes injectable
 */
export class RESTURLCombiner extends URLCombiner {
  constructor(...parts: string[]) {
    super(environment.rest.baseUrl, '/api', ...parts);
  }
}
