import { SortDirection, SortOptions } from '../../../core/cache/models/sort-options.model';
import { PaginationComponentOptions } from '../../pagination/pagination-component-options.model';
import { PaginatedSearchOptions } from './paginated-search-options.model';
import { DSpaceObjectType } from '../../../core/shared/dspace-object-type.model';
import { SearchFilter } from './search-filter.model';

describe('PaginatedSearchOptions', () => {
  let options: PaginatedSearchOptions;
  const sortOptions = new SortOptions('test.field', SortDirection.DESC);
  const pageOptions = Object.assign(new PaginationComponentOptions(), { pageSize: 40, page: 1 });
  const filters = [
    new SearchFilter('f.test', ['value']),
    new SearchFilter('f.example', ['another value', 'second value']), // should be split into two arguments, spaces should be URI-encoded
    new SearchFilter('f.range', ['[2002 TO 2021]'], 'equals'),        // value should be URI-encoded, ',equals' should not
  ];
  const fixedFilter = 'f.fixed=1234,5678,equals';                     // '=' and ',equals' should not be URI-encoded
  const query = 'search query';
  const scope = '0fde1ecb-82cc-425a-b600-ac3576d76b47';
  const baseUrl = 'www.rest.com';
  beforeEach(() => {
    options = new PaginatedSearchOptions({
      sort: sortOptions,
      pagination: pageOptions,
      filters: filters,
      query: query,
      scope: scope,
      dsoTypes: [DSpaceObjectType.ITEM],
      fixedFilter: fixedFilter,
    });
  });

  describe('when toRestUrl is called', () => {

    it('should generate a string with all parameters that are present', () => {
      const outcome = options.toRestUrl(baseUrl);
      expect(outcome).toEqual('www.rest.com?' +
        'sort=test.field,DESC&' +
        'page=0&' +
        'size=40&' +
        'f.fixed=1234%2C5678,equals&' +
        'query=search%20query&' +
        'scope=0fde1ecb-82cc-425a-b600-ac3576d76b47&' +
        'dsoType=ITEM&' +
        'f.test=value&' +
        'f.example=another%20value&' +
        'f.example=second%20value&' +
        'f.range=%5B2002%20TO%202021%5D,equals'
      );
    });

  });
});
