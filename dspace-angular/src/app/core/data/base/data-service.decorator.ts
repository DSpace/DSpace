/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { InjectionToken } from '@angular/core';
import { CacheableObject } from '../../cache/cacheable-object.model';
import { ResourceType } from '../../shared/resource-type';
import { GenericConstructor } from '../../shared/generic-constructor';
import { hasNoValue, hasValue } from '../../../shared/empty.util';
import { HALDataService } from './hal-data-service.interface';

export const DATA_SERVICE_FACTORY = new InjectionToken<(resourceType: ResourceType) => GenericConstructor<HALDataService<any>>>('getDataServiceFor', {
  providedIn: 'root',
  factory: () => getDataServiceFor,
});
const dataServiceMap = new Map();

/**
 * A class decorator to indicate that this class is a data service for a given HAL resource type.
 *
 * In most cases, a data service should extend {@link BaseDataService}.
 * At the very least it must implement {@link HALDataService} in order for it to work with {@link LinkService}.
 *
 * @param resourceType the resource type the class is a dataservice for
 */
export function dataService(resourceType: ResourceType) {
  return (target: GenericConstructor<HALDataService<any>>): void => {
    if (hasNoValue(resourceType)) {
      throw new Error(`Invalid @dataService annotation on ${target}, resourceType needs to be defined`);
    }
    const existingDataservice = dataServiceMap.get(resourceType.value);

    if (hasValue(existingDataservice)) {
      throw new Error(`Multiple dataservices for ${resourceType.value}: ${existingDataservice} and ${target}`);
    }

    dataServiceMap.set(resourceType.value, target);
  };
}

/**
 * Return the dataservice matching the given resource type
 *
 * @param resourceType the resource type you want the matching dataservice for
 */
export function getDataServiceFor<T extends CacheableObject>(resourceType: ResourceType): GenericConstructor<HALDataService<any>> {
  return dataServiceMap.get(resourceType.value);
}
