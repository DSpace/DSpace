import { Injectable } from '@angular/core';

import { BehaviorSubject, Observable, of as observableOf } from 'rxjs';
import { map, merge, mergeMap, scan } from 'rxjs/operators';
import findIndex from 'lodash/findIndex';

import {
  LOAD_MORE_NODE,
  LOAD_MORE_ROOT_NODE,
  TreeviewFlatNode,
  TreeviewNode
} from './vocabulary-treeview-node.model';
import { VocabularyEntry } from '../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { VocabularyService } from '../../../core/submission/vocabularies/vocabulary.service';
import { PageInfo } from '../../../core/shared/page-info.model';
import { isEmpty, isNotEmpty } from '../../empty.util';
import { VocabularyOptions } from '../../../core/submission/vocabularies/models/vocabulary-options.model';
import {
  getFirstSucceededRemoteDataPayload,
  getFirstSucceededRemoteListPayload
} from '../../../core/shared/operators';
import { PaginatedList } from '../../../core/data/paginated-list.model';
import { VocabularyEntryDetail } from '../../../core/submission/vocabularies/models/vocabulary-entry-detail.model';

/**
 * A service that provides methods to deal with vocabulary tree
 */
@Injectable()
export class VocabularyTreeviewService {

  /**
   * A map containing the current node showed by the tree
   */
  nodeMap = new Map<string, TreeviewNode>();

  /**
   * A map containing all the node already created for building the tree
   */
  storedNodeMap = new Map<string, TreeviewNode>();

  /**
   * An array containing all the node already created for building the tree
   */
  storedNodes: TreeviewNode[] = [];

  /**
   * The {@link VocabularyOptions} object
   */
  private vocabularyOptions: VocabularyOptions;

  /**
   * The vocabulary name
   */
  private vocabularyName = '';

  /**
   * Contains the current tree data
   */
  private dataChange = new BehaviorSubject<TreeviewNode[]>([]);

  /**
   * Array containing node'ids hierarchy
   */
  private initValueHierarchy: string[] = [];

  /**
   * A boolean representing if any operation is pending
   */
  private loading = new BehaviorSubject<boolean>(false);

  /**
   * The {@link PageInfo} object
   */
  private pageInfo: PageInfo;
  /**
   * An observable to change the loading status
   */
  private hideSearchingWhenUnsubscribed$ = new Observable(() => () => this.loading.next(false));

  /**
   * Initialize instance variables
   *
   * @param {VocabularyService} vocabularyService
   */
  constructor(private vocabularyService: VocabularyService) {
  }

  /**
   * Remove nodes saved from maps and array
   */
  cleanTree() {
    this.nodeMap = new Map<string, TreeviewNode>();
    this.storedNodeMap = new Map<string, TreeviewNode>();
    this.storedNodes = [];
    this.initValueHierarchy = [];
    this.dataChange.next([]);
  }

  /**
   * Initialize the tree's nodes
   *
   * @param options The {@link VocabularyOptions} object
   * @param pageInfo The {@link PageInfo} object
   * @param initValueId The entry id of the node to mark as selected, if any
   */
  initialize(options: VocabularyOptions, pageInfo: PageInfo, initValueId?: string): void {
    this.loading.next(true);
    this.vocabularyOptions = options;
    this.vocabularyName = options.name;
    this.pageInfo = pageInfo;
    if (isNotEmpty(initValueId)) {
      this.getNodeHierarchyById(initValueId)
        .subscribe((hierarchy: string[]) => {
          this.initValueHierarchy = hierarchy;
          this.retrieveTopNodes(pageInfo, []);
      });
    } else {
      this.retrieveTopNodes(pageInfo, []);
    }
  }

  /**
   * Returns array of the tree's nodes
   */
  getData(): Observable<TreeviewNode[]> {
    return this.dataChange;
  }

  /**
   * Expand the root node whose children are not loaded
   * @param node The root node
   */
  loadMoreRoot(node: TreeviewFlatNode) {
    const nodes = this.dataChange.value;
    nodes.pop();
    this.retrieveTopNodes(node.pageInfo, nodes);
  }

  /**
   * Expand a node whose children are not loaded
   * @param item
   * @param onlyFirstTime
   */
  loadMore(item: VocabularyEntryDetail, onlyFirstTime = false) {
    if (!this.nodeMap.has(item.otherInformation.id)) {
      return;
    }
    const parent: TreeviewNode = this.nodeMap.get(item.otherInformation.id)!;
    const children = this.nodeMap.get(item.otherInformation.id)!.children || [];
    children.pop();
    this.getChildrenNodesByParent(item.otherInformation.id, parent.pageInfo).subscribe((list: PaginatedList<VocabularyEntryDetail>) => {

      if (onlyFirstTime && parent.children!.length > 0) {
        return;
      }

      const newNodes: TreeviewNode[] = list.page.map((entry) => this._generateNode(entry));
      children.push(...newNodes);

      if ((list.pageInfo.currentPage + 1) <= list.pageInfo.totalPages) {
        // Update page info
        const newPageInfo: PageInfo = Object.assign(new PageInfo(), list.pageInfo, {
          currentPage: list.pageInfo.currentPage + 1
        });
        parent.updatePageInfo(newPageInfo);

        // Need a new load more node
        children.push(new TreeviewNode(LOAD_MORE_NODE, false, newPageInfo, item));
      }
      parent.childrenChange.next(children);
      this.dataChange.next(this.dataChange.value);
    });

  }

  /**
   * Check if any operation is pending
   */
  isLoading(): Observable<boolean> {
    return this.loading;
  }

  /**
   * Perform a search operation by query
   */
  searchByQuery(query: string) {
    this.loading.next(true);
    if (isEmpty(this.storedNodes)) {
      this.storedNodes = this.dataChange.value;
      this.storedNodeMap = this.nodeMap;
    }
    this.nodeMap = new Map<string, TreeviewNode>();
    this.dataChange.next([]);

    this.vocabularyService.getVocabularyEntriesByValue(query, false, this.vocabularyOptions, new PageInfo()).pipe(
      getFirstSucceededRemoteListPayload(),
      mergeMap((result: VocabularyEntry[]) => (result.length > 0) ? result : observableOf(null)),
      mergeMap((entry: VocabularyEntry) =>
        this.vocabularyService.findEntryDetailById(entry.otherInformation.id, this.vocabularyName).pipe(
          getFirstSucceededRemoteDataPayload()
        )
      ),
      mergeMap((entry: VocabularyEntryDetail) => this.getNodeHierarchy(entry)),
      scan((acc: TreeviewNode[], value: TreeviewNode) => {
        if (isEmpty(value) || findIndex(acc, (node) => node.item.otherInformation.id === value.item.otherInformation.id) !== -1) {
          return acc;
        } else {
          return [...acc, value];
        }
      }, []),
      merge(this.hideSearchingWhenUnsubscribed$)
    ).subscribe((nodes: TreeviewNode[]) => {
      this.dataChange.next(nodes);
      this.loading.next(false);
    });
  }

  /**
   * Reset tree state with the one before the search
   */
  restoreNodes() {
    this.loading.next(false);
    this.dataChange.next(this.storedNodes);
    this.nodeMap = this.storedNodeMap;

    this.storedNodeMap = new Map<string, TreeviewNode>();
    this.storedNodes = [];
  }

  /**
   * Generate a {@link TreeviewNode} object from vocabulary entry
   *
   * @param entry The vocabulary entry detail
   * @param isSearchNode A Boolean representing if given entry is the result of a search
   * @param toStore A Boolean representing if the node created is to store or not
   * @return TreeviewNode
   */
  private _generateNode(entry: VocabularyEntryDetail, isSearchNode = false, toStore = true): TreeviewNode {
    const entryId = entry.otherInformation.id;
    if (this.nodeMap.has(entryId)) {
      return this.nodeMap.get(entryId)!;
    }
    const hasChildren = entry.hasOtherInformation() && (entry.otherInformation as any)!.hasChildren === 'true';
    const pageInfo: PageInfo = this.pageInfo;
    const isInInitValueHierarchy = this.initValueHierarchy.includes(entryId);
    const result = new TreeviewNode(
      entry,
      hasChildren,
      pageInfo,
      null,
      isSearchNode,
      isInInitValueHierarchy);

    if (toStore) {
      this.nodeMap.set(entryId, result);
    }
    return result;
  }

  /**
   * Return the node Hierarchy by a given node's id
   * @param id The node id
   * @return Observable<string[]>
   */
  private getNodeHierarchyById(id: string): Observable<string[]> {
    return this.getById(id).pipe(
      mergeMap((entry: VocabularyEntryDetail) => this.getNodeHierarchy(entry, [], false)),
      map((node: TreeviewNode) => this.getNodeHierarchyIds(node))
    );
  }

  /**
   * Return the vocabulary entry's children
   * @param parentId The node id
   * @param pageInfo The {@link PageInfo} object
   * @return Observable<PaginatedList<VocabularyEntryDetail>>
   */
  private getChildrenNodesByParent(parentId: string, pageInfo: PageInfo): Observable<PaginatedList<VocabularyEntryDetail>> {
    return this.vocabularyService.getEntryDetailChildren(parentId, this.vocabularyName, pageInfo).pipe(
      getFirstSucceededRemoteDataPayload()
    );
  }

  /**
   * Return the vocabulary entry's parent
   * @param entryId The entry id
   */
  private getParentNode(entryId: string): Observable<VocabularyEntryDetail> {
    return this.vocabularyService.getEntryDetailParent(entryId, this.vocabularyName).pipe(
      getFirstSucceededRemoteDataPayload()
    );
  }

  /**
   * Return the vocabulary entry by id
   * @param entryId The entry id
   * @return Observable<VocabularyEntryDetail>
   */
  private getById(entryId: string): Observable<VocabularyEntryDetail> {
    return this.vocabularyService.findEntryDetailById(entryId, this.vocabularyName).pipe(
      getFirstSucceededRemoteDataPayload()
    );
  }

  /**
   * Retrieve the top level vocabulary entries
   * @param pageInfo The {@link PageInfo} object
   * @param nodes The top level nodes already loaded, if any
   */
  private retrieveTopNodes(pageInfo: PageInfo, nodes: TreeviewNode[]): void {
    this.vocabularyService.searchTopEntries(this.vocabularyName, pageInfo).pipe(
      getFirstSucceededRemoteDataPayload()
    ).subscribe((list: PaginatedList<VocabularyEntryDetail>) => {
      this.vocabularyService.clearSearchTopRequests();
      const newNodes: TreeviewNode[] = list.page.map((entry: VocabularyEntryDetail) => this._generateNode(entry));
      nodes.push(...newNodes);

      if ((list.pageInfo.currentPage + 1) <= list.pageInfo.totalPages) {
        // Need a new load more node
        const newPageInfo: PageInfo = Object.assign(new PageInfo(), list.pageInfo, {
          currentPage: list.pageInfo.currentPage + 1
        });
        const loadMoreNode = new TreeviewNode(LOAD_MORE_ROOT_NODE, false, newPageInfo);
        loadMoreNode.updatePageInfo(newPageInfo);
        nodes.push(loadMoreNode);
      }
      this.loading.next(false);
      // Notify the change.
      this.dataChange.next(nodes);
    });
  }

  /**
   * Build and return the tree node hierarchy by a given vocabulary entry
   *
   * @param item The vocabulary entry
   * @param children The vocabulary entry
   * @param toStore A Boolean representing if the node created is to store or not
   * @return Observable<string[]>
   */
  private getNodeHierarchy(item: VocabularyEntryDetail, children?: TreeviewNode[], toStore = true): Observable<TreeviewNode> {
    if (isEmpty(item)) {
      return observableOf(null);
    }
    const node = this._generateNode(item, toStore, toStore);

    if (isNotEmpty(children)) {
      const newChildren = children
        .filter((entry: TreeviewNode) => {
          return findIndex(node.children, (nodeEntry) => nodeEntry.item.otherInformation.id === entry.item.otherInformation.id) === -1;
        });
      newChildren.forEach((entry: TreeviewNode) => {
        entry.loadMoreParentItem = node.item;
      });
      node.children.push(...newChildren);
    }

    if (node.item.hasOtherInformation() && isNotEmpty(node.item.otherInformation.parent)) {
      return this.getParentNode(node.item.otherInformation.id).pipe(
        mergeMap((parentItem: VocabularyEntryDetail) => this.getNodeHierarchy(parentItem, [node], toStore))
      );
    } else {
      return observableOf(node);
    }
  }

  /**
   * Build and return the node Hierarchy ids by a given node
   *
   * @param node The given node
   * @param hierarchyIds The ids already present in the Hierarchy's array
   * @return string[]
   */
  private getNodeHierarchyIds(node: TreeviewNode, hierarchyIds: string[] = []): string[] {
    if (!hierarchyIds.includes(node.item.otherInformation.id)) {
      hierarchyIds.push(node.item.otherInformation.id);
    }
    if (isNotEmpty(node.children)) {
      return this.getNodeHierarchyIds(node.children[0], hierarchyIds);
    } else {
      return hierarchyIds;
    }
  }
}
