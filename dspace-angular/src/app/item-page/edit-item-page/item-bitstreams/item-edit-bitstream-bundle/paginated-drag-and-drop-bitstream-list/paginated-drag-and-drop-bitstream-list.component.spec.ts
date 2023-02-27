import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Bundle } from '../../../../../core/shared/bundle.model';
import { TranslateModule } from '@ngx-translate/core';
import { PaginatedDragAndDropBitstreamListComponent } from './paginated-drag-and-drop-bitstream-list.component';
import { VarDirective } from '../../../../../shared/utils/var.directive';
import { ObjectValuesPipe } from '../../../../../shared/utils/object-values-pipe';
import { ObjectUpdatesService } from '../../../../../core/data/object-updates/object-updates.service';
import { BundleDataService } from '../../../../../core/data/bundle-data.service';
import { Bitstream } from '../../../../../core/shared/bitstream.model';
import { BitstreamFormat } from '../../../../../core/shared/bitstream-format.model';
import { of as observableOf } from 'rxjs';
import { take } from 'rxjs/operators';
import { ResponsiveTableSizes } from '../../../../../shared/responsive-table-sizes/responsive-table-sizes';
import { ResponsiveColumnSizes } from '../../../../../shared/responsive-table-sizes/responsive-column-sizes';
import { createSuccessfulRemoteDataObject$ } from '../../../../../shared/remote-data.utils';
import { createPaginatedList } from '../../../../../shared/testing/utils.test';
import { RequestService } from '../../../../../core/data/request.service';
import { PaginationService } from '../../../../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../../../../shared/testing/pagination-service.stub';

describe('PaginatedDragAndDropBitstreamListComponent', () => {
  let comp: PaginatedDragAndDropBitstreamListComponent;
  let fixture: ComponentFixture<PaginatedDragAndDropBitstreamListComponent>;
  let objectUpdatesService: ObjectUpdatesService;
  let bundleService: BundleDataService;
  let objectValuesPipe: ObjectValuesPipe;
  let requestService: RequestService;
  let paginationService;

  const columnSizes = new ResponsiveTableSizes([
    new ResponsiveColumnSizes(2, 2, 3, 4, 4),
    new ResponsiveColumnSizes(2, 3, 3, 3, 3),
    new ResponsiveColumnSizes(2, 2, 2, 2, 2),
    new ResponsiveColumnSizes(6, 5, 4, 3, 3)
  ]);

  const bundle = Object.assign(new Bundle(), {
    id: 'bundle-1',
    uuid: 'bundle-1',
    _links: {
      self: { href: 'bundle-1-selflink' }
    }
  });
  const date = new Date();
  const format = Object.assign(new BitstreamFormat(), {
    shortDescription: 'PDF'
  });
  const bitstream1 = Object.assign(new Bitstream(), {
    uuid: 'bitstreamUUID1',
    name: 'Fake Bitstream 1',
    bundleName: 'ORIGINAL',
    description: 'Description',
    format: createSuccessfulRemoteDataObject$(format)
  });
  const fieldUpdate1 = {
    field: bitstream1,
    changeType: undefined
  };
  const bitstream2 = Object.assign(new Bitstream(), {
    uuid: 'bitstreamUUID2',
    name: 'Fake Bitstream 2',
    bundleName: 'ORIGINAL',
    description: 'Description',
    format: createSuccessfulRemoteDataObject$(format)
  });
  const fieldUpdate2 = {
    field: bitstream2,
    changeType: undefined
  };

  beforeEach(waitForAsync(() => {
    objectUpdatesService = jasmine.createSpyObj('objectUpdatesService',
      {
        getFieldUpdates: observableOf({
          [bitstream1.uuid]: fieldUpdate1,
          [bitstream2.uuid]: fieldUpdate2,
        }),
        getFieldUpdatesExclusive: observableOf({
          [bitstream1.uuid]: fieldUpdate1,
          [bitstream2.uuid]: fieldUpdate2,
        }),
        getFieldUpdatesByCustomOrder: observableOf({
          [bitstream1.uuid]: fieldUpdate1,
          [bitstream2.uuid]: fieldUpdate2,
        }),
        saveMoveFieldUpdate: {},
        saveRemoveFieldUpdate: {},
        removeSingleFieldUpdate: {},
        saveAddFieldUpdate: {},
        discardFieldUpdates: {},
        reinstateFieldUpdates: observableOf(true),
        initialize: {},
        getUpdatedFields: observableOf([bitstream1, bitstream2]),
        getLastModified: observableOf(date),
        hasUpdates: observableOf(true),
        isReinstatable: observableOf(false),
        isValidPage: observableOf(true),
        initializeWithCustomOrder: {},
        addPageToCustomOrder: {}
      }
    );

    bundleService = jasmine.createSpyObj('bundleService', {
      getBitstreams: createSuccessfulRemoteDataObject$(createPaginatedList([bitstream1, bitstream2])),
      getBitstreamsEndpoint: observableOf('')
    });

    objectValuesPipe = new ObjectValuesPipe();

    requestService = jasmine.createSpyObj('requestService', {
      hasByHref$: observableOf(true)
    });

    paginationService = new PaginationServiceStub();

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [PaginatedDragAndDropBitstreamListComponent, VarDirective],
      providers: [
        { provide: ObjectUpdatesService, useValue: objectUpdatesService },
        { provide: BundleDataService, useValue: bundleService },
        { provide: ObjectValuesPipe, useValue: objectValuesPipe },
        { provide: RequestService, useValue: requestService },
        { provide: PaginationService, useValue: paginationService }
      ], schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PaginatedDragAndDropBitstreamListComponent);
    comp = fixture.componentInstance;
    comp.bundle = bundle;
    comp.columnSizes = columnSizes;
    fixture.detectChanges();
  });

  it('should initialize the objectsRD$', (done) => {
    comp.objectsRD$.pipe(take(1)).subscribe((objects) => {
      expect(objects.payload.page).toEqual([bitstream1, bitstream2]);
      done();
    });
  });

  it('should initialize the URL', () => {
    expect(comp.url).toEqual(bundle.self);
  });
});
