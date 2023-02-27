import { TestBed, waitForAsync } from '@angular/core/testing';

import { TestScheduler } from 'rxjs/testing';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { cold, getTestScheduler, hot } from 'jasmine-marbles';

import { VocabularyTreeviewService } from './vocabulary-treeview.service';
import { VocabularyService } from '../../../core/submission/vocabularies/vocabulary.service';
import { TranslateLoaderMock } from '../../mocks/translate-loader.mock';
import { VocabularyOptions } from '../../../core/submission/vocabularies/models/vocabulary-options.model';
import { LOAD_MORE_NODE, LOAD_MORE_ROOT_NODE, TreeviewFlatNode, TreeviewNode } from './vocabulary-treeview-node.model';
import { PageInfo } from '../../../core/shared/page-info.model';
import { VocabularyEntryDetail } from '../../../core/submission/vocabularies/models/vocabulary-entry-detail.model';
import { buildPaginatedList } from '../../../core/data/paginated-list.model';
import { createSuccessfulRemoteDataObject } from '../../remote-data.utils';
import { VocabularyEntry } from '../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { expand, map, switchMap } from 'rxjs/operators';
import { from as observableFrom } from 'rxjs';

describe('VocabularyTreeviewService test suite', () => {

  let scheduler: TestScheduler;
  let service: VocabularyTreeviewService;
  let serviceAsAny: any;
  let loadMoreNode: TreeviewNode;
  let loadMoreRootNode: TreeviewNode;
  let loadMoreRootFlatNode: TreeviewFlatNode;
  let item: VocabularyEntryDetail;
  let itemNode: TreeviewNode;
  let item2: VocabularyEntryDetail;
  let itemNode2: TreeviewNode;
  let item3: VocabularyEntryDetail;
  let itemNode3: TreeviewNode;
  let item5: VocabularyEntryDetail;
  let itemNode5: TreeviewNode;
  let child: VocabularyEntryDetail;
  let childNode: TreeviewNode;
  let child2: VocabularyEntryDetail;
  let childNode2: TreeviewNode;
  let childEntry3: VocabularyEntry;
  let child3: VocabularyEntryDetail;
  let childNode3: TreeviewNode;
  let searchItemNode: TreeviewNode;
  let searchChildNode: TreeviewNode;
  let searchChildNode3: TreeviewNode;
  let initValueChildNode: TreeviewNode;
  let initValueChildNode2: TreeviewNode;

  let treeNodeList: TreeviewNode[];
  let treeNodeListWithChildren: TreeviewNode[];
  let treeNodeListWithLoadMore: TreeviewNode[];
  let treeNodeListWithLoadMoreRoot: TreeviewNode[];
  let nodeMap: Map<string, TreeviewNode>;
  let nodeMapWithChildren: Map<string, TreeviewNode>;
  let searchNodeMap: Map<string, TreeviewNode>;
  let vocabularyOptions;
  let pageInfo: PageInfo;

  const vocabularyServiceStub = jasmine.createSpyObj('VocabularyService', {
    getVocabularyEntriesByValue: jasmine.createSpy('getVocabularyEntriesByValue'),
    getEntryDetailParent: jasmine.createSpy('getEntryDetailParent'),
    findEntryDetailById: jasmine.createSpy('findEntryDetailById'),
    searchTopEntries: jasmine.createSpy('searchTopEntries'),
    getEntryDetailChildren: jasmine.createSpy('getEntryDetailChildren'),
    clearSearchTopRequests: jasmine.createSpy('clearSearchTopRequests')
  });

  function init() {

    pageInfo = Object.assign(new PageInfo(), {
      elementsPerPage: 1,
      totalElements: 3,
      totalPages: 1,
      currentPage: 1
    });
    loadMoreNode = new TreeviewNode(LOAD_MORE_NODE, false, new PageInfo(), item);
    loadMoreRootNode = new TreeviewNode(LOAD_MORE_ROOT_NODE, false, new PageInfo(), null);
    loadMoreRootFlatNode = new TreeviewFlatNode(LOAD_MORE_ROOT_NODE, 1, false, false, new PageInfo(), null);
    item = new VocabularyEntryDetail();
    item.id = 'vocabularyTest:root1';
    item.value = item.display = 'root1';
    item.otherInformation = { hasChildren: 'true', id: 'root1' };
    itemNode = new TreeviewNode(item, true, pageInfo);
    searchItemNode = new TreeviewNode(item, true, new PageInfo(), null, true);

    item2 = new VocabularyEntryDetail();
    item2.id = 'vocabularyTest:root2';
    item2.value = item2.display = 'root2';
    item2.otherInformation = { id: 'root2' };
    itemNode2 = new TreeviewNode(item2, false, pageInfo);

    item3 = new VocabularyEntryDetail();
    item3.id = 'vocabularyTest:root3';
    item3.value = item3.display = 'root3';
    item3.otherInformation = { id: 'root3' };
    itemNode3 = new TreeviewNode(item3, false, pageInfo);

    child = new VocabularyEntryDetail();
    child.id = 'vocabularyTest:root1-child1';
    child.value = child.display = 'root1-child1';
    child.otherInformation = { parent: 'root1', hasChildren: 'true', id: 'root1-child1' };
    childNode = new TreeviewNode(child);
    searchChildNode = new TreeviewNode(child, true, new PageInfo(), item, true);

    childEntry3 = new VocabularyEntry();
    childEntry3.value = childEntry3.display = 'root1-child1-child1';
    childEntry3.otherInformation = { parent: 'root1-child1', id: 'root1-child1-child1' };
    child3 = new VocabularyEntryDetail();
    child3.id = 'vocabularyTest:root1-child1-child1';
    child3.value = child3.display = 'root1-child1-child1';
    child3.otherInformation = { parent: 'root1-child1', id: 'root1-child1-child1' };
    childNode3 = new TreeviewNode(child3);
    searchChildNode3 = new TreeviewNode(child3, false, new PageInfo(), child, true);

    child2 = new VocabularyEntryDetail();
    child2.id = 'vocabularyTest:root1-child2';
    child2.value = child2.display = 'root1-child2';
    child2.otherInformation = { parent: 'root1', id: 'root1-child2' };
    childNode2 = new TreeviewNode(child2, true);
    initValueChildNode2 = new TreeviewNode(child2, false, new PageInfo(), item, false, true);
    initValueChildNode = new TreeviewNode(child, true, new PageInfo(), item, false, true);
    initValueChildNode.childrenChange.next([initValueChildNode2]);

    item5 = new VocabularyEntryDetail();
    item5.id = item5.value = item5.display = 'root4';
    item5.otherInformation = { id: 'root4' };
    itemNode5 = new TreeviewNode(item5);

    treeNodeList = [
      itemNode,
      itemNode2,
      itemNode3
    ];
    treeNodeListWithChildren = [
      itemNode,
      itemNode2,
      itemNode3,
      childNode
    ];
    treeNodeListWithLoadMoreRoot = treeNodeList;
    treeNodeListWithLoadMore = treeNodeListWithChildren;
    treeNodeListWithLoadMoreRoot.push(loadMoreRootNode);
    treeNodeListWithLoadMore.push(loadMoreNode);

    nodeMap = new Map<string, TreeviewNode>([
      [item.id, itemNode],
      [item2.id, itemNode2],
      [item3.id, itemNode3]
    ]);

    nodeMapWithChildren = new Map<string, TreeviewNode>([
      [item.id, itemNode],
      [item2.id, itemNode2],
      [item3.id, itemNode3],
      [child.id, childNode],
    ]);

    searchNodeMap = new Map<string, TreeviewNode>([
      [item.id, searchItemNode],
    ]);
    vocabularyOptions = new VocabularyOptions('vocabularyTest', false);
  }

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      providers: [
        { provide: VocabularyService, useValue: vocabularyServiceStub },
        VocabularyTreeviewService,
        TranslateService
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    service = TestBed.inject(VocabularyTreeviewService);
    serviceAsAny = service;
    scheduler = getTestScheduler();
    init();
  });

  describe('initialize', () => {
    it('should set vocabularyName and call retrieveTopNodes method', () => {
      serviceAsAny.vocabularyService.searchTopEntries.and.returnValue(hot('-a', {
        a: createSuccessfulRemoteDataObject(buildPaginatedList(pageInfo, [item, item2, item3]))
      }));

      scheduler.schedule(() => service.initialize(vocabularyOptions, pageInfo));
      scheduler.flush();

      expect(serviceAsAny.vocabularyName).toEqual(vocabularyOptions.name);
      expect(serviceAsAny.pageInfo).toEqual(pageInfo);
      expect(serviceAsAny.dataChange.value).toEqual([itemNode, itemNode2, itemNode3]);
    });

    it('should set initValueHierarchy', () => {
      serviceAsAny.vocabularyService.searchTopEntries.and.returnValue(hot('-c', {
        a: createSuccessfulRemoteDataObject(buildPaginatedList(pageInfo, [item, item2, item3]))
      }));
      serviceAsAny.vocabularyService.findEntryDetailById.and.returnValue(
        hot('-a', {
          a: createSuccessfulRemoteDataObject(child2)
        })
      );
      serviceAsAny.vocabularyService.getEntryDetailParent.and.returnValue(
        hot('-b', {
          b: createSuccessfulRemoteDataObject(item)
        })
      );
      scheduler.schedule(() => service.initialize(vocabularyOptions, pageInfo, 'root2'));
      scheduler.flush();

      expect(serviceAsAny.vocabularyName).toEqual(vocabularyOptions.name);
      expect(serviceAsAny.initValueHierarchy).toEqual(['root1', 'root1-child2']);
    });
  });

  describe('getData', () => {
    it('should return dataChange', () => {
      const result = service.getData();

      expect(result).toEqual(serviceAsAny.dataChange);
    });
  });

  describe('loadMoreRoot', () => {
    it('should call retrieveTopNodes properly', () => {
      spyOn(serviceAsAny, 'retrieveTopNodes');
      service.initialize(vocabularyOptions, new PageInfo());
      serviceAsAny.dataChange.next(treeNodeListWithLoadMoreRoot);
      service.loadMoreRoot(loadMoreRootFlatNode);

      expect(serviceAsAny.retrieveTopNodes).toHaveBeenCalledWith(loadMoreRootFlatNode.pageInfo, treeNodeList);
    });
  });

  describe('loadMore', () => {

    beforeEach(() => {
      init();
      itemNode.childrenChange.next([childNode]);
    });

    it('should add children nodes properly', () => {
      pageInfo = Object.assign(new PageInfo(), {
        elementsPerPage: 1,
        totalElements: 2,
        totalPages: 2,
        currentPage: 2
      });
      spyOn(serviceAsAny, 'getChildrenNodesByParent').and.returnValue(hot('a', {
        a: buildPaginatedList(pageInfo, [child2])
      }));

      serviceAsAny.dataChange.next(treeNodeListWithLoadMore);
      serviceAsAny.nodeMap = nodeMapWithChildren;
      treeNodeListWithChildren.push(new TreeviewNode(child2, false, new PageInfo(), item));

      scheduler.schedule(() => service.loadMore(item));
      scheduler.flush();

      expect(serviceAsAny.dataChange.value).toEqual(treeNodeListWithChildren);
    });

    it('should add loadMore node properly', () => {
      pageInfo = Object.assign(new PageInfo(), {
        elementsPerPage: 1,
        totalElements: 2,
        totalPages: 2,
        currentPage: 1
      });
      spyOn(serviceAsAny, 'getChildrenNodesByParent').and.returnValue(hot('a', {
        a: buildPaginatedList(pageInfo, [child2])
      }));

      serviceAsAny.dataChange.next(treeNodeListWithLoadMore);
      serviceAsAny.nodeMap = nodeMapWithChildren;
      treeNodeListWithChildren.push(childNode2);
      treeNodeListWithChildren.push(loadMoreNode);

      scheduler.schedule(() => service.loadMore(item));
      scheduler.flush();

      expect(serviceAsAny.dataChange.value).toEqual(treeNodeListWithChildren);
    });

  });

  describe('searchByQuery', () => {
    it('should set tree data properly after a search', () => {
      pageInfo = Object.assign(new PageInfo(), {
        elementsPerPage: 1,
        totalElements: 1,
        totalPages: 1,
        currentPage: 1
      });
      serviceAsAny.vocabularyService.getVocabularyEntriesByValue.and.returnValue(hot('-a', {
        a: createSuccessfulRemoteDataObject(buildPaginatedList(pageInfo, [childEntry3]))
      }));

      serviceAsAny.vocabularyService.findEntryDetailById.and.returnValue(hot('-a', {
        a: createSuccessfulRemoteDataObject(child3)
      }));

      serviceAsAny.vocabularyService.getEntryDetailParent.and.returnValues(
        hot('-a', {
          a: createSuccessfulRemoteDataObject(child)
        }),
        hot('-b', {
          b: createSuccessfulRemoteDataObject(item)
        })
      );
      vocabularyOptions.query = 'root1-child1-child1';

      scheduler.schedule(() => service.searchByQuery(vocabularyOptions));
      scheduler.flush();

      // We can't check the tree by comparing root TreeviewNodes directly in this particular test;
      // Since RxJs 7, BehaviorSubjects can no longer be reliably compared because of the new currentObservers property
      // (see https://github.com/ReactiveX/rxjs/pull/6842)
      const levels$ = serviceAsAny.dataChange.pipe(
        expand((nodes: TreeviewNode[]) => {         // recursively apply:
          return observableFrom(nodes).pipe(        //   for each node in the array...
            switchMap(node => node.childrenChange)  //   ...map it to the array its child nodes.
          );                                        // because we only have one child per node in this case,
        }),                                         // this results in an array of nodes for each level of the tree.
        map((nodes: TreeviewNode[]) => nodes.map(node => node.item)), // finally, replace nodes with their vocab entries
      );

      // Confirm that this corresponds to the hierarchy we set up above
      expect(levels$).toBeObservable(cold('-(abcd)', {
        a: [item],
        b: [child],
        c: [child3],
        d: []           // ensure that grandchild has no children & the recursion stopped there
      }));
    });
  });

  describe('restoreNodes', () => {
    it('should restore nodes properly', () => {
      serviceAsAny.storedNodes = treeNodeList;
      serviceAsAny.storedNodeMap = nodeMap;
      serviceAsAny.nodeMap = searchNodeMap;

      service.restoreNodes();

      expect(serviceAsAny.nodeMap).toEqual(nodeMap);
      expect(serviceAsAny.dataChange.value).toEqual(treeNodeList);
    });
  });
});
