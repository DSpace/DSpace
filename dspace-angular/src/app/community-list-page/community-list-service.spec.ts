import { inject, TestBed } from '@angular/core/testing';
import { Store } from '@ngrx/store';
import { of as observableOf } from 'rxjs';
import { take } from 'rxjs/operators';
import { AppState } from '../app.reducer';
import { SortDirection, SortOptions } from '../core/cache/models/sort-options.model';
import { buildPaginatedList } from '../core/data/paginated-list.model';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';
import { StoreMock } from '../shared/testing/store.mock';
import { CommunityListService, toFlatNode } from './community-list-service';
import { CollectionDataService } from '../core/data/collection-data.service';
import { CommunityDataService } from '../core/data/community-data.service';
import { Community } from '../core/shared/community.model';
import { Collection } from '../core/shared/collection.model';
import { PageInfo } from '../core/shared/page-info.model';
import { FlatNode } from './flat-node.model';
import { FindListOptions } from '../core/data/find-list-options.model';
import { APP_CONFIG } from 'src/config/app-config.interface';
import { environment } from 'src/environments/environment.test';

describe('CommunityListService', () => {
  let store: StoreMock<AppState>;
  const standardElementsPerPage = 2;
  let collectionDataServiceStub: any;
  let communityDataServiceStub: any;

  let service: CommunityListService;
  let mockSubcommunities1Page1;
  let mockCollectionsPage1;
  let mockCollectionsPage2;
  let mockListOfTopCommunitiesPage1;
  let mockListOfTopCommunitiesPage2;
  let mockTopCommunitiesWithChildrenArraysPage1;
  let mockTopCommunitiesWithChildrenArraysPage2;
  let allCommunities;
  function init() {
    mockSubcommunities1Page1 = [Object.assign(new Community(), {
      id: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88',
      uuid: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88',
    }),
      Object.assign(new Community(), {
        id: '59ee713b-ee53-4220-8c3f-9860dc84fe33',
        uuid: '59ee713b-ee53-4220-8c3f-9860dc84fe33',
      })
    ];
    mockCollectionsPage1 = [
      Object.assign(new Collection(), {
        id: 'e9dbf393-7127-415f-8919-55be34a6e9ed',
        uuid: 'e9dbf393-7127-415f-8919-55be34a6e9ed',
        name: 'Collection 1'
      }),
      Object.assign(new Collection(), {
        id: '59da2ff0-9bf4-45bf-88be-e35abd33f304',
        uuid: '59da2ff0-9bf4-45bf-88be-e35abd33f304',
        name: 'Collection 2'
      })
    ];
    mockCollectionsPage2 = [
      Object.assign(new Collection(), {
        id: 'a5159760-f362-4659-9e81-e3253ad91ede',
        uuid: 'a5159760-f362-4659-9e81-e3253ad91ede',
        name: 'Collection 3'
      }),
      Object.assign(new Collection(), {
        id: 'a392e16b-fcf2-400a-9a88-53ef7ecbdcd3',
        uuid: 'a392e16b-fcf2-400a-9a88-53ef7ecbdcd3',
        name: 'Collection 4'
      })
    ];
    mockListOfTopCommunitiesPage1 = [
      Object.assign(new Community(), {
        id: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
        uuid: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
        subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), mockSubcommunities1Page1)),
        collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
      }),
      Object.assign(new Community(), {
        id: '9076bd16-e69a-48d6-9e41-0238cb40d863',
        uuid: '9076bd16-e69a-48d6-9e41-0238cb40d863',
        subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
        collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [...mockCollectionsPage1, ...mockCollectionsPage2])),
      }),
      Object.assign(new Community(), {
        id: 'efbf25e1-2d8c-4c28-8f3e-2e04c215be24',
        uuid: 'efbf25e1-2d8c-4c28-8f3e-2e04c215be24',
        subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
        collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
      }),
    ];
    mockListOfTopCommunitiesPage2 = [
      Object.assign(new Community(), {
        id: 'c2e04392-3b8a-4dfa-976d-d76fb1b8a4b6',
        uuid: 'c2e04392-3b8a-4dfa-976d-d76fb1b8a4b6',
        subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
        collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
      }),
    ];
    mockTopCommunitiesWithChildrenArraysPage1 = [
      {
        id: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
        uuid: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
        subcommunities: mockSubcommunities1Page1,
        collections: [],
      },
      {
        id: '9076bd16-e69a-48d6-9e41-0238cb40d863',
        uuid: '9076bd16-e69a-48d6-9e41-0238cb40d863',
        subcommunities: [],
        collections: [...mockCollectionsPage1, ...mockCollectionsPage2],
      },
      {
        id: 'efbf25e1-2d8c-4c28-8f3e-2e04c215be24',
        uuid: 'efbf25e1-2d8c-4c28-8f3e-2e04c215be24',
        subcommunities: [],
        collections: [],
      }];
    mockTopCommunitiesWithChildrenArraysPage2 = [
      {
        id: 'c2e04392-3b8a-4dfa-976d-d76fb1b8a4b6',
        uuid: 'c2e04392-3b8a-4dfa-976d-d76fb1b8a4b6',
        subcommunities: [],
        collections: [],
      }];

    allCommunities = [...mockTopCommunitiesWithChildrenArraysPage1, ...mockTopCommunitiesWithChildrenArraysPage2, ...mockSubcommunities1Page1];

  }
  beforeEach(() => {
    init();
    communityDataServiceStub = {
      findTop(options: FindListOptions = {}) {
        const allTopComs = [...mockListOfTopCommunitiesPage1, ...mockListOfTopCommunitiesPage2];
        let currentPage = options.currentPage;
        const elementsPerPage = 3;
        if (currentPage === undefined) {
          currentPage = 1;
        }
        const startPageIndex = (currentPage - 1) * elementsPerPage;
        let endPageIndex = (currentPage * elementsPerPage);
        if (endPageIndex > allTopComs.length) {
          endPageIndex = allTopComs.length;
        }
        return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), allTopComs.slice(startPageIndex, endPageIndex)));
      },
      findByParent(parentUUID: string, options: FindListOptions = {}) {
        const foundCom = allCommunities.find((community) => (community.id === parentUUID));
        let currentPage = options.currentPage;
        let elementsPerPage = options.elementsPerPage;
        if (currentPage === undefined) {
          currentPage = 1;
        }
        if (elementsPerPage === 0) {
          return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), (foundCom.subcommunities as [Community])));
        }
        elementsPerPage = standardElementsPerPage;
        if (foundCom !== undefined && foundCom.subcommunities !== undefined) {
          const coms = foundCom.subcommunities as [Community];
          const startPageIndex = (currentPage - 1) * elementsPerPage;
          let endPageIndex = (currentPage * elementsPerPage);
          if (endPageIndex > coms.length) {
            endPageIndex = coms.length;
          }
          return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), coms.slice(startPageIndex, endPageIndex)));
        } else {
          return createFailedRemoteDataObject$();
        }
      }
    };
    collectionDataServiceStub = {
      findByParent(parentUUID: string, options: FindListOptions = {}) {
        const foundCom = allCommunities.find((community) => (community.id === parentUUID));
        let currentPage = options.currentPage;
        let elementsPerPage = options.elementsPerPage;
        if (currentPage === undefined) {
          currentPage = 1;
        }
        if (elementsPerPage === 0) {
          return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), (foundCom.collections as [Collection])));
        }
        elementsPerPage = standardElementsPerPage;
        if (foundCom !== undefined && foundCom.collections !== undefined) {
          const colls = foundCom.collections as [Collection];
          const startPageIndex = (currentPage - 1) * elementsPerPage;
          let endPageIndex = (currentPage * elementsPerPage);
          if (endPageIndex > colls.length) {
            endPageIndex = colls.length;
          }
          return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), colls.slice(startPageIndex, endPageIndex)));
        } else {
          return createFailedRemoteDataObject$();
        }
      }
    };
    TestBed.configureTestingModule({
      providers: [CommunityListService,
        { provide: APP_CONFIG, useValue: environment },
        { provide: CollectionDataService, useValue: collectionDataServiceStub },
        { provide: CommunityDataService, useValue: communityDataServiceStub },
        { provide: Store, useValue: StoreMock },
      ],
    });
    store = TestBed.inject(Store as any);
    service = new CommunityListService(environment, communityDataServiceStub, collectionDataServiceStub, store);
  });

  it('should create', inject([CommunityListService], (serviceIn: CommunityListService) => {
    expect(serviceIn).toBeTruthy();
  }));

  describe('getNextPageTopCommunities', () => {
    describe('also load in second page of top communities', () => {
      let flatNodeList;
      describe('None expanded: should return list containing only flatnodes of the test top communities page 1 and 2', () => {
        let findTopSpy;
        beforeEach((done) => {
          findTopSpy = spyOn(communityDataServiceStub, 'findTop').and.callThrough();

          service.loadCommunities({
            currentPage: 2,
            sort: new SortOptions('dc.title', SortDirection.ASC)
          }, null)
            .pipe(take(1))
            .subscribe((value) => {
              flatNodeList = value;
              done();
            });
        });
        it('flatnode list should contain just flatnodes of top community list page 1 and 2', () => {
          expect(findTopSpy).toHaveBeenCalled();
          expect(flatNodeList.length).toEqual(mockListOfTopCommunitiesPage1.length + mockListOfTopCommunitiesPage2.length);
          mockListOfTopCommunitiesPage1.map((community) => {
            expect(flatNodeList.find((flatnode) => (flatnode.id === community.id))).toBeTruthy();
          });
          mockListOfTopCommunitiesPage2.map((community) => {
            expect(flatNodeList.find((flatnode) => (flatnode.id === community.id))).toBeTruthy();
          });
        });
      });
    });
  });

  describe('loadCommunities', () => {
    describe('should transform all communities in a list of flatnodes with possible subcoms and collections as subflatnodes if they\'re expanded', () => {
      let flatNodeList;
      describe('None expanded: should return list containing only flatnodes of the test top communities', () => {
        beforeEach((done) => {
          service.loadCommunities({
            currentPage: 1,
            sort: new SortOptions('dc.title', SortDirection.ASC)
          }, null)
            .pipe(take(1))
            .subscribe((value) => {
              flatNodeList = value;
              done();
            });
        });
        it('length of flatnode list should be as big as top community list', () => {
          expect(flatNodeList.length).toEqual(mockListOfTopCommunitiesPage1.length);
        });
        it('flatnode list should contain flatNode representations of top communities', () => {
          mockListOfTopCommunitiesPage1.map((community) => {
            expect(flatNodeList.find((flatnode) => (flatnode.id === community.id))).toBeTruthy();
          });
        });
        it('none of the flatnodes in the list should be expanded', () => {
          flatNodeList.map((flatnode: FlatNode) => {
            expect(flatnode.isExpanded).toEqual(false);
          });
        });
      });
      describe('All top expanded, all page 1: should return list containing flatnodes of the communities in the test list and all its possible page-limited children (subcommunities and collections)', () => {
        beforeEach((done) => {
          const expandedNodes = [];
          mockListOfTopCommunitiesPage1.map((community: Community) => {
            const communityFlatNode = toFlatNode(community, observableOf(true), 0, true, null);
            communityFlatNode.currentCollectionPage = 1;
            communityFlatNode.currentCommunityPage = 1;
            expandedNodes.push(communityFlatNode);
          });
          service.loadCommunities({
            currentPage: 1,
            sort: new SortOptions('dc.title', SortDirection.ASC)
          }, expandedNodes)
            .pipe(take(1))
            .subscribe((value) => {
              flatNodeList = value;
              done();
            });
        });
        it('length of flatnode list should be as big as top community list and size of its possible page-limited children', () => {
          expect(flatNodeList.length).toEqual(mockListOfTopCommunitiesPage1.length + mockSubcommunities1Page1.length + mockSubcommunities1Page1.length);
        });
        it('flatnode list should contain flatNode representations of all page-limited children', () => {
          mockSubcommunities1Page1.map((subcommunity) => {
            expect(flatNodeList.find((flatnode) => (flatnode.id === subcommunity.id))).toBeTruthy();
          });
          mockCollectionsPage1.map((collection) => {
            expect(flatNodeList.find((flatnode) => (flatnode.id === collection.id))).toBeTruthy();
          });
        });
      });
      describe('Just first top comm expanded, all page 1: should return list containing flatnodes of the communities in the test list and all its possible page-limited children (subcommunities and collections)', () => {
        beforeEach((done) => {
          const communityFlatNode = toFlatNode(mockListOfTopCommunitiesPage1[0], observableOf(true), 0, true, null);
          communityFlatNode.currentCollectionPage = 1;
          communityFlatNode.currentCommunityPage = 1;
          const expandedNodes = [communityFlatNode];
          service.loadCommunities({
            currentPage: 1,
            sort: new SortOptions('dc.title', SortDirection.ASC)
          }, expandedNodes)
            .pipe(take(1))
            .subscribe((value) => {
              flatNodeList = value;
              done();
            });
        });
        it('length of flatnode list should be as big as top community list and size of page-limited children of first top community', () => {
          expect(flatNodeList.length).toEqual(mockListOfTopCommunitiesPage1.length + mockSubcommunities1Page1.length);
        });
        it('flatnode list should contain flatNode representations of all page-limited children of first top community', () => {
          mockSubcommunities1Page1.map((subcommunity) => {
            expect(flatNodeList.find((flatnode) => (flatnode.id === subcommunity.id))).toBeTruthy();
          });
        });
      });
      describe('Just second top comm expanded, collections at page 2: should return list containing flatnodes of the communities in the test list and all its possible page-limited children (subcommunities and collections)', () => {
        beforeEach((done) => {
          const communityFlatNode = toFlatNode(mockListOfTopCommunitiesPage1[1], observableOf(true), 0, true, null);
          communityFlatNode.currentCollectionPage = 2;
          communityFlatNode.currentCommunityPage = 1;
          const expandedNodes = [communityFlatNode];
          service.loadCommunities({
            currentPage: 1,
            sort: new SortOptions('dc.title', SortDirection.ASC)
          }, expandedNodes)
            .pipe(take(1))
            .subscribe((value) => {
              flatNodeList = value;
              done();
            });
        });
        it('length of flatnode list should be as big as top community list and size of page-limited children of second top community', () => {
          expect(flatNodeList.length).toEqual(mockListOfTopCommunitiesPage1.length + mockCollectionsPage1.length + mockCollectionsPage2.length);
        });
        it('flatnode list should contain flatNode representations of all page-limited children of first top community', () => {
          mockCollectionsPage1.map((collection) => {
            expect(flatNodeList.find((flatnode) => (flatnode.id === collection.id))).toBeTruthy();
          });
          mockCollectionsPage2.map((collection) => {
            expect(flatNodeList.find((flatnode) => (flatnode.id === collection.id))).toBeTruthy();
          });
        });
      });
    });
  });

  describe('transformListOfCommunities', () => {
    describe('should transform list of communities in a list of flatnodes with possible subcoms and collections as subflatnodes if they\'re expanded', () => {
      describe('list of communities with possible children', () => {
        let listOfCommunities;
        beforeEach(() => {
          listOfCommunities = mockListOfTopCommunitiesPage1;
        });
        let flatNodeList;
        describe('None expanded: should return list containing only flatnodes of the communities in the test list', () => {
          beforeEach((done) => {
            service.transformListOfCommunities(buildPaginatedList(new PageInfo(), listOfCommunities), 0, null, null)
              .pipe(take(1))
              .subscribe((value) => {
                flatNodeList = value;
                done();
              });
          });
          it('length of flatnode list should be as big as community test list', () => {
            expect(flatNodeList.length).toEqual(listOfCommunities.length);
          });
          it('flatnode list should contain flatNode representations of all communities from test list', () => {
            listOfCommunities.map((community) => {
              expect(flatNodeList.find((flatnode) => (flatnode.id === community.id))).toBeTruthy();
            });
          });
          it('none of the flatnodes in the list should be expanded', () => {
            flatNodeList.map((flatnode: FlatNode) => {
              expect(flatnode.isExpanded).toEqual(false);
            });
          });
        });
        describe('All top expanded, all page 1: should return list containing flatnodes of the communities in the test list and all its possible page-limited children (subcommunities and collections)', () => {
          beforeEach((done) => {
            const expandedNodes = [];
            listOfCommunities.map((community: Community) => {
              const communityFlatNode = toFlatNode(community, observableOf(true), 0, true, null);
              communityFlatNode.currentCollectionPage = 1;
              communityFlatNode.currentCommunityPage = 1;
              expandedNodes.push(communityFlatNode);
            });
            service.transformListOfCommunities(buildPaginatedList(new PageInfo(), listOfCommunities), 0, null, expandedNodes)
              .pipe(take(1))
              .subscribe((value) => {
                flatNodeList = value;
                done();
              });
          });
          it('length of flatnode list should be as big as community test list and size of its possible children', () => {
            expect(flatNodeList.length).toEqual(listOfCommunities.length + mockSubcommunities1Page1.length + mockSubcommunities1Page1.length);
          });
          it('flatnode list should contain flatNode representations of all children', () => {
            mockSubcommunities1Page1.map((subcommunity) => {
              expect(flatNodeList.find((flatnode) => (flatnode.id === subcommunity.id))).toBeTruthy();
            });
            mockSubcommunities1Page1.map((collection) => {
              expect(flatNodeList.find((flatnode) => (flatnode.id === collection.id))).toBeTruthy();
            });
          });
        });
      });
    });

  });

  describe('transformCommunity', () => {
    describe('should transform community in list of flatnodes with possible subcoms and collections as subflatnodes if its expanded', () => {
      describe('topcommunity without subcoms or collections, unexpanded', () => {
        const communityWithNoSubcomsOrColls = Object.assign(new Community(), {
          id: 'efbf25e1-2d8c-4c28-8f3e-2e04c215be24',
          uuid: 'efbf25e1-2d8c-4c28-8f3e-2e04c215be24',
          subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
          collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
          metadata: {
            'dc.description': [{ language: 'en_US', value: 'no subcoms, 2 coll' }],
            'dc.title': [{ language: 'en_US', value: 'Community 2' }]
          }
        });
        let flatNodeList;
        describe('should return list containing only flatnode corresponding to that community', () => {
          beforeEach((done) => {
            service.transformCommunity(communityWithNoSubcomsOrColls, 0, null, null)
              .pipe(take(1))
              .subscribe((value) => {
                flatNodeList = value;
                done();
              });
          });
          it('length of flatnode list should be 1', () => {
            expect(flatNodeList.length).toEqual(1);
          });
          it('flatnode list only element should be flatNode of test community', () => {
            expect(flatNodeList[0].id).toEqual(communityWithNoSubcomsOrColls.id);
          });
          it('flatnode from test community is not expanded', () => {
            expect(flatNodeList[0].isExpanded).toEqual(false);
          });
        });
      });
      describe('topcommunity with subcoms or collections, unexpanded', () => {
        const communityWithSubcoms = Object.assign(new Community(), {
          id: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
          uuid: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
          subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), mockSubcommunities1Page1)),
          collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
          metadata: {
            'dc.description': [{ language: 'en_US', value: '2 subcoms, no coll' }],
            'dc.title': [{ language: 'en_US', value: 'Community 1' }]
          }
        });
        let flatNodeList;
        describe('should return list containing only flatnode corresponding to that community', () => {
          beforeEach((done) => {
            service.transformCommunity(communityWithSubcoms, 0, null, null)
              .pipe(take(1))
              .subscribe((value) => {
                flatNodeList = value;
                done();
              });

          });
          it('length of flatnode list should be 1', () => {
            expect(flatNodeList.length).toEqual(1);
          });
          it('flatnode list only element should be flatNode of test community', () => {
            expect(flatNodeList[0].id).toEqual(communityWithSubcoms.id);
          });
          it('flatnode from test community is not expanded', () => {
            expect(flatNodeList[0].isExpanded).toEqual(false);
          });
        });
      });
      describe('topcommunity with subcoms, expanded, first page for all', () => {
        describe('should return list containing flatnodes of that community, its possible subcommunities and its possible collections', () => {
          const communityWithSubcoms = Object.assign(new Community(), {
            id: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
            uuid: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
            subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), mockSubcommunities1Page1)),
            collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
            metadata: {
              'dc.description': [{ language: 'en_US', value: '2 subcoms, no coll' }],
              'dc.title': [{ language: 'en_US', value: 'Community 1' }]
            }
          });
          let flatNodeList;
          beforeEach((done) => {
            const communityFlatNode = toFlatNode(communityWithSubcoms, observableOf(true), 0, true, null);
            communityFlatNode.currentCollectionPage = 1;
            communityFlatNode.currentCommunityPage = 1;
            const expandedNodes = [communityFlatNode];
            service.transformCommunity(communityWithSubcoms, 0, null, expandedNodes)
              .pipe(take(1))
              .subscribe((value) => {
                flatNodeList = value;
                done();
              });
          });
          it('list of flatnodes is length is  1 + nrOfSubcoms & first flatnode is of expanded test community', () => {
            expect(flatNodeList.length).toEqual(1 + mockSubcommunities1Page1.length);
            expect(flatNodeList[0].isExpanded).toEqual(true);
            expect(flatNodeList[0].id).toEqual(communityWithSubcoms.id);
          });
          it('list of flatnodes contains flatnodes for all subcoms of test community', () => {
            mockSubcommunities1Page1.map((subcommunity) => {
              expect(flatNodeList.find((flatnode) => (flatnode.id === subcommunity.id))).toBeTruthy();
            });
          });
          it('the subcoms of the test community are a level higher than the parent community', () => {
            mockSubcommunities1Page1.map((subcommunity) => {
              expect((flatNodeList.find((flatnode) => (flatnode.id === subcommunity.id))).level).toEqual(flatNodeList[0].level + 1);
            });
          });
        });
      });
      describe('topcommunity with collections, expanded, on second page of collections', () => {
        describe('should return list containing flatnodes of that community, its collections of the first two pages', () => {
          let communityWithCollections;
          let flatNodeList;
          beforeEach((done) => {
            communityWithCollections = Object.assign(new Community(), {
              id: '9076bd16-e69a-48d6-9e41-0238cb40d863',
              uuid: '9076bd16-e69a-48d6-9e41-0238cb40d863',
              subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
              collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [...mockCollectionsPage1, ...mockCollectionsPage2])),
              metadata: {
                'dc.description': [{ language: 'en_US', value: '2 subcoms, no coll' }],
                'dc.title': [{ language: 'en_US', value: 'Community 1' }]
              }
            });
            const communityFlatNode = toFlatNode(communityWithCollections, observableOf(true), 0, true, null);
            communityFlatNode.currentCollectionPage = 2;
            communityFlatNode.currentCommunityPage = 1;
            const expandedNodes = [communityFlatNode];
            service.transformCommunity(communityWithCollections, 0, null, expandedNodes)
              .pipe(take(1))
              .subscribe((value) => {
                flatNodeList = value;
                done();
              });
          });
          it('list of flatnodes is length is  1 + nrOfCollections & first flatnode is of expanded test community', () => {
            expect(flatNodeList.length).toEqual(1 + mockCollectionsPage1.length + mockCollectionsPage2.length);
            expect(flatNodeList[0].isExpanded).toEqual(true);
            expect(flatNodeList[0].id).toEqual(communityWithCollections.id);
          });
          it('list of flatnodes contains flatnodes for all subcolls (first 2 pages) of test community', () => {
            mockCollectionsPage1.map((collection) => {
              expect(flatNodeList.find((flatnode) => (flatnode.id === collection.id))).toBeTruthy();
            });
            mockCollectionsPage2.map((collection) => {
              expect(flatNodeList.find((flatnode) => (flatnode.id === collection.id))).toBeTruthy();
            });
          });
          it('the collections of the test community are a level higher than the parent community', () => {
            mockCollectionsPage1.map((collection) => {
              expect((flatNodeList.find((flatnode) => (flatnode.id === collection.id))).level).toEqual(flatNodeList[0].level + 1);
            });
            mockCollectionsPage2.map((collection) => {
              expect((flatNodeList.find((flatnode) => (flatnode.id === collection.id))).level).toEqual(flatNodeList[0].level + 1);
            });
          });
        });
      });
    });

  });

  describe('getIsExpandable', () => {
    describe('should return true', () => {
      it('if community has subcommunities', (done) => {
        const communityWithSubcoms = Object.assign(new Community(), {
          id: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
          uuid: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
          subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), mockSubcommunities1Page1)),
          collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
          metadata: {
            'dc.description': [{ language: 'en_US', value: '2 subcoms, no coll' }],
            'dc.title': [{ language: 'en_US', value: 'Community 1' }]
          }
        });
        service.getIsExpandable(communityWithSubcoms).pipe(take(1)).subscribe((result) => {
          expect(result).toEqual(true);
          done();
        });
      });
      it('if community has collections', (done) => {
        const communityWithCollections = Object.assign(new Community(), {
          id: '9076bd16-e69a-48d6-9e41-0238cb40d863',
          uuid: '9076bd16-e69a-48d6-9e41-0238cb40d863',
          subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
          collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), mockCollectionsPage1)),
          metadata: {
            'dc.description': [{ language: 'en_US', value: 'no subcoms, 2 coll' }],
            'dc.title': [{ language: 'en_US', value: 'Community 2' }]
          }
        });
        service.getIsExpandable(communityWithCollections).pipe(take(1)).subscribe((result) => {
          expect(result).toEqual(true);
          done();
        });
      });
    });
    describe('should return false', () => {
      it('if community has neither subcommunities nor collections', (done) => {
        const communityWithNoSubcomsOrColls = Object.assign(new Community(), {
          id: 'efbf25e1-2d8c-4c28-8f3e-2e04c215be24',
          uuid: 'efbf25e1-2d8c-4c28-8f3e-2e04c215be24',
          subcommunities: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
          collections: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
          metadata: {
            'dc.description': [{ language: 'en_US', value: 'no subcoms, no coll' }],
            'dc.title': [{ language: 'en_US', value: 'Community 3' }]
          }
        });
        service.getIsExpandable(communityWithNoSubcomsOrColls).pipe(take(1)).subscribe((result) => {
          expect(result).toEqual(false);
          done();
        });
      });
    });

  });

});
