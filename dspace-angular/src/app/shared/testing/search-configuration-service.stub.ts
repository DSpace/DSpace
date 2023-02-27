import { BehaviorSubject, of as observableOf } from 'rxjs';
import { SearchConfig } from '../../core/shared/search/search-filters/search-config.model';
import { SortDirection, SortOptions } from '../../core/cache/models/sort-options.model';

export class SearchConfigurationServiceStub {

  public paginationID = 'test-id';

  private searchOptions: BehaviorSubject<any> = new BehaviorSubject<any>({});
  private paginatedSearchOptions: BehaviorSubject<any> = new BehaviorSubject<any>({});

  getCurrentFrontendFilters() {
    return observableOf([]);
  }

  getCurrentScope(a) {
    return observableOf('test-id');
  }

  getCurrentQuery(a) {
    return observableOf(a);
  }

  getCurrentConfiguration(a) {
    return observableOf(a);
  }

  getConfig () {
    return observableOf({ hasSucceeded: true, payload: [] });
  }

  getAvailableConfigurationOptions() {
    return observableOf([{value: 'test', label: 'test'}]);
  }

  getConfigurationSearchConfigObservable() {
    return observableOf(new SearchConfig());
  }

  getConfigurationSortOptionsObservable() {
    return observableOf([new SortOptions('score', SortDirection.ASC), new SortOptions('score', SortDirection.DESC)]);
  }

  initializeSortOptionsFromConfiguration() {
   /* empty */
  }
}
