import { SearchService } from '../../../core/shared/search/search.service';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { SearchSettingsComponent } from './search-settings.component';
import { of as observableOf } from 'rxjs';
import { PaginationComponentOptions } from '../../pagination/pagination-component-options.model';
import { SortDirection, SortOptions } from '../../../core/cache/models/sort-options.model';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { EnumKeysPipe } from '../../utils/enum-keys-pipe';
import { By } from '@angular/platform-browser';
import { SearchFilterService } from '../../../core/shared/search/search-filter.service';
import { VarDirective } from '../../utils/var.directive';
import { SEARCH_CONFIG_SERVICE } from '../../../my-dspace-page/my-dspace-page.component';
import { SidebarService } from '../../sidebar/sidebar.service';
import { SidebarServiceStub } from '../../testing/sidebar-service.stub';
import { PaginationService } from '../../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../testing/pagination-service.stub';

describe('SearchSettingsComponent', () => {

  let comp: SearchSettingsComponent;
  let fixture: ComponentFixture<SearchSettingsComponent>;
  let searchServiceObject: SearchService;

  let pagination: PaginationComponentOptions;
  let sort: SortOptions;
  let mockResults;
  let searchServiceStub;

  let queryParam;
  let scopeParam;
  let paginatedSearchOptions;

  let paginationService;

  let activatedRouteStub;

  beforeEach(waitForAsync(() => {
    pagination = new PaginationComponentOptions();
    pagination.id = 'search-results-pagination';
    pagination.currentPage = 1;
    pagination.pageSize = 10;
    sort = new SortOptions('score', SortDirection.DESC);
    mockResults = ['test', 'data'];
    searchServiceStub = {
      searchOptions: { pagination: pagination, sort: sort },
      search: () => mockResults,
    };

    queryParam = 'test query';
    scopeParam = '7669c72a-3f2a-451f-a3b9-9210e7a4c02f';
    paginatedSearchOptions = {
      query: queryParam,
      scope: scopeParam,
      pagination,
      sort,
    };

    activatedRouteStub = {
      queryParams: observableOf({
        query: queryParam,
        scope: scopeParam,
      }),
    };

    paginationService = new PaginationServiceStub(pagination, sort);

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      declarations: [SearchSettingsComponent, EnumKeysPipe, VarDirective],
      providers: [
        { provide: SearchService, useValue: searchServiceStub },

        { provide: ActivatedRoute, useValue: activatedRouteStub },
        {
          provide: SidebarService,
          useValue: SidebarServiceStub,
        },
        {
          provide: SearchFilterService,
          useValue: {},
        },
        {
          provide: PaginationService,
          useValue: paginationService,
        },
        {
          provide: SEARCH_CONFIG_SERVICE,
          useValue: {
            paginatedSearchOptions: observableOf(paginatedSearchOptions),
            getCurrentScope: observableOf('test-id'),
          }
        },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchSettingsComponent);
    comp = fixture.componentInstance;

    comp.sortOptionsList = [
      new SortOptions('score', SortDirection.DESC),
      new SortOptions('dc.title', SortDirection.ASC),
      new SortOptions('dc.title', SortDirection.DESC)
    ];

    // SearchPageComponent test instance
    fixture.detectChanges();
    searchServiceObject = (comp as any).service;
    spyOn(comp, 'reloadOrder');
    spyOn(searchServiceObject, 'search').and.callThrough();

  });

  it('it should show the order settings with the respective selectable options', () => {
    fixture.detectChanges();
    const orderSetting = fixture.debugElement.query(By.css('div.result-order-settings'));
    expect(orderSetting).toBeDefined();
    const childElements = orderSetting.queryAll(By.css('option'));
    expect(childElements.length).toEqual(comp.sortOptionsList.length);
  });

  it('it should show the size settings', () => {
    fixture.detectChanges();
    const pageSizeSetting = fixture.debugElement.query(By.css('page-size-settings'));
    expect(pageSizeSetting).toBeDefined();
  });

  it('should have the proper order value selected by default', () => {
    fixture.detectChanges();
    const orderSetting = fixture.debugElement.query(By.css('div.result-order-settings'));
    const childElementToBeSelected = orderSetting.query(By.css('option[value="score,DESC"][selected="selected"]'));
    expect(childElementToBeSelected).toBeDefined();
  });
});
