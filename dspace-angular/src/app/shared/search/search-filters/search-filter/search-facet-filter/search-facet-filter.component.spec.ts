import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import {
  FILTER_CONFIG,
  IN_PLACE_SEARCH,
  REFRESH_FILTER,
  SearchFilterService
} from '../../../../../core/shared/search/search-filter.service';
import { SearchFilterConfig } from '../../../models/search-filter-config.model';
import { FilterType } from '../../../models/filter-type.model';
import { FacetValue } from '../../../models/facet-value.model';
import { FormsModule } from '@angular/forms';
import { BehaviorSubject, of as observableOf } from 'rxjs';
import { SearchService } from '../../../../../core/shared/search/search.service';
import { SearchServiceStub } from '../../../../testing/search-service.stub';
import { buildPaginatedList } from '../../../../../core/data/paginated-list.model';
import { RouterStub } from '../../../../testing/router.stub';
import { Router } from '@angular/router';
import { PageInfo } from '../../../../../core/shared/page-info.model';
import { SearchFacetFilterComponent } from './search-facet-filter.component';
import { RemoteDataBuildService } from '../../../../../core/cache/builders/remote-data-build.service';
import { SearchConfigurationServiceStub } from '../../../../testing/search-configuration-service.stub';
import { SEARCH_CONFIG_SERVICE } from '../../../../../my-dspace-page/my-dspace-page.component';
import { createSuccessfulRemoteDataObject$ } from '../../../../remote-data.utils';

describe('SearchFacetFilterComponent', () => {
  let comp: SearchFacetFilterComponent;
  let fixture: ComponentFixture<SearchFacetFilterComponent>;
  const filterName1 = 'test name';
  const value1 = 'testvalue1';
  const value2 = 'test2';
  const value3 = 'another value3';
  const mockFilterConfig: SearchFilterConfig = Object.assign(new SearchFilterConfig(), {
    name: filterName1,
    filterType: FilterType.text,
    hasFacets: false,
    isOpenByDefault: false,
    pageSize: 2
  });
  const values: FacetValue[] = [
    {
      label: value1,
      value: value1,
      count: 52,
      _links: {
        self: {
          href: ''
        },
        search: {
          href: ''
        }
      }
    }, {
      label: value2,
      value: value2,
      count: 20,
      _links: {
        self: {
          href: ''
        },
        search: {
          href: ''
        }
      }
    }, {
      label: value3,
      value: value3,
      count: 5,
      _links: {
        self: {
          href: ''
        },
        search: {
          href: ''
        }
      }
    }
  ];

  const searchLink = '/search';
  const selectedValues = [value1, value2];
  let filterService;
  let searchService;
  let router;
  const page = observableOf(0);

  const mockValues = createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), values));
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), NoopAnimationsModule, FormsModule],
      declarations: [SearchFacetFilterComponent],
      providers: [
        { provide: SearchService, useValue: new SearchServiceStub(searchLink) },
        { provide: Router, useValue: new RouterStub() },
        { provide: FILTER_CONFIG, useValue: new SearchFilterConfig() },
        { provide: RemoteDataBuildService, useValue: { aggregate: () => observableOf({}) } },
        { provide: SEARCH_CONFIG_SERVICE, useValue: new SearchConfigurationServiceStub() },
        { provide: IN_PLACE_SEARCH, useValue: false },
        { provide: REFRESH_FILTER, useValue: new BehaviorSubject<boolean>(false) },
        {
          provide: SearchFilterService, useValue: {
            getSelectedValuesForFilter: () => observableOf(selectedValues),
            isFilterActiveWithValue: (paramName: string, filterValue: string) => true,
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
    }).overrideComponent(SearchFacetFilterComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchFacetFilterComponent);
    comp = fixture.componentInstance; // SearchPageComponent test instance
    comp.filterConfig = mockFilterConfig;
    filterService = (comp as any).filterService;
    searchService = (comp as any).searchService;
    spyOn(searchService, 'getFacetValuesFor').and.returnValue(mockValues);
    router = (comp as any).router;
    fixture.detectChanges();
  });

  describe('when the isChecked method is called with a value', () => {
    beforeEach(() => {
      spyOn(filterService, 'isFilterActiveWithValue');
      comp.isChecked(values[1]);
    });

    it('should call isFilterActiveWithValue on the filterService with the correct filter parameter name and the passed value', () => {
      expect(filterService.isFilterActiveWithValue).toHaveBeenCalledWith(mockFilterConfig.paramName, values[1].value);
    });
  });

  describe('when the getSearchLink method is triggered', () => {
    let link: string;
    beforeEach(() => {
      link = comp.getSearchLink();
    });

    it('should return the value of the searchLink variable in the filter service', () => {
      expect(link).toEqual(searchLink);
    });
  });

  describe('when the showMore method is called', () => {
    beforeEach(() => {
      spyOn(filterService, 'incrementPage');
      comp.showMore();
    });

    it('should call incrementPage on the filterService with the correct filter parameter name', () => {
      expect(filterService.incrementPage).toHaveBeenCalledWith(mockFilterConfig.name);
    });
  });

  describe('when the showFirstPageOnly method is called', () => {
    beforeEach(() => {
      spyOn(filterService, 'resetPage');
      comp.showFirstPageOnly();
    });

    it('should call resetPage on the filterService with the correct filter parameter name', () => {
      expect(filterService.resetPage).toHaveBeenCalledWith(mockFilterConfig.name);
    });
  });

  describe('when the getCurrentPage method is called', () => {
    beforeEach(() => {
      spyOn(filterService, 'getPage');
      comp.getCurrentPage();
    });

    it('should call getPage on the filterService with the correct filter parameter name', () => {
      expect(filterService.getPage).toHaveBeenCalledWith(mockFilterConfig.name);
    });
  });

  describe('when the getCurrentUrl method is called', () => {
    const url = 'test.url/test';
    beforeEach(() => {
      router.navigateByUrl(url);
    });

    it('should call getPage on the filterService with the correct filter parameter name', () => {
      expect(router.url).toEqual(url);
    });
  });

  describe('when the onSubmit method is called with data', () => {
    const searchUrl = '/search/path';
    const testValue = 'test';

    beforeEach(() => {
      comp.selectedValues$ = observableOf(selectedValues.map((value) =>
        Object.assign(new FacetValue(), {
          label: value,
          value: value
        })));
      fixture.detectChanges();
      spyOn(comp, 'getSearchLink').and.returnValue(searchUrl);
    });

    it('should call navigate on the router with the right searchlink and parameters when the filter is provided with a valid operator', () => {
      comp.onSubmit(testValue + ',equals');
      expect(router.navigate).toHaveBeenCalledWith(searchUrl.split('/'), {
        queryParams: { [mockFilterConfig.paramName]: [...selectedValues.map((value) => `${value},equals`), `${testValue},equals`] },
        queryParamsHandling: 'merge'
      });
    });

    it('should not call navigate on the router when the filter is not provided with a valid operator', () => {
      comp.onSubmit(testValue);
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should not call navigate on the router when the empty string is given as filter', () => {
      comp.onSubmit(',equals');
      expect(router.navigate).not.toHaveBeenCalled();
    });
  });

  describe('when updateFilterValueList is called', () => {
    beforeEach(() => {
      spyOn(comp, 'showFirstPageOnly');
      comp.updateFilterValueList();
    });

    it('should call showFirstPageOnly and empty the filter', () => {
      expect(comp.animationState).toEqual('loading');
      expect((comp as any).collapseNextUpdate).toBeTruthy();
      expect(comp.filter).toEqual('');
    });
  });

  describe('when findSuggestions is called with query \'test\'', () => {
    const query = 'test';
    beforeEach(() => {
      comp.findSuggestions(query);
    });

    it('should call getFacetValuesFor on the component\'s SearchService with the right query', () => {
      expect((comp as any).searchService.getFacetValuesFor).toHaveBeenCalledWith(comp.filterConfig, 1, {}, query);
    });
  });
});
