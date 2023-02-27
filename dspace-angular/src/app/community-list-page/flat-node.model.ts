import { Observable } from 'rxjs';
import { Community } from '../core/shared/community.model';
import { Collection } from '../core/shared/collection.model';
import { ShowMoreFlatNode } from './show-more-flat-node.model';

/**
 * Each node in the tree is represented by a flatNode which contains info about the node itself and its position and
 *  state in the tree. There are nodes representing communities, collections and show more links.
 */
export interface FlatNode {
  isExpandable$: Observable<boolean>;
  name: string;
  id: string;
  level: number;
  isExpanded?: boolean;
  parent?: FlatNode;
  payload: Community | Collection | ShowMoreFlatNode;
  isShowMoreNode: boolean;
  route?: string;
  currentCommunityPage?: number;
  currentCollectionPage?: number;
}
