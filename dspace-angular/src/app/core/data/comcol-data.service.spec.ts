import { HttpClient } from '@angular/common/http';
import { Store } from '@ngrx/store';
import { cold } from 'jasmine-marbles';
import { Observable, of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { getMockRequestService } from '../../shared/mocks/request.service.mock';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { Community } from '../shared/community.model';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { ComColDataService } from './comcol-data.service';
import { CommunityDataService } from './community-data.service';
import { DSOChangeAnalyzer } from './dso-change-analyzer.service';
import { RequestService } from './request.service';
import { createFailedRemoteDataObject, createFailedRemoteDataObject$, createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { BitstreamDataService } from './bitstream-data.service';
import { CoreState } from '../core-state.model';
import { FindListOptions } from './find-list-options.model';
import { Bitstream } from '../shared/bitstream.model';
import { testCreateDataImplementation } from './base/create-data.spec';
import { testFindAllDataImplementation } from './base/find-all-data.spec';
import { testSearchDataImplementation } from './base/search-data.spec';
import { testPatchDataImplementation } from './base/patch-data.spec';
import { testDeleteDataImplementation } from './base/delete-data.spec';

const LINK_NAME = 'test';

const scopeID = 'd9d30c0c-69b7-4369-8397-ca67c888974d';

const communitiesEndpoint = 'https://rest.api/core/communities';

const communityEndpoint = `${communitiesEndpoint}/${scopeID}`;

class TestService extends ComColDataService<any> {

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected store: Store<CoreState>,
    protected cds: CommunityDataService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
    protected http: HttpClient,
    protected bitstreamDataService: BitstreamDataService,
    protected comparator: DSOChangeAnalyzer<Community>,
    protected linkPath: string
  ) {
    super('something', requestService, rdbService, objectCache, halService, comparator, notificationsService, bitstreamDataService);
  }

  protected getFindByParentHref(parentUUID: string): Observable<string> {
    // implementation in subclasses for communities/collections
    return undefined;
  }

  protected getScopeCommunityHref(options: FindListOptions): Observable<string> {
    // implementation in subclasses for communities/collections
    return observableOf(communityEndpoint);
  }
}

/* eslint-disable @typescript-eslint/no-shadow */
describe('ComColDataService', () => {
  let service: TestService;
  let requestService: RequestService;
  let cds: CommunityDataService;
  let objectCache: ObjectCacheService;
  let halService: any = {};
  let bitstreamDataService: BitstreamDataService;
  let rdbService: RemoteDataBuildService;
  let testScheduler: TestScheduler;
  let topEndpoint: string;

  const store = {} as Store<CoreState>;
  const notificationsService = {} as NotificationsService;
  const http = {} as HttpClient;
  const comparator = {} as any;

  const options = Object.assign(new FindListOptions(), {
    scopeID: scopeID
  });
  const scopedEndpoint = `${communityEndpoint}/${LINK_NAME}`;

  const mockHalService = {
    getEndpoint: (linkPath) => observableOf(communitiesEndpoint)
  };

  function initRdbService(): RemoteDataBuildService {
    return jasmine.createSpyObj('rdbService', {
      buildSingle : createFailedRemoteDataObject$('Error', 500)
    });
  }

  function initBitstreamDataService(): BitstreamDataService {
    return jasmine.createSpyObj('bitstreamDataService', {
      deleteByHref: createSuccessfulRemoteDataObject$({})
    });
  }

  function initMockCommunityDataService(): CommunityDataService {
    return jasmine.createSpyObj('cds', {
      getEndpoint: cold('--a-', { a: communitiesEndpoint }),
      getIDHref: communityEndpoint
    });
  }

  function initMockObjectCacheService(): ObjectCacheService {
    return jasmine.createSpyObj('objectCache', {
      getObjectByUUID: cold('d-', {
        d: {
          _links: {
            [LINK_NAME]: {
              href: scopedEndpoint
            }
          }
        }
      })
    });
  }

  function initTestService(): TestService {
    return new TestService(
      requestService,
      rdbService,
      store,
      cds,
      objectCache,
      halService,
      notificationsService,
      http,
      bitstreamDataService,
      comparator,
      LINK_NAME
    );
  }

  const initTestScheduler = (): TestScheduler => {
    return new TestScheduler((actual, expected) => {
      expect(actual).toEqual(expected);
    });
  };

  beforeEach(() => {
    topEndpoint = 'https://rest.api/core/communities/search/top';
    testScheduler = initTestScheduler();
    cds = initMockCommunityDataService();
    requestService = getMockRequestService();
    objectCache = initMockObjectCacheService();
    bitstreamDataService = initBitstreamDataService();
    rdbService = initRdbService();
    halService = mockHalService;
    service = initTestService();
  });

  describe('composition', () => {
    const initService = () => new TestService(null, null, null, null, null, null, null, null, null, null, null);
    testCreateDataImplementation(initService);
    testFindAllDataImplementation(initService);
    testSearchDataImplementation(initService);
    testPatchDataImplementation(initService);
    testDeleteDataImplementation(initService);
  });

  describe('getBrowseEndpoint', () => {
    it(`should call createAndSendGetRequest with the scope Community's self link`, () => {
      testScheduler.run(({ cold, flush, expectObservable }) => {
        (cds.getEndpoint as jasmine.Spy).and.returnValue(cold('a', { a: communitiesEndpoint }));
        (rdbService.buildSingle as jasmine.Spy).and.returnValue(cold('a', { a: createFailedRemoteDataObject() }));
        spyOn(service as any, 'createAndSendGetRequest');
        service.getBrowseEndpoint(options);
        flush();
        expectObservable((service as any).createAndSendGetRequest.calls.argsFor(0)[0]).toBe('(a|)', { a: communityEndpoint });
        expect((service as any).createAndSendGetRequest.calls.argsFor(0)[1]).toBeTrue();
      });
    });

    describe('if the scope Community can\'t be found', () => {
      it('should throw an error', () => {
        testScheduler.run(({ cold, expectObservable }) => {
          // spies re-defined here to use the "cold" function from rxjs's TestScheduler
          // rather than the one imported from jasmine-marbles.
          // Mixing the two seems to lead to unpredictable results
          (cds.getEndpoint as jasmine.Spy).and.returnValue(cold('a', { a: communitiesEndpoint }));
          (rdbService.buildSingle as jasmine.Spy).and.returnValue(cold('a', { a: createFailedRemoteDataObject() }));
          const expectedError = new Error(`The Community with scope ${scopeID} couldn't be retrieved`);
          expectObservable(service.getBrowseEndpoint(options)).toBe('#', undefined, expectedError);
        });
      });
    });

  });

  describe('cache refresh', () => {
    let communityWithoutParentHref;
    let communityWithParentHref;

    beforeEach(() => {
      communityWithParentHref = {
        _links: {
          parentCommunity: {
            href: 'topLevel/parentCommunity'
          }
        }
      } as Community;
      communityWithoutParentHref = {
        _links: {}
      } as Community;
    });

    describe('cache refreshed top level community', () => {
      it(`should refresh the top level community cache when the dso has a parent link that can't be resolved`, () => {
        testScheduler.run(({ flush, cold }) => {
          spyOn(halService, 'getEndpoint').and.returnValue(cold('a', { a: topEndpoint }));
          spyOn(service, 'findByHref').and.returnValue(cold('a', { a: createSuccessfulRemoteDataObject({}) }));
          service.refreshCache(communityWithParentHref);
          flush();
          expect(requestService.setStaleByHrefSubstring).toHaveBeenCalledWith(topEndpoint);
        });
      });
      it(`shouldn't do anything when the dso doesn't have a parent link`, () => {
        testScheduler.run(({ flush, cold }) => {
          spyOn(halService, 'getEndpoint').and.returnValue(cold('a', { a: topEndpoint }));
          spyOn(service, 'findByHref').and.returnValue(cold('a', { a: createSuccessfulRemoteDataObject({}) }));
          service.refreshCache(communityWithoutParentHref);
          flush();
          expect(requestService.setStaleByHrefSubstring).not.toHaveBeenCalled();
        });
      });
    });

    describe('cache refreshed child community', () => {
      let parentCommunity: Community;
      beforeEach(() => {
        parentCommunity = Object.assign(new Community(), {
          uuid: 'a20da287-e174-466a-9926-f66as300d399',
          id: 'a20da287-e174-466a-9926-f66as300d399',
          metadata: [{
            key: 'dc.title',
            value: 'parent community'
          }],
          _links: {}
        });
      });
      it('should refresh a specific cached community when the parent link can be resolved', () => {
        testScheduler.run(({ flush, cold }) => {
          spyOn(halService, 'getEndpoint').and.returnValue(cold('a', { a: topEndpoint }));
          spyOn(service, 'findByHref').and.returnValue(cold('a', { a: createSuccessfulRemoteDataObject(parentCommunity) }));
          service.refreshCache(communityWithParentHref);
          flush();
          expect(requestService.setStaleByHrefSubstring).toHaveBeenCalledWith('a20da287-e174-466a-9926-f66as300d399');
        });
      });
    });
  });

  describe('deleteLogo', () => {
    let dso;

    beforeEach(() => {
      dso = {
        _links: {
          logo: {
            href: 'logo-href'
          }
        }
      };
    });

    describe('when DSO has no logo', () => {
      beforeEach(() => {
        dso.logo = undefined;
      });

      it('should return a failed RD', (done) => {
        service.deleteLogo(dso).subscribe(rd => {
          expect(rd.hasFailed).toBeTrue();
          expect(bitstreamDataService.deleteByHref).not.toHaveBeenCalled();
          done();
        });
      });
    });

    describe('when DSO has a logo', () => {
      let logo;

      beforeEach(() => {
        logo = Object.assign(new Bitstream, {
          id: 'logo-id',
          _links: {
            self: {
              href: 'logo-href',
            }
          }
        });
      });

      describe('that can be retrieved', () => {
        beforeEach(() => {
          dso.logo = createSuccessfulRemoteDataObject$(logo);
        });

        it('should call BitstreamDataService.deleteByHref', (done) => {
          service.deleteLogo(dso).subscribe(rd => {
            expect(rd.hasSucceeded).toBeTrue();
            expect(bitstreamDataService.deleteByHref).toHaveBeenCalledWith('logo-href');
            done();
          });
        });
      });

      describe('that cannot be retrieved', () => {
        beforeEach(() => {
          dso.logo = createFailedRemoteDataObject$(logo);
        });

        it('should not call BitstreamDataService.deleteByHref', (done) => {
          service.deleteLogo(dso).subscribe(rd => {
            expect(rd.hasFailed).toBeTrue();
            expect(bitstreamDataService.deleteByHref).not.toHaveBeenCalled();
            done();
          });
        });
      });
    });
  });
});
