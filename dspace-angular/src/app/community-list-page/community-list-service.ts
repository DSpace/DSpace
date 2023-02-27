/* eslint-disable max-classes-per-file */
import { Inject, Injectable } from '@angular/core';
import { createSelector, Store } from '@ngrx/store';

import { combineLatest as observableCombineLatest, Observable, of as observableOf } from 'rxjs';
import { filter, map, switchMap } from 'rxjs/operators';

import { AppState } from '../app.reducer';
import { CommunityDataService } from '../core/data/community-data.service';
import { Community } from '../core/shared/community.model';
import { Collection } from '../core/shared/collection.model';
import { PageInfo } from '../core/shared/page-info.model';
import { hasValue, isNotEmpty } from '../shared/empty.util';
import { RemoteData } from '../core/data/remote-data';
import { buildPaginatedList, PaginatedList } from '../core/data/paginated-list.model';
import { CollectionDataService } from '../core/data/collection-data.service';
import { CommunityListSaveAction } from './community-list.actions';
import { CommunityListState } from './community-list.reducer';
import { getCommunityPageRoute } from '../community-page/community-page-routing-paths';
import { getCollectionPageRoute } from '../collection-page/collection-page-routing-paths';
import { getFirstCompletedRemoteData, getFirstSucceededRemoteData } from '../core/shared/operators';
import { followLink } from '../shared/utils/follow-link-config.model';
import { FlatNode } from './flat-node.model';
import { ShowMoreFlatNode } from './show-more-flat-node.model';
import { FindListOptions } from '../core/data/find-list-options.model';
import { AppConfig, APP_CONFIG } from 'src/config/app-config.interface';

// Helper method to combine an flatten an array of observables of flatNode arrays
export const combineAndFlatten = (obsList: Observable<FlatNode[]>[]): Observable<FlatNode[]> =>
  observableCombineLatest([...obsList]).pipe(
    map((matrix: any[][]) => [].concat(...matrix)),
    filter((arr: any[]) => arr.every((e) => hasValue(e))),
  );

/**
 * Creates a flatNode from a community or collection
 * @param c               The community or collection this flatNode represents
 * @param isExpandable    Whether or not this node is expandable (true if it has children)
 * @param level           Level indicating how deep in the tree this node should be rendered
 * @param isExpanded      Whether or not this node already is expanded
 * @param parent          Parent of this node (flatNode representing its parent community)
 */
export const toFlatNode = (
  c: Community | Collection,
  isExpandable: Observable<boolean>,
  level: number,
  isExpanded: boolean,
  parent?: FlatNode
): FlatNode => ({
  isExpandable$: isExpandable,
  name: c.name,
  id: c.id,
  level: level,
  isExpanded,
  parent,
  payload: c,
  isShowMoreNode: false,
  route: c instanceof Community ? getCommunityPageRoute(c.id) : getCollectionPageRoute(c.id),
});

/**
 * Creates a show More flatnode where only the level and parent are of importance
 */
export const showMoreFlatNode = (
  id: string,
  level: number,
  parent: FlatNode
): FlatNode => ({
  isExpandable$: observableOf(false),
  name: 'Show More Flatnode',
  id: id,
  level: level,
  isExpanded: false,
  parent: parent,
  payload: new ShowMoreFlatNode(),
  isShowMoreNode: true,
});

// Selectors the get the communityList data out of the store
const communityListStateSelector = (state: AppState) => state.communityList;
const expandedNodesSelector = createSelector(communityListStateSelector, (communityList: CommunityListState) => communityList.expandedNodes);
const loadingNodeSelector = createSelector(communityListStateSelector, (communityList: CommunityListState) => communityList.loadingNode);

/**
 * Service class for the community list, responsible for the creating of the flat list used by communityList dataSource
 *  and connection to the store to retrieve and save the state of the community list
 */
@Injectable()
export class CommunityListService {

  private pageSize: number;

  constructor(
    @Inject(APP_CONFIG) protected appConfig: AppConfig,
    private communityDataService: CommunityDataService,
    private collectionDataService: CollectionDataService,
    private store: Store<any>
  ) {
    this.pageSize = appConfig.communityList.pageSize;
  }

  private configOnePage: FindListOptions = Object.assign(new FindListOptions(), {
    elementsPerPage: 1
  });

  saveCommunityListStateToStore(expandedNodes: FlatNode[], loadingNode: FlatNode): void {
    this.store.dispatch(new CommunityListSaveAction(expandedNodes, loadingNode));
  }

  getExpandedNodesFromStore(): Observable<FlatNode[]> {
    return this.store.select(expandedNodesSelector);
  }

  getLoadingNodeFromStore(): Observable<FlatNode> {
    return this.store.select(loadingNodeSelector);
  }

  /**
   * Gets all top communities, limited by page, and transforms this in a list of flatNodes.
   * @param findOptions       FindListOptions
   * @param expandedNodes     List of expanded nodes; if a node is not expanded its subCommunities and collections need
   *                            not be added to the list
   */
  loadCommunities(findOptions: FindListOptions, expandedNodes: FlatNode[]): Observable<FlatNode[]> {
    const currentPage = findOptions.currentPage;
    const topCommunities = [];
    for (let i = 1; i <= currentPage; i++) {
      const pagination: FindListOptions = Object.assign({}, findOptions, { currentPage: i });
      topCommunities.push(this.getTopCommunities(pagination));
    }
    const topComs$ = observableCombineLatest([...topCommunities]).pipe(
      map((coms: PaginatedList<Community>[]) => {
        const newPages: Community[][] = coms.map((unit: PaginatedList<Community>) => unit.page);
        const newPage: Community[] = [].concat(...newPages);
        let newPageInfo = new PageInfo();
        if (coms && coms.length > 0) {
          newPageInfo = Object.assign({}, coms[0].pageInfo, { currentPage });
        }
        return buildPaginatedList(newPageInfo, newPage);
      })
    );
    return topComs$.pipe(
      switchMap((topComs: PaginatedList<Community>) => this.transformListOfCommunities(topComs, 0, null, expandedNodes)),
      // distinctUntilChanged((a: FlatNode[], b: FlatNode[]) => a.length === b.length)
    );
  }

  /**
   * Puts the initial top level communities in a list to be called upon
   */
  private getTopCommunities(options: FindListOptions): Observable<PaginatedList<Community>> {
    return this.communityDataService.findTop({
        currentPage: options.currentPage,
        elementsPerPage: this.pageSize,
        sort: {
          field: options.sort.field,
          direction: options.sort.direction
        }
      },
      followLink('subcommunities', { findListOptions: this.configOnePage }),
      followLink('collections', { findListOptions: this.configOnePage }))
      .pipe(
        getFirstSucceededRemoteData(),
        map((results) => results.payload),
      );
  }

  /**
   * Transforms a list of communities to a list of FlatNodes according to the instructions detailed in transformCommunity
   * @param listOfPaginatedCommunities    Paginated list of communities to be transformed
   * @param level                         Level the tree is currently at
   * @param parent                        FlatNode of the parent of this list of communities
   * @param expandedNodes                 List of expanded nodes; if a node is not expanded its subcommunities and collections need not be added to the list
   */
  public transformListOfCommunities(listOfPaginatedCommunities: PaginatedList<Community>,
                                    level: number,
                                    parent: FlatNode,
                                    expandedNodes: FlatNode[]): Observable<FlatNode[]> {
    if (isNotEmpty(listOfPaginatedCommunities.page)) {
      let currentPage = listOfPaginatedCommunities.currentPage;
      if (isNotEmpty(parent)) {
        currentPage = expandedNodes.find((node: FlatNode) => node.id === parent.id).currentCommunityPage;
      }
      let obsList = listOfPaginatedCommunities.page
        .map((community: Community) => {
          return this.transformCommunity(community, level, parent, expandedNodes);
        });
      if (currentPage < listOfPaginatedCommunities.totalPages && currentPage === listOfPaginatedCommunities.currentPage) {
        obsList = [...obsList, observableOf([showMoreFlatNode('community', level, parent)])];
      }

      return combineAndFlatten(obsList);
    } else {
      return observableOf([]);
    }
  }

  /**
   * Transforms a community in a list of FlatNodes containing firstly a flatnode of the community itself,
   *      followed by flatNodes of its possible subcommunities and collection
   * It gets called recursively for each subcommunity to add its subcommunities and collections to the list
   * Number of subcommunities and collections added, is dependant on the current page the parent is at for respectively subcommunities and collections.
   * @param community         Community being transformed
   * @param level             Depth of the community in the list, subcommunities and collections go one level deeper
   * @param parent            Flatnode of the parent community
   * @param expandedNodes     List of nodes which are expanded, if node is not expanded, it need not add its page-limited subcommunities or collections
   */
  public transformCommunity(community: Community, level: number, parent: FlatNode, expandedNodes: FlatNode[]): Observable<FlatNode[]> {
    let isExpanded = false;
    if (isNotEmpty(expandedNodes)) {
      isExpanded = hasValue(expandedNodes.find((node) => (node.id === community.id)));
    }

    const isExpandable$ = this.getIsExpandable(community);

    const communityFlatNode = toFlatNode(community, isExpandable$, level, isExpanded, parent);

    let obsList = [observableOf([communityFlatNode])];

    if (isExpanded) {
      const currentCommunityPage = expandedNodes.find((node: FlatNode) => node.id === community.id).currentCommunityPage;
      let subcoms = [];
      for (let i = 1; i <= currentCommunityPage; i++) {
        const nextSetOfSubcommunitiesPage = this.communityDataService.findByParent(community.uuid, {
            elementsPerPage: this.pageSize,
            currentPage: i
          },
          followLink('subcommunities', { findListOptions: this.configOnePage }),
          followLink('collections', { findListOptions: this.configOnePage }))
          .pipe(
            getFirstCompletedRemoteData(),
            switchMap((rd: RemoteData<PaginatedList<Community>>) => {
              if (hasValue(rd) && hasValue(rd.payload)) {
                return this.transformListOfCommunities(rd.payload, level + 1, communityFlatNode, expandedNodes);
              } else {
                return observableOf([]);
              }
            })
          );

        subcoms = [...subcoms, nextSetOfSubcommunitiesPage];
      }

      obsList = [...obsList, combineAndFlatten(subcoms)];

      const currentCollectionPage = expandedNodes.find((node: FlatNode) => node.id === community.id).currentCollectionPage;
      let collections = [];
      for (let i = 1; i <= currentCollectionPage; i++) {
        const nextSetOfCollectionsPage = this.collectionDataService.findByParent(community.uuid, {
          elementsPerPage: this.pageSize,
          currentPage: i
        })
          .pipe(
            getFirstCompletedRemoteData(),
            map((rd: RemoteData<PaginatedList<Collection>>) => {
              if (hasValue(rd) && hasValue(rd.payload)) {
                let nodes = rd.payload.page
                  .map((collection: Collection) => toFlatNode(collection, observableOf(false), level + 1, false, communityFlatNode));
                if (currentCollectionPage < rd.payload.totalPages && currentCollectionPage === rd.payload.currentPage) {
                  nodes = [...nodes, showMoreFlatNode('collection', level + 1, communityFlatNode)];
                }
                return nodes;
              } else {
                return [];
              }
            }),
          );
        collections = [...collections, nextSetOfCollectionsPage];
      }
      obsList = [...obsList, combineAndFlatten(collections)];
    }

    return combineAndFlatten(obsList);
  }

  /**
   * Checks if a community has subcommunities or collections by querying the respective services with a pageSize = 0
   *      Returns an observable that combines the result.payload.totalElements fo the observables that the
   *          respective services return when queried
   * @param community     Community being checked whether it is expandable (if it has subcommunities or collections)
   */
  public getIsExpandable(community: Community): Observable<boolean> {
    let hasSubcoms$: Observable<boolean>;
    let hasColls$: Observable<boolean>;
    hasSubcoms$ = this.communityDataService.findByParent(community.uuid, this.configOnePage)
      .pipe(
        map((rd: RemoteData<PaginatedList<Community>>) => {
          if (hasValue(rd) && hasValue(rd.payload)) {
            return rd.payload.totalElements > 0;
          } else {
            return false;
          }
        }),
      );

    hasColls$ = this.collectionDataService.findByParent(community.uuid, this.configOnePage)
      .pipe(
        map((rd: RemoteData<PaginatedList<Collection>>) => {
          if (hasValue(rd) && hasValue(rd.payload)) {
            return rd.payload.totalElements > 0;
          } else {
            return false;
          }
        }),
      );

    let hasChildren$: Observable<boolean>;
    hasChildren$ = observableCombineLatest(hasSubcoms$, hasColls$).pipe(
      map(([hasSubcoms, hasColls]: [boolean, boolean]) => hasSubcoms || hasColls)
    );

    return hasChildren$;
  }

}
