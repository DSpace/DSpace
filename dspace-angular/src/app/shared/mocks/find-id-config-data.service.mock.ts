import { createSuccessfulRemoteDataObject$ } from '../remote-data.utils';
import { ConfigurationProperty } from '../../core/shared/configuration-property.model';

export function getMockFindByIdDataService(propertyKey: string, ...values: string[]) {
  return jasmine.createSpyObj('findByIdDataService', {
    findByPropertyName: createSuccessfulRemoteDataObject$({
      ... new ConfigurationProperty(),
      name: propertyKey,
      values: values,
    })
  });
}


