import { Injectable } from '@angular/core';
import { RequestService } from './request.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { GetRequest, PostRequest } from './request.models';
import { Observable } from 'rxjs';
import { filter, find, map } from 'rxjs/operators';
import { hasValue, isNotEmpty } from '../../shared/empty.util';
import { Registration } from '../shared/registration.model';
import { getFirstCompletedRemoteData } from '../shared/operators';
import { ResponseParsingService } from './parsing.service';
import { GenericConstructor } from '../shared/generic-constructor';
import { RegistrationResponseParsingService } from './registration-response-parsing.service';
import { RemoteData } from './remote-data';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import { HttpHeaders } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
/**
 * Service that will register a new email address and request a token
 */
export class EpersonRegistrationService {

  protected linkPath = 'registrations';
  protected searchByTokenPath = '/search/findByToken?token=';

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected halService: HALEndpointService,
  ) {

  }

  /**
   * Retrieves the Registration endpoint
   */
  getRegistrationEndpoint(): Observable<string> {
    return this.halService.getEndpoint(this.linkPath);
  }

  /**
   * Retrieves the endpoint to search by registration token
   */
  getTokenSearchEndpoint(token: string): Observable<string> {
    return this.halService.getEndpoint(this.linkPath).pipe(
      filter((href: string) => isNotEmpty(href)),
      map((href: string) => `${href}${this.searchByTokenPath}${token}`));
  }

  /**
   * Register a new email address
   * @param email
   * @param captchaToken the value of x-recaptcha-token header
   */
  registerEmail(email: string, captchaToken: string = null, type?: string): Observable<RemoteData<Registration>> {
    const registration = new Registration();
    registration.email = email;

    const requestId = this.requestService.generateRequestId();

    const href$ = this.getRegistrationEndpoint();

    const options: HttpOptions = Object.create({});
    let headers = new HttpHeaders();
    if (captchaToken) {
      headers = headers.append('x-recaptcha-token', captchaToken);
    }
    options.headers = headers;

    if (hasValue(type)) {
      options.params = type ?
        new HttpParams({ fromString: 'accountRequestType=' + type }) : new HttpParams();
    }

    href$.pipe(
      find((href: string) => hasValue(href)),
      map((href: string) => {
        const request = new PostRequest(requestId, href, registration, options);
        this.requestService.send(request);
      })
    ).subscribe();

    return this.rdbService.buildFromRequestUUID<Registration>(requestId).pipe(
      getFirstCompletedRemoteData()
    );
  }

  /**
   * Search a registration based on the provided token
   * @param token
   */
  searchByToken(token: string): Observable<RemoteData<Registration>> {
    const requestId = this.requestService.generateRequestId();

    const href$ = this.getTokenSearchEndpoint(token).pipe(
      find((href: string) => hasValue(href)),
    );

    href$.subscribe((href: string) => {
      const request = new GetRequest(requestId, href);
      Object.assign(request, {
        getResponseParser(): GenericConstructor<ResponseParsingService> {
          return RegistrationResponseParsingService;
        }
      });
      this.requestService.send(request, true);
    });

    return this.rdbService.buildSingle<Registration>(href$).pipe(
      map((rd) => {
        if (rd.hasSucceeded && hasValue(rd.payload)) {
          return Object.assign(rd, { payload: Object.assign(rd.payload, { token }) });
        } else {
          return rd;
        }
      })
    );
  }

}
