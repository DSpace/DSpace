import { ClaimedTask } from '../../../core/tasks/models/claimed-task-object.model';
import { SearchResult } from '../../search/models/search-result.model';

/**
 * Represents a search result object of a Declined/Rejected ClaimedTask object (sent back to the submitter)
 */
export class ClaimedDeclinedTaskSearchResult extends SearchResult<ClaimedTask> {
}
