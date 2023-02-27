import { cold, getTestScheduler } from 'jasmine-marbles';

import { of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { ProfileClaimService } from './profile-claim.service';
import { SearchService } from '../../core/shared/search/search.service';
import { ItemSearchResult } from '../../shared/object-collection/shared/item-search-result.model';
import { SearchObjects } from '../../shared/search/models/search-objects.model';
import { Item } from '../../core/shared/item.model';
import { createNoContentRemoteDataObject, createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { EPerson } from '../../core/eperson/models/eperson.model';

describe('ProfileClaimService', () => {
  let scheduler: TestScheduler;
  let service: ProfileClaimService;
  let serviceAsAny: any;
  let searchService: jasmine.SpyObj<SearchService>;

  const eperson: EPerson = Object.assign(new EPerson(), {
    id: 'id',
    metadata: {
      'eperson.firstname': [
        {
          value: 'John'
        }
      ],
      'eperson.lastname': [
        {
          value: 'Doe'
        },
      ],
    },
    email: 'fake@email.com'
  });
  const item1: Item = Object.assign(new Item(), {
    uuid: 'e1c51c69-896d-42dc-8221-1d5f2ad5516e',
    metadata: {
      'person.email': [
        {
          value: 'fake@email.com'
        }
      ],
      'person.familyName': [
        {
          value: 'Doe'
        }
      ],
      'person.givenName': [
        {
          value: 'John'
        }
      ]
    },
    _links: {
      self: {
        href: 'item-href'
      }
    }
  });
  const item2: Item = Object.assign(new Item(), {
    uuid: 'c8279647-1acc-41ae-b036-951d5f65649b',
    metadata: {
      'person.email': [
        {
          value: 'fake2@email.com'
        }
      ],
      'dc.title': [
        {
          value: 'John, Doe'
        }
      ]
    },
    _links: {
      self: {
        href: 'item-href'
      }
    }
  });
  const item3: Item = Object.assign(new Item(), {
    uuid: 'c8279647-1acc-41ae-b036-951d5f65649b',
    metadata: {
      'person.email': [
        {
          value: 'fake3@email.com'
        }
      ],
      'dc.title': [
        {
          value: 'John, Doe'
        }
      ]
    },
    _links: {
      self: {
        href: 'item-href'
      }
    }
  });

  const searchResult1 = Object.assign(new ItemSearchResult(), { indexableObject: item1 });
  const searchResult2 = Object.assign(new ItemSearchResult(), { indexableObject: item2 });
  const searchResult3 = Object.assign(new ItemSearchResult(), { indexableObject: item3 });

  const searchResult = Object.assign(new SearchObjects(), {
    page: [searchResult1, searchResult2, searchResult3]
  });
  const emptySearchResult = Object.assign(new SearchObjects(), {
    page: []
  });
  const searchResultRD = createSuccessfulRemoteDataObject(searchResult);
  const emptySearchResultRD = createSuccessfulRemoteDataObject(emptySearchResult);

  beforeEach(() => {
    scheduler = getTestScheduler();

    searchService = jasmine.createSpyObj('SearchService', {
      search: jasmine.createSpy('search')
    });

    service = new ProfileClaimService(searchService);
    serviceAsAny = service;
  });

  describe('hasProfilesToSuggest', () => {

    describe('when has suggestions', () => {
      beforeEach(() => {
        spyOn(service, 'searchForSuggestions').and.returnValue(observableOf(searchResultRD));
      });

      it('should return true', () => {
        const result = service.hasProfilesToSuggest(eperson);
        const expected = cold('(a|)', {
          a: true
        });
        expect(result).toBeObservable(expected);
      });

    });

    describe('when has not suggestions', () => {
      beforeEach(() => {
        spyOn(service, 'searchForSuggestions').and.returnValue(observableOf(emptySearchResultRD));
      });

      it('should return false', () => {
        const result = service.hasProfilesToSuggest(eperson);
        const expected = cold('(a|)', {
          a: false
        });
        expect(result).toBeObservable(expected);
      });

    });

    describe('when has not valid eperson', () => {
      it('should return false', () => {
        const result = service.hasProfilesToSuggest(null);
        const expected = cold('(a|)', {
          a: false
        });
        expect(result).toBeObservable(expected);
      });

    });

  });

  describe('search', () => {

    describe('when has search results', () => {
      beforeEach(() => {
        searchService.search.and.returnValue(observableOf(searchResultRD));
      });

      it('should return the proper search object', () => {
        const result = service.searchForSuggestions(eperson);
        const expected = cold('(a|)', {
          a: searchResultRD
        });
        expect(result).toBeObservable(expected);
      });

    });

    describe('when has not suggestions', () => {
      beforeEach(() => {
        searchService.search.and.returnValue(observableOf(emptySearchResultRD));
      });

      it('should return null', () => {
        const result = service.searchForSuggestions(eperson);
        const expected = cold('(a|)', {
          a: emptySearchResultRD
        });
        expect(result).toBeObservable(expected);
      });

    });

    describe('when has not valid eperson', () => {
      it('should return null', () => {
        const result = service.searchForSuggestions(null);
        const expected = cold('(a|)', {
          a: createNoContentRemoteDataObject()
        });
        expect(result).toBeObservable(expected);
      });

    });

  });
});
