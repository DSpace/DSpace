import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectorRef, Component, Injector, NO_ERRORS_SCHEMA } from '@angular/core';

import { of as observableOf } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { cold } from 'jasmine-marbles';
import uniqueId from 'lodash/uniqueId';

import { createSuccessfulRemoteDataObject } from '../remote-data.utils';
import { createTestComponent } from '../testing/utils.test';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import { GroupDataService } from '../../core/eperson/group-data.service';
import { RequestService } from '../../core/data/request.service';
import { getMockRequestService } from '../mocks/request.service.mock';
import { EpersonGroupListComponent, SearchEvent } from './eperson-group-list.component';
import { EPersonMock } from '../testing/eperson.mock';
import { GroupMock } from '../testing/group-mock';
import { PaginationComponentOptions } from '../pagination/pagination-component-options.model';
import { buildPaginatedList } from '../../core/data/paginated-list.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PaginationService } from '../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../testing/pagination-service.stub';

describe('EpersonGroupListComponent test suite', () => {
  let comp: EpersonGroupListComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<EpersonGroupListComponent>;
  let de;
  let groupService: any;
  let epersonService: any;
  let paginationService;

  const paginationOptions: PaginationComponentOptions = new PaginationComponentOptions();
  paginationOptions.id = uniqueId('eperson-group-list-pagination-test');
  paginationOptions.pageSize = 5;

  const mockEpersonService = jasmine.createSpyObj('epersonService',
    {
      findByHref: jasmine.createSpy('findByHref'),
      findAll: jasmine.createSpy('findAll'),
      searchByScope: jasmine.createSpy('searchByScope'),
    },
    {
      linkPath: 'epersons'
    }
  );

  const mockGroupService = jasmine.createSpyObj('groupService',
    {
      findByHref: jasmine.createSpy('findByHref'),
      findAll: jasmine.createSpy('findAll'),
      searchGroups: jasmine.createSpy('searchGroups'),
    },
    {
      linkPath: 'groups'
    }
  );

  const epersonPaginatedList = buildPaginatedList(new PageInfo(), [EPersonMock, EPersonMock]);
  const epersonPaginatedListRD = createSuccessfulRemoteDataObject(epersonPaginatedList);

  const groupPaginatedList = buildPaginatedList(new PageInfo(), [GroupMock, GroupMock]);
  const groupPaginatedListRD = createSuccessfulRemoteDataObject(groupPaginatedList);

  paginationService = new PaginationServiceStub();

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot()
      ],
      declarations: [
        EpersonGroupListComponent,
        TestComponent
      ],
      providers: [
        { provide: EPersonDataService, useValue: mockEpersonService },
        { provide: GroupDataService, useValue: mockGroupService },
        { provide: RequestService, useValue: getMockRequestService() },
        { provide: PaginationService, useValue: paginationService },
        EpersonGroupListComponent,
        ChangeDetectorRef,
        Injector
      ],
      schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();
  }));

  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      mockEpersonService.searchByScope.and.returnValue(observableOf(epersonPaginatedListRD));
      const html = `
        <ds-eperson-group-list [isListOfEPerson]="isListOfEPerson" [initSelected]="initSelected"></ds-eperson-group-list>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create EpersonGroupListComponent', inject([EpersonGroupListComponent], (app: EpersonGroupListComponent) => {

      expect(app).toBeDefined();

    }));
  });

  describe('when is list of eperson', () => {

    beforeEach(() => {
      // initTestScheduler();
      fixture = TestBed.createComponent(EpersonGroupListComponent);
      epersonService = TestBed.inject(EPersonDataService);
      comp = fixture.componentInstance;
      compAsAny = fixture.componentInstance;
      comp.isListOfEPerson = true;
    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      de = null;
      fixture.destroy();
    });

    it('should inject EPersonDataService', () => {
      spyOn(comp, 'updateList');
      fixture.detectChanges();

      expect(compAsAny.dataService).toBeDefined();
      expect(comp.updateList).toHaveBeenCalled();
    });

    it('should init entrySelectedId', () => {
      spyOn(comp, 'updateList');
      comp.initSelected = EPersonMock.id;

      fixture.detectChanges();

      expect(compAsAny.entrySelectedId.value).toBe(EPersonMock.id);
    });

    it('should init the list of eperson', () => {
      epersonService.searchByScope.and.returnValue(observableOf(epersonPaginatedListRD));
      fixture.detectChanges();

      expect(compAsAny.list$.value).toEqual(epersonPaginatedListRD);
      expect(comp.getList()).toBeObservable(cold('a', {
        a: epersonPaginatedListRD
      }));
    });

    it('should emit select event', () => {
      spyOn(comp.select, 'emit');
      comp.emitSelect(EPersonMock);

      expect(comp.select.emit).toHaveBeenCalled();
      expect(compAsAny.entrySelectedId.value).toBe(EPersonMock.id);
    });

    it('should return true when entry is selected', () => {
      compAsAny.entrySelectedId.next(EPersonMock.id);

      expect(comp.isSelected(EPersonMock)).toBeObservable(cold('a', {
        a: true
      }));
    });

    it('should return false when entry is not selected', () => {
      compAsAny.entrySelectedId.next('');

      expect(comp.isSelected(EPersonMock)).toBeObservable(cold('a', {
        a: false
      }));
    });
  });

  describe('when is list of group', () => {

    beforeEach(() => {
      // initTestScheduler();
      fixture = TestBed.createComponent(EpersonGroupListComponent);
      groupService = TestBed.inject(GroupDataService);
      comp = fixture.componentInstance;
      compAsAny = fixture.componentInstance;
      comp.isListOfEPerson = false;
    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      de = null;
      fixture.destroy();
    });

    it('should inject GroupDataService', () => {
      spyOn(comp, 'updateList');
      fixture.detectChanges();

      expect(compAsAny.dataService).toBeDefined();
      expect(comp.updateList).toHaveBeenCalled();
    });

    it('should init entrySelectedId', () => {
      spyOn(comp, 'updateList');
      comp.initSelected = GroupMock.id;

      fixture.detectChanges();

      expect(compAsAny.entrySelectedId.value).toBe(GroupMock.id);
    });

    it('should init the list of group', () => {
      groupService.searchGroups.and.returnValue(observableOf(groupPaginatedListRD));
      fixture.detectChanges();

      expect(compAsAny.list$.value).toEqual(groupPaginatedListRD);
      expect(comp.getList()).toBeObservable(cold('a', {
        a: groupPaginatedListRD
      }));
    });

    it('should emit select event', () => {
      spyOn(comp.select, 'emit');
      comp.emitSelect(GroupMock);

      expect(comp.select.emit).toHaveBeenCalled();
      expect(compAsAny.entrySelectedId.value).toBe(GroupMock.id);
    });

    it('should return true when entry is selected', () => {
      compAsAny.entrySelectedId.next(EPersonMock.id);

      expect(comp.isSelected(EPersonMock)).toBeObservable(cold('a', {
        a: true
      }));
    });

    it('should return false when entry is not selected', () => {
      compAsAny.entrySelectedId.next('');

      expect(comp.isSelected(EPersonMock)).toBeObservable(cold('a', {
        a: false
      }));
    });

    it('should update list on search triggered', () => {
      const options: PaginationComponentOptions = comp.paginationOptions;
      const event: SearchEvent = {
        scope: 'metadata',
        query: 'test'
      };
      spyOn(comp, 'updateList');
      comp.onSearch(event);

      expect(compAsAny.updateList).toHaveBeenCalledWith('metadata', 'test');
    });
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

  isListOfEPerson = true;
  initSelected = '';
}
