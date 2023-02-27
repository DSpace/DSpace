import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ObjectDetailComponent } from './object-detail.component';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateLoaderMock } from '../mocks/translate-loader.mock';
import { buildPaginatedList } from '../../core/data/paginated-list.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { createSuccessfulRemoteDataObject } from '../remote-data.utils';
import { DSpaceObject } from '../../core/shared/dspace-object.model';

describe('ObjectDetailComponent', () => {
  let comp: ObjectDetailComponent;
  let fixture: ComponentFixture<ObjectDetailComponent>;
  const testEvent: any = { test: 'test' };

  const testObjects = [
    Object.assign(new DSpaceObject(), { one: 1 }),
    Object.assign(new DSpaceObject(), { two: 2 }),
    Object.assign(new DSpaceObject(), { three: 3 }),
    Object.assign(new DSpaceObject(), { four: 4 }),
    Object.assign(new DSpaceObject(), { five: 5 }),
    Object.assign(new DSpaceObject(), { six: 6 }),
    Object.assign(new DSpaceObject(), { seven: 7 }),
    Object.assign(new DSpaceObject(), { eight: 8 }),
    Object.assign(new DSpaceObject(), { nine: 9 }),
    Object.assign(new DSpaceObject(), { ten: 10 }),
  ];
  const pageInfo = Object.assign(new PageInfo(), {
    elementsPerPage: 1,
    totalElements: 10,
    totalPages: 10,
    currentPage: 1
  });
  const mockRD = createSuccessfulRemoteDataObject(buildPaginatedList(pageInfo, testObjects));

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      declarations: [ObjectDetailComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ObjectDetailComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ObjectDetailComponent);
    comp = fixture.componentInstance; // SearchPageComponent test instance
    comp.objects = mockRD;
    fixture.detectChanges();
  });

  describe('when the pageChange output on the pagination is triggered', () => {
    beforeEach(() => {
      spyOn(comp, 'onPageChange');
      const paginationEl = fixture.debugElement.query(By.css('ds-pagination'));
      paginationEl.triggerEventHandler('pageChange', testEvent);
    });

    it('should call onPageChange on the component', () => {
      expect(comp.onPageChange).toHaveBeenCalledWith(testEvent);
    });
  });

  describe('when the pageSizeChange output on the pagination is triggered', () => {
    beforeEach(() => {
      spyOn(comp, 'onPageSizeChange');
      const paginationEl = fixture.debugElement.query(By.css('ds-pagination'));
      paginationEl.triggerEventHandler('pageSizeChange', testEvent);
    });

    it('should call onPageSizeChange on the component', () => {
      expect(comp.onPageSizeChange).toHaveBeenCalledWith(testEvent);
    });
  });

  describe('when the sortDirectionChange output on the pagination is triggered', () => {
    beforeEach(() => {
      spyOn(comp, 'onSortDirectionChange');
      const paginationEl = fixture.debugElement.query(By.css('ds-pagination'));
      paginationEl.triggerEventHandler('sortDirectionChange', testEvent);
    });

    it('should call onSortDirectionChange on the component', () => {
      expect(comp.onSortDirectionChange).toHaveBeenCalledWith(testEvent);
    });
  });

  describe('when the sortFieldChange output on the pagination is triggered', () => {
    beforeEach(() => {
      spyOn(comp, 'onSortFieldChange');
      const paginationEl = fixture.debugElement.query(By.css('ds-pagination'));
      paginationEl.triggerEventHandler('sortFieldChange', testEvent);
    });

    it('should call onSortFieldChange on the component', () => {
      expect(comp.onSortFieldChange).toHaveBeenCalledWith(testEvent);
    });
  });

  describe('when the paginationChange output on the pagination is triggered', () => {
    beforeEach(() => {
      spyOn(comp, 'onPaginationChange');
      const paginationEl = fixture.debugElement.query(By.css('ds-pagination'));
      paginationEl.triggerEventHandler('paginationChange', testEvent);
    });

    it('should call onPaginationChange on the component', () => {
      expect(comp.onPaginationChange).toHaveBeenCalledWith(testEvent);
    });
  });

  describe('when onPageChange is triggered with an event', () => {
    beforeEach(() => {
      spyOn(comp.pageChange, 'emit');
      comp.onPageChange(testEvent);
    });

    it('should emit the value from the pageChange EventEmitter', fakeAsync(() => {
      tick(1);
      expect(comp.pageChange.emit).toHaveBeenCalled();
      expect(comp.pageChange.emit).toHaveBeenCalledWith(testEvent);
    }));
  });

  describe('when onPageSizeChange is triggered with an event', () => {
    beforeEach(() => {
      spyOn(comp.pageSizeChange, 'emit');
      comp.onPageSizeChange(testEvent);
    });

    it('should emit the value from the pageSizeChange EventEmitter', fakeAsync(() => {
      tick(1);
      expect(comp.pageSizeChange.emit).toHaveBeenCalled();
      expect(comp.pageSizeChange.emit).toHaveBeenCalledWith(testEvent);
    }));
  });

  describe('when onSortDirectionChange is triggered with an event', () => {
    beforeEach(() => {
      spyOn(comp.sortDirectionChange, 'emit');
      comp.onSortDirectionChange(testEvent);
    });

    it('should emit the value from the sortDirectionChange EventEmitter', fakeAsync(() => {
      tick(1);
      expect(comp.sortDirectionChange.emit).toHaveBeenCalled();
      expect(comp.sortDirectionChange.emit).toHaveBeenCalledWith(testEvent);
    }));
  });

  describe('when onSortFieldChange is triggered with an event', () => {
    beforeEach(() => {
      spyOn(comp.sortFieldChange, 'emit');
      comp.onSortFieldChange(testEvent);
    });

    it('should emit the value from the sortFieldChange EventEmitter', fakeAsync(() => {
      tick(1);
      expect(comp.sortFieldChange.emit).toHaveBeenCalled();
      expect(comp.sortFieldChange.emit).toHaveBeenCalledWith(testEvent);
    }));
  });

  describe('when onPaginationChange is triggered with an event', () => {
    beforeEach(() => {
      spyOn(comp.paginationChange, 'emit');
      comp.onPaginationChange(testEvent);
    });

    it('should emit the value from the paginationChange EventEmitter', fakeAsync(() => {
      tick(1);
      expect(comp.paginationChange.emit).toHaveBeenCalled();
      expect(comp.paginationChange.emit).toHaveBeenCalledWith(testEvent);
    }));
  });
});
