import { WorkspaceItem } from '../../../core/submission/models/workspaceitem.model';
import { searchResultFor } from '../../search/search-result-element-decorator';
import { SearchResult } from '../../search/models/search-result.model';

/**
 * Represents a search result object of a WorkspaceItem object
 */
@searchResultFor(WorkspaceItem)
export class WorkspaceItemSearchResult extends SearchResult<WorkspaceItem> {
}
