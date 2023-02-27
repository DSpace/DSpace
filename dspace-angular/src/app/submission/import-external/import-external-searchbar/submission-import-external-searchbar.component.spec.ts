import { ChangeDetectorRef, Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import {
  SourceElement,
  SubmissionImportExternalSearchbarComponent
} from './submission-import-external-searchbar.component';
import { ExternalSourceDataService } from '../../../core/data/external-source-data.service';
import { createTestComponent } from '../../../shared/testing/utils.test';
import {
  externalSourceCiencia,
  externalSourceMyStaffDb,
  externalSourceOrcid,
  getMockExternalSourceService
} from '../../../shared/mocks/external-source.service.mock';
import { PageInfo } from '../../../core/shared/page-info.model';
import { buildPaginatedList, PaginatedList } from '../../../core/data/paginated-list.model';
import { createSuccessfulRemoteDataObject } from '../../../shared/remote-data.utils';
import { ExternalSource } from '../../../core/shared/external-source.model';
import { HostWindowService } from '../../../shared/host-window.service';
import { HostWindowServiceStub } from '../../../shared/testing/host-window-service.stub';
import { getTestScheduler } from 'jasmine-marbles';
import { TestScheduler } from 'rxjs/testing';
import { RequestParam } from '../../../core/cache/models/request-param.model';
import { FindListOptions } from '../../../core/data/find-list-options.model';

describe('SubmissionImportExternalSearchbarComponent test suite', () => {
  let comp: SubmissionImportExternalSearchbarComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<SubmissionImportExternalSearchbarComponent>;
  let scheduler: TestScheduler;
  let sourceList: SourceElement[];
  let paginatedList: PaginatedList<ExternalSource>;

  const mockExternalSourceService: any = getMockExternalSourceService();
  paginatedList = buildPaginatedList(new PageInfo(), [externalSourceOrcid, externalSourceCiencia, externalSourceMyStaffDb]);
  let paginatedListRD = createSuccessfulRemoteDataObject(paginatedList);

  beforeEach(waitForAsync(() => {
    scheduler = getTestScheduler();
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
      ],
      declarations: [
        SubmissionImportExternalSearchbarComponent,
        TestComponent,
      ],
      providers: [
        { provide: ExternalSourceDataService, useValue: mockExternalSourceService },
        ChangeDetectorRef,
        { provide: HostWindowService, useValue: new HostWindowServiceStub(800) },
        SubmissionImportExternalSearchbarComponent
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents().then();
  }));

  // First test to check the correct component creation
  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      mockExternalSourceService.searchBy.and.returnValue(observableOf(paginatedListRD));
      const html = `
        <ds-submission-import-external-searchbar [initExternalSourceData]="initExternalSourceData"></ds-submission-import-external-searchbar>`;
      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create SubmissionImportExternalSearchbarComponent', inject([SubmissionImportExternalSearchbarComponent], (app: SubmissionImportExternalSearchbarComponent) => {
      expect(app).toBeDefined();
    }));
  });

  describe('', () => {

    beforeEach(() => {
      fixture = TestBed.createComponent(SubmissionImportExternalSearchbarComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      const pageInfo = new PageInfo();
      paginatedList = buildPaginatedList(pageInfo, [externalSourceOrcid, externalSourceCiencia, externalSourceMyStaffDb]);
      paginatedListRD = createSuccessfulRemoteDataObject(paginatedList);
      compAsAny.externalService.searchBy.and.returnValue(observableOf(paginatedListRD));
      sourceList = [
        {id: 'orcid', name: 'orcid'},
        {id: 'ciencia', name: 'ciencia'},
        {id: 'my_staff_db', name: 'my_staff_db'},
      ];
    });

    afterEach(() => {
      fixture.destroy();
      comp = null;
      compAsAny = null;
    });

    it('Should init component properly (without initExternalSourceData)', () => {
      comp.initExternalSourceData = { entity: 'Publication', sourceId: '', query: '' };
      scheduler.schedule(() => fixture.detectChanges());
      scheduler.flush();

      expect(comp.selectedElement).toEqual(sourceList[0]);
      expect(compAsAny.pageInfo).toEqual(paginatedList.pageInfo);
      expect(comp.sourceList).toEqual(sourceList);
    });

    it('Should init component properly (with initExternalSourceData populated)', () => {
      comp.initExternalSourceData = { entity: 'Publication', query: 'dummy', sourceId: 'ciencia' };
      scheduler.schedule(() => fixture.detectChanges());
      scheduler.flush();

      expect(comp.selectedElement).toEqual(sourceList[1]);
      expect(compAsAny.pageInfo).toEqual(paginatedList.pageInfo);
      expect(comp.sourceList).toEqual(sourceList);
    });

    it('Variable \'selectedElement\' should be assigned', () => {
      const selectedElement = {id: 'orcid', name: 'orcid'};
      comp.makeSourceSelection(selectedElement);
      expect(comp.selectedElement).toEqual(selectedElement);
    });

    it('Should load additional external sources', () => {
      comp.initExternalSourceData = { entity: 'Publication', query: 'dummy', sourceId: 'ciencia' };
      comp.sourceListLoading = false;
      compAsAny.pageInfo = new PageInfo({
        elementsPerPage: 3,
        totalElements: 6,
        totalPages: 2,
        currentPage: 0
      });
      compAsAny.findListOptions = Object.assign({}, new FindListOptions(), {
        elementsPerPage: 3,
        currentPage: 0,
        searchParams: [
          new RequestParam('entityType', 'Publication')
        ]
      });
      comp.sourceList = sourceList;
      const expected = sourceList.concat(sourceList);

      scheduler.schedule(() => comp.onScroll());
      scheduler.flush();

      expect(comp.sourceList).toEqual(expected);
    });

    it('The \'search\' method should call \'emit\'', () => {
      comp.initExternalSourceData = { entity: 'Publication', query: 'dummy', sourceId: 'ciencia' };
      comp.selectedElement = { id: 'orcidV2', name: 'orcidV2' };
      comp.searchString = 'dummy';
      const expected = { entity: 'Publication', sourceId: comp.selectedElement.id, query: comp.searchString };
      spyOn(comp.externalSourceData, 'emit');
      comp.search();

      expect(comp.externalSourceData.emit).toHaveBeenCalledWith(expected);
    });
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {
  initExternalSourceData = { entity: 'Publication', query: 'dummy', sourceId: 'ciencia' };
}
