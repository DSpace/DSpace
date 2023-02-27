import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { SearchConfigurationService } from '../../../../../../core/shared/search/search-configuration.service';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { VarDirective } from '../../../../../utils/var.directive';
import { Observable, of as observableOf } from 'rxjs';
import { PaginatedSearchOptions } from '../../../../../search/models/paginated-search-options.model';
import { ItemSearchResult } from '../../../../../object-collection/shared/item-search-result.model';
import { Item } from '../../../../../../core/shared/item.model';
import { DsDynamicLookupRelationSelectionTabComponent } from './dynamic-lookup-relation-selection-tab.component';
import { PaginationComponentOptions } from '../../../../../pagination/pagination-component-options.model';
import { Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { RemoteData } from '../../../../../../core/data/remote-data';
import { buildPaginatedList, PaginatedList } from '../../../../../../core/data/paginated-list.model';
import { ListableObject } from '../../../../../object-collection/shared/listable-object.model';
import { createSuccessfulRemoteDataObject$ } from '../../../../../remote-data.utils';
import { PaginationService } from '../../../../../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../../../../testing/pagination-service.stub';

describe('DsDynamicLookupRelationSelectionTabComponent', () => {
  let component: DsDynamicLookupRelationSelectionTabComponent;
  let fixture: ComponentFixture<DsDynamicLookupRelationSelectionTabComponent>;
  let pSearchOptions = new PaginatedSearchOptions({ pagination: new PaginationComponentOptions() });
  let item1 = Object.assign(new Item(), { uuid: 'e1c51c69-896d-42dc-8221-1d5f2ad5516e' });
  let item2 = Object.assign(new Item(), { uuid: 'c8279647-1acc-41ae-b036-951d5f65649b' });
  let searchResult1 = Object.assign(new ItemSearchResult(), { indexableObject: item1 });
  let searchResult2 = Object.assign(new ItemSearchResult(), { indexableObject: item2 });
  let listID = '6b0c8221-fcb4-47a8-b483-ca32363fffb3';
  let selection$;
  let selectionRD$;
  let router;

  function init() {
    pSearchOptions = new PaginatedSearchOptions({ pagination: new PaginationComponentOptions() });
    item1 = Object.assign(new Item(), { uuid: 'e1c51c69-896d-42dc-8221-1d5f2ad5516e' });
    item2 = Object.assign(new Item(), { uuid: 'c8279647-1acc-41ae-b036-951d5f65649b' });
    searchResult1 = Object.assign(new ItemSearchResult(), { indexableObject: item1 });
    searchResult2 = Object.assign(new ItemSearchResult(), { indexableObject: item2 });
    listID = '6b0c8221-fcb4-47a8-b483-ca32363fffb3';
    selection$ = observableOf([searchResult1, searchResult2]);
    selectionRD$ = createSelection([searchResult1, searchResult2]);
    router = jasmine.createSpyObj('router', ['navigate']);
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      declarations: [DsDynamicLookupRelationSelectionTabComponent, VarDirective],
      imports: [TranslateModule.forRoot()],
      providers: [
        {
          provide: SearchConfigurationService, useValue: {
            paginatedSearchOptions: observableOf(pSearchOptions)
          },
        },
        {
          provide: Router, useValue: router
        },
        {
          provide: PaginationService, useValue: new PaginationServiceStub()
        }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DsDynamicLookupRelationSelectionTabComponent);
    component = fixture.componentInstance;
    component.selection$ = selection$;
    component.listId = listID;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call navigate on the router when is called resetRoute', () => {
    component.selectionRD$ = createSelection([]);
    fixture.detectChanges();
    const colComponent = fixture.debugElement.query(By.css('ds-viewable-collection'));
    expect(colComponent).toBe(null);
  });

  it('should call navigate on the router when is called resetRoute', () => {
    component.selectionRD$ = selectionRD$;
    const colComponent = fixture.debugElement.query(By.css('ds-viewable-collection'));
    expect(colComponent).not.toBe(null);
  });
});

function createSelection(content: ListableObject[]): Observable<RemoteData<PaginatedList<ListableObject>>> {
  return createSuccessfulRemoteDataObject$(buildPaginatedList(undefined, content));
}
