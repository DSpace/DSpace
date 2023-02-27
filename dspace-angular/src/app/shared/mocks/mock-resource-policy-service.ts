import { ResourcePolicyDataService } from '../../core/resource-policy/resource-policy-data.service';

export function getMockResourcePolicyService(): ResourcePolicyDataService {
  return jasmine.createSpyObj('resourcePolicyService', {
    searchByResource: jasmine.createSpy('searchByResource'),
    create: jasmine.createSpy('create'),
    delete: jasmine.createSpy('delete'),
    update: jasmine.createSpy('update')
  });
}
