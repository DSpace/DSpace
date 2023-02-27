import { Injectable } from '@angular/core';

import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

import { RemoteData } from '../../core/data/remote-data';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { SearchService } from '../../core/shared/search/search.service';
import { isEmpty, isNotEmpty } from '../../shared/empty.util';
import { PaginatedSearchOptions } from '../../shared/search/models/paginated-search-options.model';
import { getFirstCompletedRemoteData } from '../../core/shared/operators';
import { SearchObjects } from '../../shared/search/models/search-objects.model';
import { createNoContentRemoteDataObject } from '../../shared/remote-data.utils';

/**
 * Service that handle profiles claim.
 */
@Injectable()
export class ProfileClaimService {

  constructor(private searchService: SearchService) {
  }

  /**
   * Returns true if it is possible to suggest profiles to be claimed to the given eperson.
   *
   * @param eperson the eperson
   */
  hasProfilesToSuggest(eperson: EPerson): Observable<boolean> {
    return this.searchForSuggestions(eperson).pipe(
      getFirstCompletedRemoteData(),
      map((rd: RemoteData<SearchObjects<DSpaceObject>>) => {
        return isNotEmpty(rd) && rd.hasSucceeded && rd.payload?.page?.length > 0;
      })
    );
  }

  /**
   * Returns profiles that could be associated with the given user.
   *
   * @param eperson the user
   */
  searchForSuggestions(eperson: EPerson): Observable<RemoteData<SearchObjects<DSpaceObject>>> {
    const query = this.personQueryData(eperson);
    if (isEmpty(query)) {
      return of(createNoContentRemoteDataObject() as RemoteData<SearchObjects<DSpaceObject>>);
    }
    return this.lookup(query);
  }

  /**
   * Search object by the given query.
   *
   * @param query the query for the search
   */
  private lookup(query: string): Observable<RemoteData<SearchObjects<DSpaceObject>>> {
    if (isEmpty(query)) {
      return of(createNoContentRemoteDataObject() as RemoteData<SearchObjects<DSpaceObject>>);
    }
    return this.searchService.search(new PaginatedSearchOptions({
      configuration: 'eperson_claims',
      query: query
    }), null, false, true);
  }

  /**
   * Return the search query for person lookup, from the given eperson
   *
   * @param eperson The eperson to use for the lookup
   */
  private personQueryData(eperson: EPerson): string {
    if (eperson && eperson.email) {
      return 'person.email:' + eperson.email;
    } else {
      return null;
    }
  }

}
