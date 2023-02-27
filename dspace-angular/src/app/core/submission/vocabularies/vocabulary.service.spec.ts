import { cold, getTestScheduler, hot } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { RequestService } from '../../data/request.service';
import { RequestParam } from '../../cache/models/request-param.model';
import { PageInfo } from '../../shared/page-info.model';
import { buildPaginatedList } from '../../data/paginated-list.model';
import {
  createSuccessfulRemoteDataObject,
  createSuccessfulRemoteDataObject$
} from '../../../shared/remote-data.utils';
import { RestResponse } from '../../cache/response.models';
import { VocabularyService } from './vocabulary.service';
import { getMockRequestService } from '../../../shared/mocks/request.service.mock';
import { getMockRemoteDataBuildService } from '../../../shared/mocks/remote-data-build.service.mock';
import { VocabularyOptions } from './models/vocabulary-options.model';
import { VocabularyFindOptions } from './models/vocabulary-find-options.model';
import { HrefOnlyDataService } from '../../data/href-only-data.service';
import { getMockHrefOnlyDataService } from '../../../shared/mocks/href-only-data.service.mock';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { RequestEntry } from '../../data/request-entry.model';
import { VocabularyDataService } from './vocabulary.data.service';
import { VocabularyEntryDetailsDataService } from './vocabulary-entry-details.data.service';

describe('VocabularyService', () => {
  let scheduler: TestScheduler;
  let service: VocabularyService;
  let requestService: RequestService;
  let rdbService: RemoteDataBuildService;
  let objectCache: ObjectCacheService;
  let halService: HALEndpointService;
  let hrefOnlyDataService: HrefOnlyDataService;
  let responseCacheEntry: RequestEntry;

  const vocabulary: any = {
    id: 'types',
    name: 'types',
    scrollable: true,
    hierarchical: false,
    preloadLevel: 1,
    type: 'vocabulary',
    uuid: 'vocabulary-types',
    entries: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    _links: {
      self: {
        href: 'https://rest.api/rest/api/submission/vocabularies/types'
      },
      entries: {
        href: 'https://rest.api/rest/api/submission/vocabularies/types/entries'
      },
    }
  };

  const hierarchicalVocabulary: any = {
    id: 'srsc',
    name: 'srsc',
    scrollable: false,
    hierarchical: true,
    preloadLevel: 2,
    type: 'vocabulary',
    uuid: 'vocabulary-srsc',
    entries: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    _links: {
      self: {
        href: 'https://rest.api/rest/api/submission/vocabularies/types'
      },
      entries: {
        href: 'https://rest.api/rest/api/submission/vocabularies/types/entries'
      },
    }
  };

  const vocabularyEntry: any = {
    display: 'testValue1',
    value: 'testValue1',
    otherInformation: {},
    type: 'vocabularyEntry'
  };

  const vocabularyEntry2: any = {
    display: 'testValue2',
    value: 'testValue2',
    otherInformation: {},
    type: 'vocabularyEntry'
  };

  const vocabularyEntry3: any = {
    display: 'testValue3',
    value: 'testValue3',
    otherInformation: {},
    type: 'vocabularyEntry'
  };

  const vocabularyEntryParentDetail: any = {
    authority: 'authorityId2',
    display: 'testParent',
    value: 'testParent',
    otherInformation: {
      id: 'authorityId2',
      hasChildren: 'true',
      note: 'Familjeforskning'
    },
    type: 'vocabularyEntryDetail',
    _links: {
      self: {
        href: 'https://rest.api/rest/api/submission/vocabularyEntryDetails/srsc:VR131402'
      },
      parent: {
        href: 'https://rest.api/rest/api/submission/vocabularyEntryDetails/srsc:parent'
      },
      children: {
        href: 'https://rest.api/rest/api/submission/vocabularyEntryDetails/srsc:children'
      }
    }
  };

  const vocabularyEntryChildDetail: any = {
    authority: 'authoritytestChild1',
    display: 'testChild1',
    value: 'testChild1',
    otherInformation: {
      id: 'authoritytestChild1',
      hasChildren: 'true',
      note: 'Familjeforskning'
    },
    type: 'vocabularyEntryDetail',
    _links: {
      self: {
        href: 'https://rest.api/rest/api/submission/vocabularyEntryDetails/srsc:authoritytestChild1'
      },
      parent: {
        href: 'https://rest.api/rest/api/submission/vocabularyEntryDetails/srsc:parent'
      },
      children: {
        href: 'https://rest.api/rest/api/submission/vocabularyEntryDetails/srsc:children'
      }
    }
  };

  const vocabularyEntryChild2Detail: any = {
    authority: 'authoritytestChild2',
    display: 'testChild2',
    value: 'testChild2',
    otherInformation: {
      id: 'authoritytestChild2',
      hasChildren: 'true',
      note: 'Familjeforskning'
    },
    type: 'vocabularyEntryDetail',
    _links: {
      self: {
        href: 'https://rest.api/rest/api/submission/vocabularyEntryDetails/srsc:authoritytestChild2'
      },
      parent: {
        href: 'https://rest.api/rest/api/submission/vocabularyEntryDetails/srsc:parent'
      },
      children: {
        href: 'https://rest.api/rest/api/submission/vocabularyEntryDetails/srsc:children'
      }
    }
  };

  const endpointURL = `https://rest.api/rest/api/submission/vocabularies`;
  const requestURL = `https://rest.api/rest/api/submission/vocabularies/${vocabulary.id}`;
  const entryDetailEndpointURL = `https://rest.api/rest/api/submission/vocabularyEntryDetails`;
  const entryDetailRequestURL = `https://rest.api/rest/api/submission/vocabularyEntryDetails/${hierarchicalVocabulary.id}:testValue`;
  const entryDetailParentRequestURL = `https://rest.api/rest/api/submission/vocabularyEntryDetails/${hierarchicalVocabulary.id}:testValue/parent`;
  const entryDetailChildrenRequestURL = `https://rest.api/rest/api/submission/vocabularyEntryDetails/${hierarchicalVocabulary.id}:testValue/children`;
  const requestUUID = '8b3c613a-5a4b-438b-9686-be1d5b4a1c5a';
  const vocabularyId = 'types';
  const metadata = 'dc.type';
  const collectionUUID = '8b39g7ya-5a4b-438b-851f-be1d5b4a1c5a';
  const entryID = 'dsfsfsdf-5a4b-438b-851f-be1d5b4a1c5a';
  const searchRequestURL = `https://rest.api/rest/api/submission/vocabularies/search/byMetadataAndCollection?metadata=${metadata}&collection=${collectionUUID}`;
  const entriesRequestURL = `https://rest.api/rest/api/submission/vocabularies/${vocabulary.id}/entries`;
  const entriesByValueRequestURL = `https://rest.api/rest/api/submission/vocabularies/${vocabulary.id}/entries?filter=test&exact=false`;
  const entryByValueRequestURL = `https://rest.api/rest/api/submission/vocabularies/${vocabulary.id}/entries?filter=test&exact=true`;
  const entryByIDRequestURL = `https://rest.api/rest/api/submission/vocabularies/${vocabulary.id}/entries?entryID=${entryID}`;
  const vocabularyOptions: VocabularyOptions = {
    name: vocabularyId,
    closed: false
  };
  const pageInfo = new PageInfo();
  const array = [vocabulary, hierarchicalVocabulary];
  const arrayEntries = [vocabularyEntry, vocabularyEntry2, vocabularyEntry3];
  const childrenEntries = [vocabularyEntryChildDetail, vocabularyEntryChild2Detail];
  const paginatedList = buildPaginatedList(pageInfo, array);
  const paginatedListEntries = buildPaginatedList(pageInfo, arrayEntries);
  const childrenPaginatedList = buildPaginatedList(pageInfo, childrenEntries);
  const vocabularyRD = createSuccessfulRemoteDataObject(vocabulary);
  const vocabularyRD$ = createSuccessfulRemoteDataObject$(vocabulary);
  const vocabularyEntriesRD = createSuccessfulRemoteDataObject$(paginatedListEntries);
  const vocabularyEntryDetailParentRD = createSuccessfulRemoteDataObject(vocabularyEntryParentDetail);
  const vocabularyEntryChildrenRD = createSuccessfulRemoteDataObject(childrenPaginatedList);
  const paginatedListRD = createSuccessfulRemoteDataObject(paginatedList);
  const getRequestEntries$ = (successful: boolean) => {
    return observableOf({
      response: { isSuccessful: successful, payload: arrayEntries } as any
    } as RequestEntry);
  };

  function initTestService() {
    hrefOnlyDataService = getMockHrefOnlyDataService();

    return new VocabularyService(
      requestService,
      new VocabularyDataService(requestService, rdbService, objectCache, halService),
      new VocabularyEntryDetailsDataService(requestService, rdbService, objectCache, halService),
    );
  }

  describe('vocabularies endpoint', () => {
    beforeEach(() => {
      scheduler = getTestScheduler();

      halService = jasmine.createSpyObj('halService', {
        getEndpoint: cold('a', { a: endpointURL }),
      });
    });

    afterEach(() => {
      service = null;
    });

    describe('vocabularies', () => {
      beforeEach(() => {
        responseCacheEntry = new RequestEntry();
        responseCacheEntry.request = { href: 'https://rest.api/' } as any;
        responseCacheEntry.response = new RestResponse(true, 200, 'Success');

        requestService = jasmine.createSpyObj('requestService', {
          generateRequestId: requestUUID,
          send: true,
          removeByHrefSubstring: {},
          getByHref: observableOf(responseCacheEntry),
          getByUUID: observableOf(responseCacheEntry),
        });
        rdbService = jasmine.createSpyObj('rdbService', {
          buildSingle: hot('a|', {
            a: vocabularyRD
          }),
          buildList: hot('a|', {
            a: paginatedListRD,
          }),
        });

        service = initTestService();

        spyOn((service as any).vocabularyDataService, 'findById').and.callThrough();
        spyOn((service as any).vocabularyDataService, 'findAll').and.callThrough();
        spyOn((service as any).vocabularyDataService, 'findByHref').and.callThrough();
        spyOn((service as any).vocabularyDataService.findAllData, 'getFindAllHref').and.returnValue(observableOf(entriesRequestURL));
      });

      afterEach(() => {
        service = null;
      });

      describe('findVocabularyById', () => {
        it('should proxy the call to vocabularyDataService.findById', () => {
          scheduler.schedule(() => service.findVocabularyById(vocabularyId));
          scheduler.flush();

          expect((service as any).vocabularyDataService.findById).toHaveBeenCalledWith(vocabularyId, true, true);
        });

        it('should return a RemoteData<Vocabulary> for the object with the given id', () => {
          const result = service.findVocabularyById(vocabularyId);
          const expected = cold('a|', {
            a: vocabularyRD
          });
          expect(result).toBeObservable(expected);
        });
      });

      describe('findVocabularyByHref', () => {
        it('should proxy the call to vocabularyDataService.findVocabularyByHref', () => {
          scheduler.schedule(() => service.findVocabularyByHref(requestURL));
          scheduler.flush();

          expect((service as any).vocabularyDataService.findByHref).toHaveBeenCalledWith(requestURL, true, true);
        });

        it('should return a RemoteData<Vocabulary> for the object with the given URL', () => {
          const result = service.findVocabularyByHref(requestURL);
          const expected = cold('a|', {
            a: vocabularyRD
          });
          expect(result).toBeObservable(expected);
        });
      });

      describe('findAllVocabularies', () => {
        it('should proxy the call to vocabularyDataService.findAllVocabularies', () => {
          scheduler.schedule(() => service.findAllVocabularies());
          scheduler.flush();

          expect((service as any).vocabularyDataService.findAll).toHaveBeenCalled();
        });

        it('should return a RemoteData<PaginatedList<Vocabulary>>', () => {
          const result = service.findAllVocabularies();
          const expected = cold('a|', {
            a: paginatedListRD
          });
          expect(result).toBeObservable(expected);
        });
      });
    });

    describe('vocabulary entries', () => {

      beforeEach(() => {
        requestService = getMockRequestService(getRequestEntries$(true));
        rdbService = getMockRemoteDataBuildService(undefined, vocabularyEntriesRD);
        spyOn(rdbService, 'buildList').and.callThrough();
        service = initTestService();
        spyOn(service, 'findVocabularyById').and.returnValue(vocabularyRD$);
      });

      describe('getVocabularyEntries', () => {

        it('should call findVocabularyById with options containing no query, filter, exact or entryID', () => {
          scheduler.schedule(() => service.getVocabularyEntries(vocabularyOptions, pageInfo).subscribe());
          scheduler.flush();

          expect(service.findVocabularyById).toHaveBeenCalledWith(vocabularyOptions.name, true, true, jasmine.objectContaining({
            findListOptions: jasmine.objectContaining({
              query: null,
              filter: null,
              exact: null,
              entryID: null,
            })
          }));
        });
      });

      describe('getVocabularyEntriesByValue', () => {

        it('should call findVocabularyById with options containing the correct filter and exact', () => {
          scheduler.schedule(() => service.getVocabularyEntriesByValue('test', false, vocabularyOptions, pageInfo).subscribe());
          scheduler.flush();

          expect(service.findVocabularyById).toHaveBeenCalledWith(vocabularyOptions.name, true, true, jasmine.objectContaining({
            findListOptions: jasmine.objectContaining({
              filter: 'test',
              exact: false,
            })
          }));
        });
      });

      describe('getVocabularyEntryByValue', () => {

        it('should call findVocabularyById with options containing the correct filter and exact', () => {
          scheduler.schedule(() => service.getVocabularyEntryByValue('test', vocabularyOptions).subscribe());
          scheduler.flush();

          expect(service.findVocabularyById).toHaveBeenCalledWith(vocabularyOptions.name, true, true, jasmine.objectContaining({
            findListOptions: jasmine.objectContaining({
              filter: 'test',
              exact: true,
            })
          }));
        });
      });

      describe('getVocabularyEntryByID', () => {
        it('should call findVocabularyById with options containing the entryID', () => {
          scheduler.schedule(() => service.getVocabularyEntryByID(entryID, vocabularyOptions).subscribe());
          scheduler.flush();

          expect(service.findVocabularyById).toHaveBeenCalledWith(vocabularyOptions.name, true, true, jasmine.objectContaining({
            findListOptions: jasmine.objectContaining({
              entryID,
            })
          }));
        });
      });

    });

  });

  describe('vocabularyEntryDetails endpoint', () => {
    beforeEach(() => {
      scheduler = getTestScheduler();

      halService = jasmine.createSpyObj('halService', {
        getEndpoint: cold('a', { a: entryDetailEndpointURL })
      });

      responseCacheEntry = new RequestEntry();
      responseCacheEntry.request = { href: 'https://rest.api/' } as any;
      responseCacheEntry.response = new RestResponse(true, 200, 'Success');

      requestService = jasmine.createSpyObj('requestService', {
        generateRequestId: requestUUID,
        send: true,
        removeByHrefSubstring: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: observableOf(responseCacheEntry),
      });
      rdbService = jasmine.createSpyObj('rdbService', {
        buildSingle: hot('a|', {
          a: vocabularyEntryDetailParentRD
        }),
        buildList: hot('a|', {
          a: vocabularyEntryChildrenRD
        }),
      });

      service = initTestService();

      spyOn((service as any).vocabularyEntryDetailDataService, 'findById').and.callThrough();
      spyOn((service as any).vocabularyEntryDetailDataService, 'findAll').and.callThrough();
      spyOn((service as any).vocabularyEntryDetailDataService, 'findByHref').and.callThrough();
      spyOn((service as any).vocabularyEntryDetailDataService, 'findListByHref').and.callThrough();
      spyOn((service as any).vocabularyEntryDetailDataService, 'searchBy').and.callThrough();
      spyOn((service as any).vocabularyEntryDetailDataService.searchData, 'getSearchByHref').and.returnValue(observableOf(searchRequestURL));
      spyOn((service as any).vocabularyEntryDetailDataService.findAllData, 'getFindAllHref').and.returnValue(observableOf(entryDetailChildrenRequestURL));
      spyOn((service as any).vocabularyEntryDetailDataService, 'getBrowseEndpoint').and.returnValue(observableOf(entryDetailEndpointURL));
    });

    afterEach(() => {
      service = null;
    });

    describe('findEntryDetailByHref', () => {
      it('should proxy the call to vocabularyDataService.findEntryDetailByHref', () => {
        scheduler.schedule(() => service.findEntryDetailByHref(entryDetailRequestURL));
        scheduler.flush();

        expect((service as any).vocabularyEntryDetailDataService.findByHref).toHaveBeenCalledWith(entryDetailRequestURL, true, true);
      });

      it('should return a RemoteData<VocabularyEntryDetail> for the object with the given URL', () => {
        const result = service.findEntryDetailByHref(entryDetailRequestURL);
        const expected = cold('a|', {
          a: vocabularyEntryDetailParentRD
        });
        expect(result).toBeObservable(expected);
      });
    });

    describe('findEntryDetailById', () => {
      it('should proxy the call to vocabularyDataService.findVocabularyById', () => {
        scheduler.schedule(() => service.findEntryDetailById('testValue', hierarchicalVocabulary.id));
        scheduler.flush();
        const expectedId = `${hierarchicalVocabulary.id}:testValue`;
        expect((service as any).vocabularyEntryDetailDataService.findById).toHaveBeenCalledWith(expectedId, true, true);
      });

      it('should return a RemoteData<VocabularyEntryDetail> for the object with the given id', () => {
        const result = service.findEntryDetailById('testValue', hierarchicalVocabulary.id);
        const expected = cold('a|', {
          a: vocabularyEntryDetailParentRD
        });
        expect(result).toBeObservable(expected);
      });
    });

    describe('getEntryDetailParent', () => {
      it('should proxy the call to vocabularyDataService.getEntryDetailParent', () => {
        scheduler.schedule(() => service.getEntryDetailParent('testValue', hierarchicalVocabulary.id).subscribe());
        scheduler.flush();

        expect((service as any).vocabularyEntryDetailDataService.findByHref).toHaveBeenCalledWith(entryDetailParentRequestURL, true, true);
      });

      it('should return a RemoteData<VocabularyEntryDetail> for the object with the given URL', () => {
        const result = service.getEntryDetailParent('testValue', hierarchicalVocabulary.id);
        const expected = cold('a|', {
          a: vocabularyEntryDetailParentRD
        });
        expect(result).toBeObservable(expected);
      });
    });

    describe('getEntryDetailChildren', () => {
      it('should proxy the call to vocabularyDataService.getEntryDetailChildren', () => {
        const options: VocabularyFindOptions = new VocabularyFindOptions(
          null,
          null,
          null,
          null,
          pageInfo.elementsPerPage,
          pageInfo.currentPage
        );
        scheduler.schedule(() => service.getEntryDetailChildren('testValue', hierarchicalVocabulary.id, pageInfo).subscribe());
        scheduler.flush();

        expect((service as any).vocabularyEntryDetailDataService.findListByHref).toHaveBeenCalledWith(entryDetailChildrenRequestURL, options, true, true);
      });

      it('should return a RemoteData<PaginatedList<ResourcePolicy>> for the object with the given URL', () => {
        const result = service.getEntryDetailChildren('testValue', hierarchicalVocabulary.id, new PageInfo());
        const expected = cold('a|', {
          a: vocabularyEntryChildrenRD
        });
        expect(result).toBeObservable(expected);
      });
    });

    describe('searchByTop', () => {
      it('should proxy the call to vocabularyEntryDetailDataService.searchBy', () => {
        const options: VocabularyFindOptions = new VocabularyFindOptions(
          null,
          null,
          null,
          null,
          pageInfo.elementsPerPage,
          pageInfo.currentPage
        );
        options.searchParams = [new RequestParam('vocabulary', 'srsc')];
        scheduler.schedule(() => service.searchTopEntries('srsc', pageInfo));
        scheduler.flush();

        expect((service as any).vocabularyEntryDetailDataService.searchBy).toHaveBeenCalledWith((service as any).searchTopMethod, options, true, true);
      });

      it('should return a RemoteData<PaginatedList<ResourcePolicy>> for the search', () => {
        const result = service.searchTopEntries('srsc', pageInfo);
        const expected = cold('a|', {
          a: vocabularyEntryChildrenRD
        });
        expect(result).toBeObservable(expected);
      });

    });

    describe('clearSearchTopRequests', () => {
      it('should remove requests on the data service\'s endpoint', (done) => {
        service.clearSearchTopRequests();

        expect(requestService.removeByHrefSubstring).toHaveBeenCalledWith(`search/${(service as any).searchTopMethod}`);
        done();
      });
    });

  });
});
