import { SearchHierarchyFilterComponent } from './search-hierarchy-filter.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { DebugElement, EventEmitter, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { VocabularyService } from '../../../../../core/submission/vocabularies/vocabulary.service';
import { of as observableOf, BehaviorSubject } from 'rxjs';
import { RemoteData } from '../../../../../core/data/remote-data';
import { RequestEntryState } from '../../../../../core/data/request-entry-state.model';
import { TranslateModule } from '@ngx-translate/core';
import { RouterStub } from '../../../../testing/router.stub';
import { buildPaginatedList } from '../../../../../core/data/paginated-list.model';
import { PageInfo } from '../../../../../core/shared/page-info.model';
import { CommonModule } from '@angular/common';
import { SearchService } from '../../../../../core/shared/search/search.service';
import {
  FILTER_CONFIG,
  IN_PLACE_SEARCH,
  SearchFilterService,
  REFRESH_FILTER
} from '../../../../../core/shared/search/search-filter.service';
import { RemoteDataBuildService } from '../../../../../core/cache/builders/remote-data-build.service';
import { Router } from '@angular/router';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { SEARCH_CONFIG_SERVICE } from '../../../../../my-dspace-page/my-dspace-page.component';
import { SearchConfigurationServiceStub } from '../../../../testing/search-configuration-service.stub';
import { VocabularyEntryDetail } from '../../../../../core/submission/vocabularies/models/vocabulary-entry-detail.model';
import { FacetValue} from '../../../models/facet-value.model';
import { SearchFilterConfig } from '../../../models/search-filter-config.model';

describe('SearchHierarchyFilterComponent', () => {

  let fixture: ComponentFixture<SearchHierarchyFilterComponent>;
  let showVocabularyTreeLink: DebugElement;

  const testSearchLink = 'test-search';
  const testSearchFilter = 'test-search-filter';
  const VocabularyTreeViewComponent = {
    select: new EventEmitter<VocabularyEntryDetail>(),
  };

  const searchService = {
    getSearchLink: () => testSearchLink,
    getFacetValuesFor: () => observableOf([]),
  };
  const searchFilterService = {
    getPage: () => observableOf(0),
  };
  const router = new RouterStub();
  const ngbModal = jasmine.createSpyObj('modal', {
    open: {
      componentInstance: VocabularyTreeViewComponent,
    }
  });
  const vocabularyService = {
    searchTopEntries: () => undefined,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        NgbModule,
        TranslateModule.forRoot(),
      ],
      declarations: [
        SearchHierarchyFilterComponent,
      ],
      providers: [
        { provide: SearchService, useValue: searchService },
        { provide: SearchFilterService, useValue: searchFilterService },
        { provide: RemoteDataBuildService, useValue: {} },
        { provide: Router, useValue: router },
        { provide: NgbModal, useValue: ngbModal },
        { provide: VocabularyService, useValue: vocabularyService },
        { provide: SEARCH_CONFIG_SERVICE, useValue: new SearchConfigurationServiceStub() },
        { provide: IN_PLACE_SEARCH, useValue: false },
        { provide: FILTER_CONFIG, useValue: Object.assign(new SearchFilterConfig(), { name: testSearchFilter }) },
        { provide: REFRESH_FILTER, useValue: new BehaviorSubject<boolean>(false)}
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  function init() {
    fixture = TestBed.createComponent(SearchHierarchyFilterComponent);
    fixture.detectChanges();
    showVocabularyTreeLink = fixture.debugElement.query(By.css('a#show-test-search-filter-tree'));
  }

  describe('if the vocabulary doesn\'t exist', () => {

    beforeEach(() => {
      spyOn(vocabularyService, 'searchTopEntries').and.returnValue(observableOf(new RemoteData(
        undefined, 0, 0, RequestEntryState.Error, undefined, undefined, 404
      )));
      init();
    });

    it('should not show the vocabulary tree link', () => {
      expect(showVocabularyTreeLink).toBeNull();
    });
  });

  describe('if the vocabulary exists', () => {

    beforeEach(() => {
      spyOn(vocabularyService, 'searchTopEntries').and.returnValue(observableOf(new RemoteData(
        undefined, 0, 0, RequestEntryState.Success, undefined, buildPaginatedList(new PageInfo(), []), 200
      )));
      init();
    });

    it('should show the vocabulary tree link', () => {
      expect(showVocabularyTreeLink).toBeTruthy();
    });

    describe('when clicking the vocabulary tree link', () => {

      const alreadySelectedValues = [
        'already-selected-value-1',
        'already-selected-value-2',
      ];
      const newSelectedValue = 'new-selected-value';

      beforeEach(async () => {
        showVocabularyTreeLink.nativeElement.click();
        fixture.componentInstance.selectedValues$ = observableOf(
          alreadySelectedValues.map(value => Object.assign(new FacetValue(), { value }))
        );
        VocabularyTreeViewComponent.select.emit(Object.assign(new VocabularyEntryDetail(), {
          value: newSelectedValue,
        }));
      });

      it('should open the vocabulary tree modal', () => {
        expect(ngbModal.open).toHaveBeenCalled();
      });

      describe('when selecting a value from the vocabulary tree', () => {

        it('should add a new search filter to the existing search filters', () => {
          waitForAsync(() => expect(router.navigate).toHaveBeenCalledWith([testSearchLink], {
            queryParams: {
              [`f.${testSearchFilter}`]: [
                ...alreadySelectedValues,
                newSelectedValue,
              ].map((value => `${value},equals`)),
            },
            queryParamsHandling: 'merge',
          }));
        });
      });
    });
  });
});
