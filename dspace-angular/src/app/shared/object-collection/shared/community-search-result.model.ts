import { Community } from '../../../core/shared/community.model';
import { SearchResult } from '../../search/models/search-result.model';
import { searchResultFor } from '../../search/search-result-element-decorator';

@searchResultFor(Community)
export class CommunitySearchResult extends SearchResult<Community> {
}
