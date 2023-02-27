/* eslint-disable max-classes-per-file */
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RemoteData } from './remote-data';
import { RequestService } from './request.service';
import { ConfigurationProperty } from '../shared/configuration-property.model';
import { CONFIG_PROPERTY } from '../shared/config-property.resource-type';
import { IdentifiableDataService } from './base/identifiable-data.service';
import { dataService } from './base/data-service.decorator';

@Injectable()
@dataService(CONFIG_PROPERTY)
/**
 * Data Service responsible for retrieving Configuration properties
 */
export class ConfigurationDataService extends IdentifiableDataService<ConfigurationProperty> {

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
  ) {
    super('properties', requestService, rdbService, objectCache, halService);
  }

  /**
   * Finds a configuration property by name
   * @param name
   */
  findByPropertyName(name: string): Observable<RemoteData<ConfigurationProperty>> {
    return this.findById(name);
  }
}
