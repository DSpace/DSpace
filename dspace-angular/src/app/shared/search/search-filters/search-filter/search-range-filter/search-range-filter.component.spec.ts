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
import { SearchRangeFilterComponent } from './search-range-filter.component';
import { RemoteDataBuildService } from '../../../../../core/cache/builders/remote-data-build.service';
import { SEARCH_CONFIG_SERVICE } from '../../../../../my-dspace-page/my-dspace-page.component';
import { SearchConfigurationServiceStub } from '../../../../testing/search-configuration-service.stub';
import { createSuccessfulRemoteDataObject$ } from '../../../../remote-data.utils';
import { RouteService } from '../../../../../core/services/route.service';

describe('SearchRangeFilterComponent', () => {
  let comp: SearchRangeFilterComponent;
  let fixture: ComponentFixture<SearchRangeFilterComponent>;
  const minSuffix = '.min';
  const maxSuffix = '.max';
  const filterName1 = 'test name';
  const value1 = '2000 - 2012';
  const value2 = '1992 - 2000';
  const value3 = '1990 - 1992';
  const mockFilterConfig: SearchFilterConfig = Object.assign(new SearchFilterConfig(), {
    name: filterName1,
    filterType: FilterType.range,
    hasFacets: false,
    isOpenByDefault: false,
    pageSize: 2,
    minValue: 200,
    maxValue: 3000,
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
  const selectedValues = observableOf([value1]);
  let filterService;
  let searchService;
  let router;
  const page = observableOf(0);

  const mockValues = createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), values));
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), NoopAnimationsModule, FormsModule],
      declarations: [SearchRangeFilterComponent],
      providers: [
        { provide: SearchService, useValue: new SearchServiceStub(searchLink) },
        { provide: Router, useValue: new RouterStub() },
        { provide: FILTER_CONFIG, useValue: mockFilterConfig },
        { provide: RemoteDataBuildService, useValue: { aggregate: () => observableOf({}) } },
        { provide: RouteService, useValue: { getQueryParameterValue: () => observableOf({}) } },
        { provide: SEARCH_CONFIG_SERVICE, useValue: new SearchConfigurationServiceStub() },
        { provide: IN_PLACE_SEARCH, useValue: false },
        { provide: REFRESH_FILTER, useValue: new BehaviorSubject<boolean>(false) },
        {
          provide: SearchFilterService, useValue: {
            getSelectedValuesForFilter: () => selectedValues,
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
    }).overrideComponent(SearchRangeFilterComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchRangeFilterComponent);
    comp = fixture.componentInstance; // SearchPageComponent test instance
    filterService = (comp as any).filterService;
    searchService = (comp as any).searchService;
    spyOn(searchService, 'getFacetValuesFor').and.returnValue(mockValues);
    router = (comp as any).router;
    fixture.detectChanges();
  });

  describe('when the onSubmit method is called with data', () => {
    const searchUrl = '/search/path';
    // const data = { [mockFilterConfig.paramName + minSuffix]: '1900', [mockFilterConfig.paramName + maxSuffix]: '1950' };
    beforeEach(() => {
      comp.range = [1900, 1950];
      spyOn(comp, 'getSearchLink').and.returnValue(searchUrl);
      comp.onSubmit();
    });

    it('should call navigate on the router with the right searchlink and parameters', () => {
      expect(router.navigate).toHaveBeenCalledWith(searchUrl.split('/'), {
        queryParams: {
          [mockFilterConfig.paramName + minSuffix]: [1900],
          [mockFilterConfig.paramName + maxSuffix]: [1950]
        },
        queryParamsHandling: 'merge'
      });
    });
  });
});
