import { Component, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { createPaginatedList, createTestComponent } from '../../../shared/testing/utils.test';
import { MyDSpaceNewExternalDropdownComponent } from './my-dspace-new-external-dropdown.component';
import { EntityTypeDataService } from '../../../core/data/entity-type-data.service';
import { ItemType } from '../../../core/shared/item-relationships/item-type.model';
import { ResourceType } from '../../../core/shared/resource-type';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { PageInfo } from '../../../core/shared/page-info.model';
import { RouterStub } from '../../../shared/testing/router.stub';
import { BrowserOnlyMockPipe } from '../../../shared/testing/browser-only-mock.pipe';

export function getMockEntityTypeService(): EntityTypeDataService {
  const pageInfo = { elementsPerPage: 20, totalElements: 4, totalPages: 1, currentPage: 0 } as PageInfo;
  const type1: ItemType = {
    id: '1',
    label: 'Publication',
    uuid: '1',
    type: new ResourceType('entitytype'),
    _links: undefined
  };
  const type2: ItemType = {
    id: '2',
    label: 'Journal',
    uuid: '2',
    type: new ResourceType('entitytype'),
    _links: undefined
  };
  const type3: ItemType = {
    id: '2',
    label: 'DataPackage',
    uuid: '2',
    type: new ResourceType('entitytype'),
    _links: undefined
  };
  const rd$ = createSuccessfulRemoteDataObject$(createPaginatedList([type1, type2, type3]));
  return jasmine.createSpyObj('entityTypeService', {
    getAllAuthorizedRelationshipTypeImport: rd$,
    hasMoreThanOneAuthorizedImport: observableOf(true)
  });
}

export function getMockEmptyEntityTypeService(): EntityTypeDataService {
  const pageInfo = { elementsPerPage: 20, totalElements: 1, totalPages: 1, currentPage: 0 } as PageInfo;
  const type1: ItemType = {
    id: '1',
    label: 'Publication',
    uuid: '1',
    type: new ResourceType('entitytype'),
    _links: undefined
  };
  const rd$ = createSuccessfulRemoteDataObject$(createPaginatedList([type1]));
  return jasmine.createSpyObj('entityTypeService', {
    getAllAuthorizedRelationshipTypeImport: rd$,
    hasMoreThanOneAuthorizedImport: observableOf(false)
  });
}

describe('MyDSpaceNewExternalDropdownComponent test', () => {
  let testComp: TestComponent;
  let testFixture: ComponentFixture<TestComponent>;
  let submissionComponent: MyDSpaceNewExternalDropdownComponent;
  let submissionComponentFixture: ComponentFixture<MyDSpaceNewExternalDropdownComponent>;

  const entityType1: ItemType = {
    id: '1',
    label: 'Publication',
    uuid: '1',
    type: new ResourceType('entitytype'),
    _links: undefined
  };

  describe('With only one Entity', () => {
    beforeEach(waitForAsync(() => {
      TestBed.configureTestingModule({
        imports: [
          CommonModule,
          TranslateModule.forRoot(),
        ],
        declarations: [
          MyDSpaceNewExternalDropdownComponent,
          TestComponent,
          BrowserOnlyMockPipe
        ],
        providers: [
          { provide: EntityTypeDataService, useValue: getMockEmptyEntityTypeService() },
          { provide: Router, useValue: new RouterStub() },
          MyDSpaceNewExternalDropdownComponent
        ],
        schemas: [NO_ERRORS_SCHEMA]
      }).compileComponents();

      const html = `<ds-my-dspace-new-submission (uploadEnd)="reload($event)"></ds-my-dspace-new-submission>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;

      submissionComponentFixture = TestBed.createComponent(MyDSpaceNewExternalDropdownComponent);
      submissionComponent = submissionComponentFixture.componentInstance;
      submissionComponentFixture.detectChanges();
    }));

    afterEach(() => {
      testFixture.destroy();
      submissionComponentFixture.destroy();
    });

    it('should create MyDSpaceNewExternalDropdownComponent', inject([MyDSpaceNewExternalDropdownComponent], (app: MyDSpaceNewExternalDropdownComponent) => {
      expect(app).toBeDefined();
    }));

    it('should be a single button', inject([MyDSpaceNewExternalDropdownComponent], (app: MyDSpaceNewExternalDropdownComponent) => {
      submissionComponentFixture.detectChanges();
      const addDivElement: DebugElement = submissionComponentFixture.debugElement.query(By.css('.add'));
      const addDiv = addDivElement.nativeElement;
      expect(addDiv.innerHTML).toBeDefined();
      const buttonElement: DebugElement = addDivElement.query(By.css('.btn'));
      const button = buttonElement.nativeElement;
      expect(button.innerHTML).toBeDefined();
      const dropdownElement: DebugElement = submissionComponentFixture.debugElement.query(By.css('.dropdown-menu'));
      expect(dropdownElement).toBeNull();
    }));
  });

  describe('With more than one Entity', () => {
    beforeEach(waitForAsync(() => {
      TestBed.configureTestingModule({
        imports: [
          CommonModule,
          TranslateModule.forRoot(),
        ],
        declarations: [
          MyDSpaceNewExternalDropdownComponent,
          TestComponent,
          BrowserOnlyMockPipe,
        ],
        providers: [
          { provide: EntityTypeDataService, useValue: getMockEntityTypeService() },
          { provide: Router, useValue: new RouterStub() },
          MyDSpaceNewExternalDropdownComponent
        ],
        schemas: [NO_ERRORS_SCHEMA]
      }).compileComponents();

      const html = `<ds-my-dspace-new-submission (uploadEnd)="reload($event)"></ds-my-dspace-new-submission>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;

      submissionComponentFixture = TestBed.createComponent(MyDSpaceNewExternalDropdownComponent);
      submissionComponent = submissionComponentFixture.componentInstance;
      submissionComponentFixture.detectChanges();
    }));

    afterEach(() => {
      testFixture.destroy();
      submissionComponentFixture.destroy();
    });

    it('should create MyDSpaceNewExternalDropdownComponent', inject([MyDSpaceNewExternalDropdownComponent], (app: MyDSpaceNewExternalDropdownComponent) => {
      expect(app).toBeDefined();
    }));

    it('should be a dropdown button', inject([MyDSpaceNewExternalDropdownComponent], (app: MyDSpaceNewExternalDropdownComponent) => {
      const dropdownElement: DebugElement = submissionComponentFixture.debugElement.query(By.css('.dropdown-menu'));
      const dropdown = dropdownElement.nativeElement;
      expect(dropdown.innerHTML).toBeDefined();
    }));

    it('should invoke modalService.open', () => {
      submissionComponent.openPage(entityType1);

      expect((submissionComponent as any).router.navigate).toHaveBeenCalled();
    });
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {
  reload = (event) => {
    return;
  };
}
