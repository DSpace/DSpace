import { SortOptions } from '../../../cache/models/sort-options.model';
import { RequestParam } from '../../../cache/models/request-param.model';
import { isNotEmpty } from '../../../../shared/empty.util';
import { FindListOptions } from '../../../data/find-list-options.model';

/**
 * Representing properties used to build a vocabulary find request
 */
export class VocabularyFindOptions extends FindListOptions {

  constructor(public query: string = '',
              public filter?: string,
              public exact?: boolean,
              public entryID?: string,
              public elementsPerPage?: number,
              public currentPage?: number,
              public sort?: SortOptions
              ) {
    super();

    const searchParams = [];

    if (isNotEmpty(query)) {
      searchParams.push(new RequestParam('query', query));
    }
    if (isNotEmpty(filter)) {
      searchParams.push(new RequestParam('filter', filter));
    }
    if (isNotEmpty(exact)) {
      searchParams.push(new RequestParam('exact', exact.toString()));
    }
    if (isNotEmpty(entryID)) {
      searchParams.push(new RequestParam('entryID', entryID));
    }
    this.searchParams = searchParams;
  }
}
