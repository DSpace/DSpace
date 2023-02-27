import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, of as observableOf } from 'rxjs';
import { isEmpty } from '../../empty.util';
import { RestRequestMethod } from '../../../core/data/rest-request-method';

import { RawRestResponse } from '../../../core/dspace-rest/raw-rest-response.model';
import { DspaceRestService, HttpOptions } from '../../../core/dspace-rest/dspace-rest.service';
import { MOCK_RESPONSE_MAP, ResponseMapMock } from './mocks/response-map.mock';
import { environment } from '../../../../environments/environment';

/**
 * Service to access DSpace's REST API.
 *
 * If a URL is found in this.mockResponseMap, it returns the mock response instead
 * This service can be used for mocking REST responses when developing new features
 * This is especially useful, when a REST endpoint is broken or does not exist yet
 */
@Injectable()
export class EndpointMockingRestService extends DspaceRestService {

  constructor(
    @Inject(MOCK_RESPONSE_MAP) protected mockResponseMap: ResponseMapMock,
    protected http: HttpClient
  ) {
    super(http);
  }

  /**
   * Performs a request to the REST API with the `get` http method.
   *
   * If the URL is found in this.mockResponseMap,
   * it returns the mock response instead
   *
   * @param absoluteURL
   *      A URL
   * @return Observable<RawRestResponse>
   *      An Observable<RawRestResponse> containing the response
   */
  get(absoluteURL: string): Observable<RawRestResponse> {
    const mockData = this.getMockData(absoluteURL);
    if (isEmpty(mockData)) {
      return super.get(absoluteURL);
    } else {
      return this.toMockResponse$(mockData);
    }
  }

  /**
   * Performs a request to the REST API.
   *
   * If the URL is found in this.mockResponseMap,
   * it returns the mock response instead
   *
   * @param method
   *    the HTTP method for the request
   * @param url
   *    the URL for the request
   * @param body
   *    an optional body for the request
   * @return Observable<RawRestResponse>
   *      An Observable<RawRestResponse> containing the response from the server
   */
  request(method: RestRequestMethod, url: string, body?: any, options?: HttpOptions, isMultipart?: boolean): Observable<RawRestResponse> {
    const mockData = this.getMockData(url);
    if (isEmpty(mockData)) {
      return super.request(method, url, body, options, isMultipart);
    } else {
      return this.toMockResponse$(mockData);
    }
  }

  /**
   * Turn the mock object in to an Observable<RawRestResponse>
   *
   * @param mockData
   *    the mock response
   * @return
   *    an Observable<RawRestResponse> containing the mock response
   */
  private toMockResponse$(mockData: any): Observable<RawRestResponse> {
    return observableOf({
      payload: mockData,
      headers: new HttpHeaders(),
      statusCode: 200,
      statusText: 'OK'
    });
  }

  /**
   * Get the mock response associated with this URL from this.mockResponseMap
   *
   * @param urlStr
   *    the URL to fetch a mock reponse for
   * @return any
   *    the mock response if there is one, undefined otherwise
   */
  private getMockData(urlStr: string): any {
    let key;
    if (this.mockResponseMap.has(urlStr)) {
      key = urlStr;
    } else {
      // didn't find an exact match for the url, try to match only the endpoint without namespace and parameters
      const url = new URL(urlStr);
      key = url.pathname.slice(environment.rest.nameSpace.length);
    }
    if (this.mockResponseMap.has(key)) {
      // parse and stringify to clone the object to ensure that any changes made
      // to it afterwards don't affect future calls
      return JSON.parse(JSON.stringify(this.mockResponseMap.get(key)));
    } else {
      return undefined;
    }
  }
}
