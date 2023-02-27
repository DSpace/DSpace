import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { SearchConfigurationService } from '../../../../../../core/shared/search/search-configuration.service';
import { SearchFilterService } from '../../../../../../core/shared/search/search-filter.service';
import { SearchService } from '../../../../../../core/shared/search/search.service';
import { RouterStub } from '../../../../../testing/router.stub';
import { SearchServiceStub } from '../../../../../testing/search-service.stub';
import { FacetValue } from '../../../../models/facet-value.model';
import { FilterType } from '../../../../models/filter-type.model';
import { SearchFilterConfig } from '../../../../models/search-filter-config.model';
import { SearchFacetOptionComponent } from './search-facet-option.component';
import { PaginationComponentOptions } from '../../../../../pagination/pagination-component-options.model';
import { PaginationService } from '../../../../../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../../../../testing/pagination-service.stub';
import { ShortNumberPipe } from '../../../../../utils/short-number.pipe';

describe('SearchFacetOptionComponent', () => {
  let comp: SearchFacetOptionComponent;
  let fixture: ComponentFixture<SearchFacetOptionComponent>;
  const filterName1 = 'testname';
  const filterName2 = 'testAuthorityname';
  const value1 = 'testvalue1';
  const value2 = 'test2';
  const operator = 'authority';

  const mockFilterConfig = Object.assign(new SearchFilterConfig(), {
    name: filterName1,
    filterType: FilterType.range,
    hasFacets: false,
    isOpenByDefault: false,
    pageSize: 2,
    minValue: 200,
    maxValue: 3000,
  });

  const mockAuthorityFilterConfig = Object.assign(new SearchFilterConfig(), {
    name: filterName2,
    filterType: FilterType.authority,
    hasFacets: false,
    isOpenByDefault: false,
    pageSize: 2
  });

  const value: FacetValue = {
    label: value2,
    value: value2,
    count: 20,
    _links: {
      self: { href: 'selectedValue-self-link2' },
      search: { href: `` }
    }
  };

  const selectedValue: FacetValue = {
    label: value1,
    value: value1,
    count: 20,
    _links: {
      self: { href: 'selectedValue-self-link1' },
      search: { href: `http://test.org/api/discover/search/objects?f.${filterName1}=${value1},${operator}` }
    }
  };

  const authorityValue: FacetValue = {
    label: value2,
    value: value2,
    count: 20,
    _links: {
      self: { href: 'authorityValue-self-link2' },
      search: { href: `http://test.org/api/discover/search/objects?f.${filterName2}=${value2},${operator}` }
    }
  };

  const searchLink = '/search';
  const selectedValues = [selectedValue];
  const selectedValues$ = observableOf(selectedValues);
  let filterService;
  let searchService;
  let router;
  const page = observableOf(0);

  const pagination = Object.assign(new PaginationComponentOptions(), { id: 'page-id', currentPage: 1, pageSize: 20 });
  const paginationService = new PaginationServiceStub(pagination);
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), NoopAnimationsModule, FormsModule],
      declarations: [SearchFacetOptionComponent, ShortNumberPipe],
      providers: [
        { provide: SearchService, useValue: new SearchServiceStub(searchLink) },
        { provide: Router, useValue: new RouterStub() },
        { provide: PaginationService, useValue: paginationService },
        {
          provide: SearchConfigurationService, useValue: {
            paginationID: 'page-id',
            searchOptions: observableOf({})
          }
        },
        {
          provide: SearchFilterService, useValue: {
            getSelectedValuesForFilter: () => selectedValues,
            isFilterActiveWithValue: (paramName: string, filterValue: string) => observableOf(true),
            getPage: (paramName: string) => page,
            /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
            incrementPage: (filterName: string) => {
            },
            resetPage: (filterName: string) => {
            }
            /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
          }
        }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(SearchFacetOptionComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchFacetOptionComponent);
    comp = fixture.componentInstance; // SearchPageComponent test instance
    filterService = (comp as any).filterService;
    searchService = (comp as any).searchService;
    router = (comp as any).router;
    comp.filterValue = value;
    comp.selectedValues$ = selectedValues$;
    comp.filterConfig = mockFilterConfig;
    fixture.detectChanges();
  });

  describe('when the updateAddParams method is called with a value', () => {
    it('should update the addQueryParams with the new parameter values', () => {
      comp.addQueryParams = {};
      (comp as any).updateAddParams(selectedValues);
      expect(comp.addQueryParams).toEqual({
        [mockFilterConfig.paramName]: [`${value1},${operator}`, value.value + ',equals'],
        ['page-id.page']: 1
      });
    });
  });

  describe('when filter type is authority and the updateAddParams method is called with a value', () => {
    it('should update the addQueryParams with the new parameter values', () => {
      comp.filterValue = authorityValue;
      comp.filterConfig = mockAuthorityFilterConfig;
      fixture.detectChanges();

      comp.addQueryParams = {};
      (comp as any).updateAddParams(selectedValues);
      expect(comp.addQueryParams).toEqual({
        [mockAuthorityFilterConfig.paramName]: [value1 + ',equals', `${value2},${operator}`],
        ['page-id.page']: 1
      });
    });
  });

  describe('when isVisible emits true', () => {
    it('the facet option should be visible', () => {
      comp.isVisible = observableOf(true);
      fixture.detectChanges();
      const linkEl = fixture.debugElement.query(By.css('a'));
      expect(linkEl).not.toBeNull();
    });
  });

  describe('when isVisible emits false', () => {
    it('the facet option should not be visible', () => {
      comp.isVisible = observableOf(false);
      fixture.detectChanges();
      const linkEl = fixture.debugElement.query(By.css('a'));
      expect(linkEl).toBeNull();
    });
  });
});
