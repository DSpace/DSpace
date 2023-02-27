/* eslint-disable max-classes-per-file */
import { BehaviorSubject } from 'rxjs';
import { VocabularyEntryDetail } from '../../../core/submission/vocabularies/models/vocabulary-entry-detail.model';
import { PageInfo } from '../../../core/shared/page-info.model';

export const LOAD_MORE = 'LOAD_MORE';
export const LOAD_MORE_ROOT = 'LOAD_MORE_ROOT';
export const LOAD_MORE_NODE: any = { id: LOAD_MORE };
export const LOAD_MORE_ROOT_NODE: any = { id: LOAD_MORE_ROOT };

/** Nested node */
export class TreeviewNode {
  childrenChange = new BehaviorSubject<TreeviewNode[]>([]);

  get children(): TreeviewNode[] {
    return this.childrenChange.value;
  }

  constructor(public item: VocabularyEntryDetail,
              public hasChildren = false,
              public pageInfo: PageInfo = new PageInfo(),
              public loadMoreParentItem: VocabularyEntryDetail | null = null,
              public isSearchNode = false,
              public isInInitValueHierarchy = false) {
  }

  updatePageInfo(pageInfo: PageInfo) {
    this.pageInfo = pageInfo;
  }
}

/** Flat node with expandable and level information */
export class TreeviewFlatNode {
  constructor(public item: VocabularyEntryDetail,
              public level = 1,
              public expandable = false,
              public childrenLoaded = false,
              public pageInfo: PageInfo = new PageInfo(),
              public loadMoreParentItem: VocabularyEntryDetail | null = null,
              public isSearchNode = false,
              public isInInitValueHierarchy = false) {
  }
}

