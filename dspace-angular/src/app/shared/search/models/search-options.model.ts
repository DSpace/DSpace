import { hasValue, isNotEmpty } from '../../empty.util';
import { URLCombiner } from '../../../core/url-combiner/url-combiner';
import { SearchFilter } from './search-filter.model';
import { DSpaceObjectType } from '../../../core/shared/dspace-object-type.model';
import { ViewMode } from '../../../core/shared/view-mode.model';

/**
 * This model class represents all parameters needed to request information about a certain search request
 */
export class SearchOptions {
  configuration?: string;
  view?: ViewMode = ViewMode.ListElement;
  scope?: string;
  query?: string;
  dsoTypes?: DSpaceObjectType[];
  filters?: SearchFilter[];
  fixedFilter?: string;

  constructor(
    options: {
      configuration?: string, scope?: string, query?: string, dsoTypes?: DSpaceObjectType[], filters?: SearchFilter[],
      fixedFilter?: string
    }
  ) {
      this.configuration = options.configuration;
      this.scope = options.scope;
      this.query = options.query;
      this.dsoTypes = options.dsoTypes;
      this.filters = options.filters;
      this.fixedFilter = options.fixedFilter;
  }

  /**
   * Method to generate the URL that can be used request information about a search request
   * @param {string} url The URL to the REST endpoint
   * @param {string[]} args A list of query arguments that should be included in the URL
   * @returns {string} URL with all search options and passed arguments as query parameters
   */
  toRestUrl(url: string, args: string[] = []): string {
    if (isNotEmpty(this.configuration)) {
      args.push(`configuration=${encodeURIComponent(this.configuration)}`);
    }
    if (isNotEmpty(this.fixedFilter)) {
      args.push(this.encodedFixedFilter);
    }
    if (isNotEmpty(this.query)) {
      args.push(`query=${encodeURIComponent(this.query)}`);
    }
    if (isNotEmpty(this.scope)) {
      args.push(`scope=${encodeURIComponent(this.scope)}`);
    }
    if (isNotEmpty(this.dsoTypes)) {
      this.dsoTypes.forEach((dsoType: string) => {
        args.push(`dsoType=${encodeURIComponent(dsoType)}`);
      });
    }
    if (isNotEmpty(this.filters)) {
      this.filters.forEach((filter: SearchFilter) => {
        filter.values.forEach((value) => {
          const filterValue = value.includes(',') ? `${value}` : value + (filter.operator ? ',' + filter.operator : '');
          args.push(`${filter.key}=${this.encodeFilterQueryValue(filterValue)}`);
        });
      });
    }
    if (isNotEmpty(args)) {
      url = new URLCombiner(url, `?${args.join('&')}`).toString();
    }
    return url;
  }

  get encodedFixedFilter(): string {
    // expected format: 'arg=value'
    //  -> split the query agument into (arg=)(value) and only encode 'value'
    const match = this.fixedFilter.match(/^([^=]+=)(.+)$/);

    if (hasValue(match)) {
      return match[1] + this.encodeFilterQueryValue(match[2]);
    } else {
      return this.encodeFilterQueryValue(this.fixedFilter);
    }
  }

  encodeFilterQueryValue(filterQueryValue: string): string {
    // expected format: 'value' or 'value,operator'
    //  -> split into (value)(,operator) and only encode 'value'
    const match = filterQueryValue.match(/^(.*)(,\w+)$/);

    if (hasValue(match)) {
      return encodeURIComponent(match[1]) + match[2];
    } else {
      return encodeURIComponent(filterQueryValue);
    }
  }
}
