import { TestBed } from '@angular/core/testing';
import { CSSVariableService } from './css-variable.service';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { getTestScheduler } from 'jasmine-marbles';
import { buildPaginatedList } from '../../core/data/paginated-list.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { KeyValuePair } from '../key-value-pair.model';

describe('CSSVariableService', () => {
  let store: MockStore;

  let service: CSSVariableService;
  let initialState;
  const varKey1 = '--test-1';
  const varValue1 = 'test-value-1';
  const varKey2 = '--test-2';
  const varValue2 = 'test-value-2';
  const varKey3 = '--test-3';
  const varValue3 = 'test-value-3';
  const queryInAll = 'test';
  const queryFor3 = '3';

  function init() {
    initialState = {
      ['cssVariables']: {
        [varKey1]: varValue1,
        [varKey2]: varValue2,
        [varKey3]: varValue3,
      }
    };
  }

  beforeEach(() => {
    init();
    TestBed.configureTestingModule({
      providers: [
        CSSVariableService,
        provideMockStore({ initialState }),
      ],
    });
    service = TestBed.inject(CSSVariableService as any);
    store = TestBed.inject(MockStore as any);
  });

  it('should create', () => {
    expect(service).toBeTruthy();
  });

  describe('searchVariable', () => {
    it('should return the right keys and variables in a paginated list for query that returns all 3 results', () => {
      const currentPage = 1;
      const pageSize = 5;
      const pageInfo = new PageInfo({ currentPage, elementsPerPage: pageSize, totalPages: 1, totalElements: 3 });
      const page: KeyValuePair<string, string>[] = [{ key: varKey1, value: varValue1 }, { key: varKey2, value: varValue2 }, { key: varKey3, value: varValue3 }];
      const result = buildPaginatedList(pageInfo, page);
      getTestScheduler().expectObservable(service.searchVariable(queryInAll, { currentPage, pageSize } as any)).toBe('a', { a: result });
    });

    it('should return the right keys and variables in a paginated list for query that returns only the 3rd results', () => {
      const currentPage = 1;
      const pageSize = 5;
      const pageInfo = new PageInfo({ currentPage, elementsPerPage: pageSize, totalPages: 1, totalElements: 1 });
      const page: KeyValuePair<string, string>[] = [{ key: varKey3, value: varValue3 }];
      const result = buildPaginatedList(pageInfo, page);
      getTestScheduler().expectObservable(service.searchVariable(queryFor3, { currentPage, pageSize } as any)).toBe('a', { a: result });
    });

    it('should return the right keys and variables in a paginated list that\'s not longer than the page size', () => {
      const currentPage = 1;
      const pageSize = 2;
      const pageInfo = new PageInfo({ currentPage, elementsPerPage: pageSize, totalPages: 2, totalElements: 3 });
      const page: KeyValuePair<string, string>[] = [{ key: varKey1, value: varValue1 }, { key: varKey2, value: varValue2 }];
      const result = buildPaginatedList(pageInfo, page);
      getTestScheduler().expectObservable(service.searchVariable(queryInAll, { currentPage, pageSize } as any)).toBe('a', { a: result });
    });
  });

});
