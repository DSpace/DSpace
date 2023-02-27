import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { DspaceRestService } from '../core/dspace-rest/dspace-rest.service';
import { RawRestResponse } from '../core/dspace-rest/raw-rest-response.model';
import { HALEndpointService } from '../core/shared/hal-endpoint.service';

@Injectable({
    providedIn: 'root'
})
export class HealthService {
    constructor(protected halService: HALEndpointService,
        protected restService: DspaceRestService) {
        }
    /**
     * @returns health data
     */
    getHealth(): Observable<RawRestResponse> {
       return this.halService.getEndpoint('/actuator').pipe(
           map((restURL: string) => restURL + '/health'),
           switchMap((endpoint: string) => this.restService.get(endpoint)));
    }

    /**
     * @returns information of server
     */
    getInfo(): Observable<RawRestResponse> {
        return this.halService.getEndpoint('/actuator').pipe(
            map((restURL: string) => restURL + '/info'),
            switchMap((endpoint: string) => this.restService.get(endpoint)));
    }
}
