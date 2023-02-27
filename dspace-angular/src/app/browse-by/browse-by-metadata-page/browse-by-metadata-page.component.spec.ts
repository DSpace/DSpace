import {
  BrowseByMetadataPageComponent,
  browseParamsToOptions,
  getBrowseSearchOptions
} from './browse-by-metadata-page.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { BrowseService } from '../../core/browse/browse.service';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { EnumKeysPipe } from '../../shared/utils/enum-keys-pipe';
import { ActivatedRoute, Router } from '@angular/router';
import { ActivatedRouteStub } from '../../shared/testing/active-router.stub';
import { Observable, of as observableOf } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RemoteData } from '../../core/data/remote-data';
import { buildPaginatedList, PaginatedList } from '../../core/data/paginated-list.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { BrowseEntrySearchOptions } from '../../core/browse/browse-entry-search-options.model';
import { SortDirection } from '../../core/cache/models/sort-options.model';
import { Item } from '../../core/shared/item.model';
import { DSpaceObjectDataService } from '../../core/data/dspace-object-data.service';
import { Community } from '../../core/shared/community.model';
import { RouterMock } from '../../shared/mocks/router.mock';
import { BrowseEntry } from '../../core/shared/browse-entry.model';
import { VarDirective } from '../../shared/utils/var.directive';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { PaginationService } from '../../core/pagination/pagination.service';
import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import { PaginationServiceStub } from '../../shared/testing/pagination-service.stub';
import { APP_CONFIG } from '../../../config/app-config.interface';

describe('BrowseByMetadataPageComponent', () => {
  let comp: BrowseByMetadataPageComponent;
  let fixture: ComponentFixture<BrowseByMetadataPageComponent>;
  let browseService: BrowseService;
  let route: ActivatedRoute;
  let paginationService;

  const mockCommunity = Object.assign(new Community(), {
    id: 'test-uuid',
    metadata: [
      {
        key: 'dc.title',
        value: 'test community'
      }
    ]
  });

  const environmentMock = {
    browseBy: {
      showThumbnails: true,
      pageSize: 10
    }
  };

  const mockEntries = [
    {
      type: BrowseEntry.type,
      authority: null,
      value: 'John Doe',
      language: 'en',
      count: 1
    },
    {
      type: BrowseEntry.type,
      authority: null,
      value: 'James Doe',
      language: 'en',
      count: 3
    },
    {
      type: BrowseEntry.type,
      authority: null,
      value: 'Fake subject',
      language: 'en',
      count: 2
    }
  ];

  const mockItems = [
    Object.assign(new Item(), {
      id: 'fakeId'
    })
  ];

  const mockBrowseService = {
    getBrowseEntriesFor: (options: BrowseEntrySearchOptions) => toRemoteData(mockEntries),
    getBrowseItemsFor: (value: string, options: BrowseEntrySearchOptions) => toRemoteData(mockItems)
  };

  const mockDsoService = {
    findById: () => createSuccessfulRemoteDataObject$(mockCommunity)
  };

  const activatedRouteStub = Object.assign(new ActivatedRouteStub(), {
    params: observableOf({})
  });

  paginationService = new PaginationServiceStub();

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [CommonModule, RouterTestingModule.withRoutes([]), TranslateModule.forRoot(), NgbModule],
      declarations: [BrowseByMetadataPageComponent, EnumKeysPipe, VarDirective],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: BrowseService, useValue: mockBrowseService },
        { provide: DSpaceObjectDataService, useValue: mockDsoService },
        { provide: PaginationService, useValue: paginationService },
        { provide: Router, useValue: new RouterMock() },
        { provide: APP_CONFIG, useValue: environmentMock }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BrowseByMetadataPageComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
    browseService = (comp as any).browseService;
    route = (comp as any).route;
    route.params = observableOf({});
    comp.ngOnInit();
    fixture.detectChanges();
  });

  it('should not fetch any items when no value is provided', () => {
    expect(comp.items$).toBeUndefined();
  });

  it('should set embed thumbnail property to true', () => {
    expect(comp.fetchThumbnails).toBeTrue();
  });

  describe('when a value is provided', () => {
    beforeEach(() => {
      const paramsWithValue = {
        metadata: 'author',
        value: 'John Doe'
      };

      route.params = observableOf(paramsWithValue);
      comp.ngOnInit();
      comp.updateParent('fake-scope');
      comp.updateLogo();
      fixture.detectChanges();
    });

    it('should fetch items', () => {
      comp.items$.subscribe((result) => {
        expect(result.payload.page).toEqual(mockItems);
      });
    });

    it('should fetch the logo', () => {
      expect(comp.logo$).toBeTruthy();
    });
  });

  describe('when calling browseParamsToOptions', () => {
    let result: BrowseEntrySearchOptions;

    beforeEach(() => {
      const paramsScope = {
        scope: 'fake-scope'
      };
      const paginationOptions = Object.assign(new PaginationComponentOptions(), {
        currentPage: 5,
        pageSize: comp.appConfig.browseBy.pageSize,
      });
      const sortOptions = {
        direction: SortDirection.ASC,
        field: 'fake-field',
      };

      result = browseParamsToOptions(paramsScope, paginationOptions, sortOptions, 'author', comp.fetchThumbnails);
    });

    it('should return BrowseEntrySearchOptions with the correct properties', () => {

      expect(result.metadataDefinition).toEqual('author');
      expect(result.pagination.currentPage).toEqual(5);
      expect(result.pagination.pageSize).toEqual(10);
      expect(result.sort.direction).toEqual(SortDirection.ASC);
      expect(result.sort.field).toEqual('fake-field');
      expect(result.scope).toEqual('fake-scope');
      expect(result.fetchThumbnail).toBeTrue();
    });
  });

  describe('calling getBrowseSearchOptions', () => {
    let result: BrowseEntrySearchOptions;

    beforeEach(() => {
      const paramsScope = {
        scope: 'fake-scope'
      };
      const paginationOptions = Object.assign(new PaginationComponentOptions(), {
        currentPage: 5,
        pageSize: comp.appConfig.browseBy.pageSize,
      });
      const sortOptions = {
        direction: SortDirection.ASC,
        field: 'fake-field',
      };

      result = getBrowseSearchOptions('title', paginationOptions, sortOptions, comp.fetchThumbnails);
    });
    it('should return BrowseEntrySearchOptions with the correct properties', () => {

      expect(result.metadataDefinition).toEqual('title');
      expect(result.pagination.currentPage).toEqual(5);
      expect(result.pagination.pageSize).toEqual(10);
      expect(result.sort.direction).toEqual(SortDirection.ASC);
      expect(result.sort.field).toEqual('fake-field');
      expect(result.fetchThumbnail).toBeTrue();
    });
  });
});

export function toRemoteData(objects: any[]): Observable<RemoteData<PaginatedList<any>>> {
  return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), objects));
}
