import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { ObjectListComponent } from './object-list.component';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { SelectableListService } from './selectable-list/selectable-list.service';

describe('ObjectListComponent', () => {
  let comp: ObjectListComponent;
  let fixture: ComponentFixture<ObjectListComponent>;
  const testEvent: any = { test: 'test' };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [],
      declarations: [ObjectListComponent],
      providers: [{ provide: SelectableListService, useValue: {} }],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ObjectListComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ObjectListComponent);
    comp = fixture.componentInstance; // SearchPageComponent test instance
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
