import { ItemVersionsComponent } from './item-versions.component';
import {
  ComponentFixture, TestBed, waitForAsync
} from '@angular/core/testing';
import { VarDirective } from '../../shared/utils/var.directive';
import { TranslateModule } from '@ngx-translate/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Item } from '../../core/shared/item.model';
import { Version } from '../../core/shared/version.model';
import { VersionHistory } from '../../core/shared/version-history.model';
import { VersionHistoryDataService } from '../../core/data/version-history-data.service';
import { BrowserModule, By } from '@angular/platform-browser';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { EMPTY, of, of as observableOf } from 'rxjs';
import { PaginationService } from '../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../shared/testing/pagination-service.stub';
import { AuthService } from '../../core/auth/auth.service';
import { VersionDataService } from '../../core/data/version-data.service';
import { ItemDataService } from '../../core/data/item-data.service';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { FeatureID } from '../../core/data/feature-authorization/feature-id';
import { WorkspaceitemDataService } from '../../core/submission/workspaceitem-data.service';
import { WorkflowItemDataService } from '../../core/submission/workflowitem-data.service';
import { ConfigurationDataService } from '../../core/data/configuration-data.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ItemSharedModule } from '../item-shared.module';

describe('ItemVersionsComponent', () => {
  let component: ItemVersionsComponent;
  let fixture: ComponentFixture<ItemVersionsComponent>;
  let authenticationService: AuthService;
  let authorizationService: AuthorizationDataService;
  let versionHistoryService: VersionHistoryDataService;
  let workspaceItemDataService: WorkspaceitemDataService;
  let workflowItemDataService: WorkflowItemDataService;
  let versionService: VersionDataService;
  let configurationService: ConfigurationDataService;

  const versionHistory = Object.assign(new VersionHistory(), {
    id: '1',
    draftVersion: true,
  });

  const version1 = Object.assign(new Version(), {
    id: '1',
    version: 1,
    created: new Date(2020, 1, 1),
    summary: 'first version',
    versionhistory: createSuccessfulRemoteDataObject$(versionHistory),
    _links: {
      self: {
        href: 'version2-url',
      },
    },
  });
  const version2 = Object.assign(new Version(), {
    id: '2',
    version: 2,
    summary: 'second version',
    created: new Date(2020, 1, 2),
    versionhistory: createSuccessfulRemoteDataObject$(versionHistory),
    _links: {
      self: {
        href: 'version2-url',
      },
    },
  });
  const versions = [version1, version2];
  versionHistory.versions = createSuccessfulRemoteDataObject$(createPaginatedList(versions));

  const item1 = Object.assign(new Item(), { // is a workspace item
    id: 'item-identifier-1',
    uuid: 'item-identifier-1',
    handle: '123456789/1',
    version: createSuccessfulRemoteDataObject$(version1),
    _links: {
      self: {
        href: '/items/item-identifier-1'
      }
    }
  });
  const item2 = Object.assign(new Item(), {
    id: 'item-identifier-2',
    uuid: 'item-identifier-2',
    handle: '123456789/2',
    version: createSuccessfulRemoteDataObject$(version2),
    _links: {
      self: {
        href: '/items/item-identifier-2'
      }
    }
  });
  const items = [item1, item2];
  version1.item = createSuccessfulRemoteDataObject$(item1);
  version2.item = createSuccessfulRemoteDataObject$(item2);

  const versionHistoryServiceSpy = jasmine.createSpyObj('versionHistoryService', {
    getVersions: createSuccessfulRemoteDataObject$(createPaginatedList(versions)),
    getVersionHistoryFromVersion$: of(versionHistory),
    getLatestVersionItemFromHistory$: of(item1),  // called when version2 is deleted
  });
  const authenticationServiceSpy = jasmine.createSpyObj('authenticationService', {
    isAuthenticated: observableOf(true),
    setRedirectUrl: {}
  });
  const authorizationServiceSpy = jasmine.createSpyObj('authorizationService', {
    isAuthorized: observableOf(true)
  });
  const workspaceItemDataServiceSpy = jasmine.createSpyObj('workspaceItemDataService', {
    findByItem: EMPTY,
  });
  const workflowItemDataServiceSpy = jasmine.createSpyObj('workflowItemDataService', {
    findByItem: EMPTY,
  });
  const versionServiceSpy = jasmine.createSpyObj('versionService', {
    findById: EMPTY,
  });

  const configurationServiceSpy = jasmine.createSpyObj('configurationService', {
    findByPropertyName: of(true),
  });

  const itemDataServiceSpy = jasmine.createSpyObj('itemDataService', {
    delete: createSuccessfulRemoteDataObject$({}),
  });

  const routerSpy = jasmine.createSpyObj('router', {
    navigateByUrl: null,
  });

  beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      declarations: [ItemVersionsComponent, VarDirective],
      imports: [TranslateModule.forRoot(), CommonModule, FormsModule, ReactiveFormsModule, BrowserModule, ItemSharedModule],
      providers: [
        {provide: PaginationService, useValue: new PaginationServiceStub()},
        {provide: FormBuilder, useValue: new FormBuilder()},
        {provide: NotificationsService, useValue: new NotificationsServiceStub()},
        {provide: AuthService, useValue: authenticationServiceSpy},
        {provide: AuthorizationDataService, useValue: authorizationServiceSpy},
        {provide: VersionHistoryDataService, useValue: versionHistoryServiceSpy},
        {provide: ItemDataService, useValue: itemDataServiceSpy},
        {provide: VersionDataService, useValue: versionServiceSpy},
        {provide: WorkspaceitemDataService, useValue: workspaceItemDataServiceSpy},
        {provide: WorkflowItemDataService, useValue: workflowItemDataServiceSpy},
        {provide: ConfigurationDataService, useValue: configurationServiceSpy},
        { provide: Router, useValue: routerSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    versionHistoryService = TestBed.inject(VersionHistoryDataService);
    authenticationService = TestBed.inject(AuthService);
    authorizationService = TestBed.inject(AuthorizationDataService);
    workspaceItemDataService = TestBed.inject(WorkspaceitemDataService);
    workflowItemDataService = TestBed.inject(WorkflowItemDataService);
    versionService = TestBed.inject(VersionDataService);
    configurationService = TestBed.inject(ConfigurationDataService);

  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemVersionsComponent);
    component = fixture.componentInstance;
    component.item = item1;
    component.displayActions = true;
    fixture.detectChanges();
  });

  it(`should display ${versions.length} rows`, () => {
    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows.length).toBe(versions.length);
  });

  versions.forEach((version: Version, index: number) => {
    const versionItem = items[index];

    it(`should display version ${version.version} in the correct column for version ${version.id}`, () => {
      const id = fixture.debugElement.query(By.css(`#version-row-${version.id} .version-row-element-version`));
      expect(id.nativeElement.textContent).toContain(version.version.toString());
    });

    // Check if the current version contains an asterisk
    if (item1.uuid === versionItem.uuid) {
      it('should add an asterisk to the version of the selected item', () => {
        const item = fixture.debugElement.query(By.css(`#version-row-${version.id} .version-row-element-version`));
        expect(item.nativeElement.textContent).toContain('*');
      });
    }

    it(`should display date ${version.created} in the correct column for version ${version.id}`, () => {
      const date = fixture.debugElement.query(By.css(`#version-row-${version.id} .version-row-element-date`));
      switch (versionItem.uuid) {
        case item1.uuid:
          expect(date.nativeElement.textContent.trim()).toEqual('2020-02-01 00:00:00');
          break;
        case item2.uuid:
          expect(date.nativeElement.textContent.trim()).toEqual('2020-02-02 00:00:00');
          break;
        default:
          throw new Error('Unexpected versionItem');
      }
    });

    it(`should display summary ${version.summary} in the correct column for version ${version.id}`, () => {
      const summary = fixture.debugElement.query(By.css(`#version-row-${version.id} .version-row-element-summary`));
      expect(summary.nativeElement.textContent).toEqual(version.summary);
    });
  });

  describe('when the user can only delete a version', () => {
    beforeAll(waitForAsync(() => {
      const canDelete = (featureID: FeatureID, url: string ) => of(featureID === FeatureID.CanDeleteVersion);
      authorizationServiceSpy.isAuthorized.and.callFake(canDelete);
    }));
    it('should not disable the delete button', () => {
      const deleteButtons = fixture.debugElement.queryAll(By.css(`.version-row-element-delete`));
      deleteButtons.forEach((btn) => {
        expect(btn.nativeElement.disabled).toBe(false);
      });
    });
    it('should disable other buttons', () => {
      const createButtons = fixture.debugElement.queryAll(By.css(`.version-row-element-create`));
      createButtons.forEach((btn) => {
        expect(btn.nativeElement.disabled).toBe(true);
      });
      const editButtons = fixture.debugElement.queryAll(By.css(`.version-row-element-create`));
      editButtons.forEach((btn) => {
        expect(btn.nativeElement.disabled).toBe(true);
      });
    });
  });

  describe('when page is changed', () => {
    it('should call getAllVersions', () => {
      spyOn(component, 'getAllVersions');
      component.onPageChange();
      expect(component.getAllVersions).toHaveBeenCalled();
    });
  });

  describe('when onSummarySubmit() is called', () => {
    const id = 'version-being-edited-id';
    beforeEach(() => {
      component.versionBeingEditedId = id;
    });
    it('should call versionService.findById', () => {
      component.onSummarySubmit();
      expect(versionService.findById).toHaveBeenCalledWith(id);
    });
  });

  describe('when editing is enabled for an item', () => {
    beforeEach(() => {
      component.enableVersionEditing(version1);
    });
    it('should set all variables', () => {
      expect(component.versionBeingEditedSummary).toEqual('first version');
      expect(component.versionBeingEditedNumber).toEqual(1);
      expect(component.versionBeingEditedId).toEqual('1');
    });
    it('isAnyBeingEdited should be true', () => {
      expect(component.isAnyBeingEdited()).toBeTrue();
    });
    it('isThisBeingEdited should be true for version1', () => {
      expect(component.isThisBeingEdited(version1)).toBeTrue();
    });
    it('isThisBeingEdited should be false for version2', () => {
      expect(component.isThisBeingEdited(version2)).toBeFalse();
    });
  });

  describe('when editing is disabled', () => {
    beforeEach(() => {
      component.disableVersionEditing();
    });
    it('should unset all variables', () => {
      expect(component.versionBeingEditedSummary).toBeUndefined();
      expect(component.versionBeingEditedNumber).toBeUndefined();
      expect(component.versionBeingEditedId).toBeUndefined();
    });
    it('isAnyBeingEdited should be false', () => {
      expect(component.isAnyBeingEdited()).toBeFalse();
    });
    it('isThisBeingEdited should be false for all versions', () => {
      expect(component.isThisBeingEdited(version1)).toBeFalse();
      expect(component.isThisBeingEdited(version2)).toBeFalse();
    });
  });

  describe('when deleting a version', () => {
    let deleteButton;

    beforeEach(() => {
      const canDelete = (featureID: FeatureID, url: string ) => of(featureID === FeatureID.CanDeleteVersion);
      authorizationServiceSpy.isAuthorized.and.callFake(canDelete);

      fixture.detectChanges();

      // delete the last version in the table (version2 → item2)
      deleteButton = fixture.debugElement.queryAll(By.css('.version-row-element-delete'))[1].nativeElement;

      itemDataServiceSpy.delete.calls.reset();
    });

    describe('if confirmed via modal', () => {
      beforeEach(waitForAsync(() => {
        deleteButton.click();
        fixture.detectChanges();
        (document as any).querySelector('.modal-footer .confirm').click();
      }));

      it('should call ItemService.delete', () => {
        expect(itemDataServiceSpy.delete).toHaveBeenCalledWith(item2.id);
      });
    });

    describe('if canceled via modal', () => {
      beforeEach(waitForAsync(() => {
        deleteButton.click();
        fixture.detectChanges();
        (document as any).querySelector('.modal-footer .cancel').click();
      }));

      it('should not call ItemService.delete', () => {
        expect(itemDataServiceSpy.delete).not.toHaveBeenCalled();
      });
    });
  });
});
