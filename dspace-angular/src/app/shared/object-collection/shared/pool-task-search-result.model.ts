import { PoolTask } from '../../../core/tasks/models/pool-task-object.model';
import { SearchResult } from '../../search/models/search-result.model';
import { searchResultFor } from '../../search/search-result-element-decorator';

/**
 * Represents a search result object of a PoolTask object
 */
@searchResultFor(PoolTask)
export class PoolTaskSearchResult extends SearchResult<PoolTask> {
}
