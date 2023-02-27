import { BrowseByDatePageComponent } from './browse-by-date-page.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { EnumKeysPipe } from '../../shared/utils/enum-keys-pipe';
import { ActivatedRoute, Router } from '@angular/router';
import { BrowseService } from '../../core/browse/browse.service';
import { DSpaceObjectDataService } from '../../core/data/dspace-object-data.service';
import { RouterMock } from '../../shared/mocks/router.mock';
import { ChangeDetectorRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { of as observableOf } from 'rxjs';
import { ActivatedRouteStub } from '../../shared/testing/active-router.stub';
import { Community } from '../../core/shared/community.model';
import { Item } from '../../core/shared/item.model';
import { BrowseEntrySearchOptions } from '../../core/browse/browse-entry-search-options.model';
import { toRemoteData } from '../browse-by-metadata-page/browse-by-metadata-page.component.spec';
import { VarDirective } from '../../shared/utils/var.directive';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { PaginationService } from '../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../shared/testing/pagination-service.stub';
import { APP_CONFIG } from '../../../config/app-config.interface';
import { environment } from '../../../environments/environment';

describe('BrowseByDatePageComponent', () => {
  let comp: BrowseByDatePageComponent;
  let fixture: ComponentFixture<BrowseByDatePageComponent>;
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

  const firstItem = Object.assign(new Item(), {
    id: 'first-item-id',
    metadata: {
      'dc.date.issued': [
        {
          value: '1950-01-01'
        }
      ]
    }
  });

  const mockBrowseService = {
    getBrowseEntriesFor: (options: BrowseEntrySearchOptions) => toRemoteData([]),
    getBrowseItemsFor: (value: string, options: BrowseEntrySearchOptions) => toRemoteData([firstItem]),
    getFirstItemFor: () => createSuccessfulRemoteDataObject$(firstItem)
  };

  const mockDsoService = {
    findById: () => createSuccessfulRemoteDataObject$(mockCommunity)
  };

  const activatedRouteStub = Object.assign(new ActivatedRouteStub(), {
    params: observableOf({}),
    queryParams: observableOf({}),
    data: observableOf({ metadata: 'dateissued', metadataField: 'dc.date.issued' })
  });

  const mockCdRef = Object.assign({
    detectChanges: () => fixture.detectChanges()
  });

  paginationService = new PaginationServiceStub();

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [CommonModule, RouterTestingModule.withRoutes([]), TranslateModule.forRoot(), NgbModule],
      declarations: [BrowseByDatePageComponent, EnumKeysPipe, VarDirective],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: BrowseService, useValue: mockBrowseService },
        { provide: DSpaceObjectDataService, useValue: mockDsoService },
        { provide: Router, useValue: new RouterMock() },
        { provide: PaginationService, useValue: paginationService },
        { provide: ChangeDetectorRef, useValue: mockCdRef },
        { provide: APP_CONFIG, useValue: environment }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BrowseByDatePageComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
    route = (comp as any).route;
  });

  it('should initialize the list of items', () => {
    comp.items$.subscribe((result) => {
      expect(result.payload.page).toEqual([firstItem]);
    });
  });

  it('should create a list of startsWith options with the earliest year at the end (rounded down by 10)', () => {
    expect(comp.startsWithOptions[comp.startsWithOptions.length - 1]).toEqual(1950);
  });

  it('should create a list of startsWith options with the current year first', () => {
    expect(comp.startsWithOptions[0]).toEqual(new Date().getUTCFullYear());
  });
});
