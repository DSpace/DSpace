import { BrowseDefinitionDataService } from './browse-definition-data.service';
import { followLink } from '../../shared/utils/follow-link-config.model';
import { EMPTY } from 'rxjs';
import { FindListOptions } from '../data/find-list-options.model';
import { getMockRemoteDataBuildService } from '../../shared/mocks/remote-data-build.service.mock';
import { RequestService } from '../data/request.service';
import { HALEndpointServiceStub } from '../../shared/testing/hal-endpoint-service.stub';
import { getMockObjectCacheService } from '../../shared/mocks/object-cache.service.mock';

describe(`BrowseDefinitionDataService`, () => {
  let requestService: RequestService;
  let service: BrowseDefinitionDataService;
  let findAllDataSpy;
  let searchDataSpy;
  const browsesEndpointURL = 'https://rest.api/browses';
  const halService: any = new HALEndpointServiceStub(browsesEndpointURL);

  const options = new FindListOptions();
  const linksToFollow = [
    followLink('entries'),
    followLink('items')
  ];

  function initTestService() {
    return new BrowseDefinitionDataService(
      requestService,
      getMockRemoteDataBuildService(),
      getMockObjectCacheService(),
      halService,
    );
  }

  beforeEach(() => {
    service = initTestService();
    findAllDataSpy = jasmine.createSpyObj('findAllData', {
      findAll: EMPTY,
    });
    searchDataSpy = jasmine.createSpyObj('searchData', {
      searchBy: EMPTY,
      getSearchByHref: EMPTY,
    });
    (service as any).findAllData = findAllDataSpy;
    (service as any).searchData = searchDataSpy;
  });

  describe('findByFields', () => {
    it(`should call searchByHref on searchData`, () => {
      service.findByFields(['test'], true, false, ...linksToFollow);
      expect(searchDataSpy.getSearchByHref).toHaveBeenCalled();
    });
  });
  describe('searchBy', () => {
    it(`should call searchBy on searchData`, () => {
      service.searchBy('test', options, true, false, ...linksToFollow);
      expect(searchDataSpy.searchBy).toHaveBeenCalledWith('test', options, true, false, ...linksToFollow);
    });
  });
  describe(`findAll`, () => {
    it(`should call findAll on findAllData`, () => {
      service.findAll(options, true, false, ...linksToFollow);
      expect(findAllDataSpy.findAll).toHaveBeenCalledWith(options, true, false, ...linksToFollow);
    });
  });



});
