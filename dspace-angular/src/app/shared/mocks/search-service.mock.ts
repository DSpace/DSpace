import { of as observableOf } from 'rxjs';
import { SearchService } from '../../core/shared/search/search.service';

export function getMockSearchService(): SearchService {
  return jasmine.createSpyObj('searchService', {
    search: '',
    getEndpoint: observableOf('discover/search/objects'),
    getSearchLink: '/mydspace',
    getScopes: observableOf(['test-scope']),
    setServiceOptions: {}
  });
}
