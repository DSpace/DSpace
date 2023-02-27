import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { BrowserModule, By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { Store } from '@ngrx/store';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { Observable, of as observableOf } from 'rxjs';
import { RemoteDataBuildService } from '../../../core/cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../../../core/cache/object-cache.service';
import { DSOChangeAnalyzer } from '../../../core/data/dso-change-analyzer.service';
import { DSpaceObjectDataService } from '../../../core/data/dspace-object-data.service';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';
import { buildPaginatedList, PaginatedList } from '../../../core/data/paginated-list.model';
import { RemoteData } from '../../../core/data/remote-data';
import { EPersonDataService } from '../../../core/eperson/eperson-data.service';
import { GroupDataService } from '../../../core/eperson/group-data.service';
import { Group } from '../../../core/eperson/models/group.model';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { HALEndpointService } from '../../../core/shared/hal-endpoint.service';
import { PageInfo } from '../../../core/shared/page-info.model';
import { UUIDService } from '../../../core/shared/uuid.service';
import { FormBuilderService } from '../../../shared/form/builder/form-builder.service';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { GroupMock, GroupMock2 } from '../../../shared/testing/group-mock';
import { GroupFormComponent } from './group-form.component';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { getMockFormBuilderService } from '../../../shared/mocks/form-builder-service.mock';
import { getMockTranslateService } from '../../../shared/mocks/translate.service.mock';
import { TranslateLoaderMock } from '../../../shared/testing/translate-loader.mock';
import { RouterMock } from '../../../shared/mocks/router.mock';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { Operation } from 'fast-json-patch';
import { ValidateGroupExists } from './validators/group-exists.validator';
import { NoContent } from '../../../core/shared/NoContent.model';

describe('GroupFormComponent', () => {
  let component: GroupFormComponent;
  let fixture: ComponentFixture<GroupFormComponent>;
  let translateService: TranslateService;
  let builderService: FormBuilderService;
  let ePersonDataServiceStub: any;
  let groupsDataServiceStub: any;
  let dsoDataServiceStub: any;
  let authorizationService: AuthorizationDataService;
  let notificationService: NotificationsServiceStub;
  let router;

  let groups;
  let groupName;
  let groupDescription;
  let expected;

  beforeEach(waitForAsync(() => {
    groups = [GroupMock, GroupMock2];
    groupName = 'testGroupName';
    groupDescription = 'testDescription';
    expected = Object.assign(new Group(), {
      name: groupName,
      metadata: {
        'dc.description': [
          {
            value: groupDescription
          }
        ],
      },
    });
    ePersonDataServiceStub = {};
    groupsDataServiceStub = {
      allGroups: groups,
      activeGroup: null,
      createdGroup: null,
      getActiveGroup(): Observable<Group> {
        return observableOf(this.activeGroup);
      },
      getGroupRegistryRouterLink(): string {
        return '/access-control/groups';
      },
      editGroup(group: Group) {
        this.activeGroup = group;
      },
      clearGroupsRequests() {
        return null;
      },
      patch(group: Group, operations: Operation[]) {
        return null;
      },
      delete(objectId: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
        return createSuccessfulRemoteDataObject$({});
      },
      cancelEditGroup(): void {
        this.activeGroup = null;
      },
      findById(id: string) {
        return observableOf({ payload: null, hasSucceeded: true });
      },
      findByHref(href: string) {
        return createSuccessfulRemoteDataObject$(this.createdGroup);
      },
      create(group: Group): Observable<RemoteData<Group>> {
        this.allGroups = [...this.allGroups, group];
        this.createdGroup = Object.assign({}, group, {
          _links: { self: { href: 'group-selflink' } }
        });
        return createSuccessfulRemoteDataObject$(this.createdGroup);
      },
      searchGroups(query: string): Observable<RemoteData<PaginatedList<Group>>> {
        return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), []));
      },
      getGroupEditPageRouterLinkWithID(id: string) {
        return `group-edit-page-for-${id}`;
      }
    };
    authorizationService = jasmine.createSpyObj('authorizationService', {
      isAuthorized: observableOf(true)
    });
    dsoDataServiceStub = {
      findByHref(href: string): Observable<RemoteData<DSpaceObject>> {
        return null;
      }
    };
    builderService = Object.assign(getMockFormBuilderService(),{
      createFormGroup(formModel, options = null) {
        const controls = {};
        formModel.forEach( model => {
            model.parent = parent;
            const controlModel = model;
            const controlState = { value: controlModel.value, disabled: controlModel.disabled };
            const controlOptions = this.createAbstractControlOptions(controlModel.validators, controlModel.asyncValidators, controlModel.updateOn);
            controls[model.id] = new FormControl(controlState, controlOptions);
        });
        return new FormGroup(controls, options);
      },
      createAbstractControlOptions(validatorsConfig = null, asyncValidatorsConfig = null, updateOn = null) {
        return {
            validators: validatorsConfig !== null ? this.getValidators(validatorsConfig) : null,
        };
      },
      getValidators(validatorsConfig) {
          return this.getValidatorFns(validatorsConfig);
      },
      getValidatorFns(validatorsConfig, validatorsToken = this._NG_VALIDATORS) {
        let validatorFns = [];
        if (this.isObject(validatorsConfig)) {
            validatorFns = Object.keys(validatorsConfig).map(validatorConfigKey => {
                const validatorConfigValue = validatorsConfig[validatorConfigKey];
                if (this.isValidatorDescriptor(validatorConfigValue)) {
                    const descriptor = validatorConfigValue;
                    return this.getValidatorFn(descriptor.name, descriptor.args, validatorsToken);
                }
                return this.getValidatorFn(validatorConfigKey, validatorConfigValue, validatorsToken);
            });
        }
        return validatorFns;
      },
      getValidatorFn(validatorName, validatorArgs = null, validatorsToken = this._NG_VALIDATORS) {
        let validatorFn;
        if (Validators.hasOwnProperty(validatorName)) { // Built-in Angular Validators
            validatorFn = Validators[validatorName];
        } else { // Custom Validators
            if (this._DYNAMIC_VALIDATORS && this._DYNAMIC_VALIDATORS.has(validatorName)) {
                validatorFn = this._DYNAMIC_VALIDATORS.get(validatorName);
            } else if (validatorsToken) {
                validatorFn = validatorsToken.find(validator => validator.name === validatorName);
            }
        }
        if (validatorFn === undefined) { // throw when no validator could be resolved
            throw new Error(`validator '${validatorName}' is not provided via NG_VALIDATORS, NG_ASYNC_VALIDATORS or DYNAMIC_FORM_VALIDATORS`);
        }
        if (validatorArgs !== null) {
            return validatorFn(validatorArgs);
        }
        return validatorFn;
    },
      isValidatorDescriptor(value) {
          if (this.isObject(value)) {
              return value.hasOwnProperty('name') && value.hasOwnProperty('args');
          }
          return false;
      },
      isObject(value) {
        return typeof value === 'object' && value !== null;
      }
    });
    translateService = getMockTranslateService();
    router = new RouterMock();
    notificationService = new NotificationsServiceStub();
    TestBed.configureTestingModule({
      imports: [CommonModule, NgbModule, FormsModule, ReactiveFormsModule, BrowserModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
      ],
      declarations: [GroupFormComponent],
      providers: [GroupFormComponent,
        { provide: EPersonDataService, useValue: ePersonDataServiceStub },
        { provide: GroupDataService, useValue: groupsDataServiceStub },
        { provide: DSpaceObjectDataService, useValue: dsoDataServiceStub },
        { provide: NotificationsService, useValue: notificationService },
        { provide: FormBuilderService, useValue: builderService },
        { provide: DSOChangeAnalyzer, useValue: {} },
        { provide: HttpClient, useValue: {} },
        { provide: ObjectCacheService, useValue: {} },
        { provide: UUIDService, useValue: {} },
        { provide: Store, useValue: {} },
        { provide: RemoteDataBuildService, useValue: {} },
        { provide: HALEndpointService, useValue: {} },
        {
          provide: ActivatedRoute,
          useValue: { data: observableOf({ dso: { payload: {} } }), params: observableOf({}) }
        },
        { provide: Router, useValue: router },
        { provide: AuthorizationDataService, useValue: authorizationService },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('when submitting the form', () => {
    beforeEach(() => {
      spyOn(component.submitForm, 'emit');
      component.groupName.value = groupName;
      component.groupDescription.value = groupDescription;
    });
    describe('without active Group', () => {
      beforeEach(() => {
        component.onSubmit();
        fixture.detectChanges();
      });

      it('should emit a new group using the correct values', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect(component.submitForm.emit).toHaveBeenCalledWith(expected);
        });
      }));
    });
    describe('with active Group', () => {
      let expected2;
      beforeEach(() => {
        expected2 = Object.assign(new Group(), {
          name: 'newGroupName',
          metadata: {
            'dc.description': [
              {
                value: groupDescription
              }
            ],
          },
        });
        spyOn(groupsDataServiceStub, 'getActiveGroup').and.returnValue(observableOf(expected));
        spyOn(groupsDataServiceStub, 'patch').and.returnValue(createSuccessfulRemoteDataObject$(expected2));
        component.groupName.value = 'newGroupName';
        component.onSubmit();
        fixture.detectChanges();
      });

      it('should edit with name and description operations', () => {
        const operations = [{
          op: 'add',
          path: '/metadata/dc.description',
          value: 'testDescription'
        }, {
          op: 'replace',
          path: '/name',
          value: 'newGroupName'
        }];
        expect(groupsDataServiceStub.patch).toHaveBeenCalledWith(expected, operations);
      });

      it('should edit with description operations', () => {
        component.groupName.value = null;
        component.onSubmit();
        fixture.detectChanges();
        const operations = [{
          op: 'add',
          path: '/metadata/dc.description',
          value: 'testDescription'
        }];
        expect(groupsDataServiceStub.patch).toHaveBeenCalledWith(expected, operations);
      });

      it('should edit with name operations', () => {
        component.groupDescription.value = null;
        component.onSubmit();
        fixture.detectChanges();
        const operations = [{
          op: 'replace',
          path: '/name',
          value: 'newGroupName'
        }];
        expect(groupsDataServiceStub.patch).toHaveBeenCalledWith(expected, operations);
      });

      it('should emit the existing group using the correct new values', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect(component.submitForm.emit).toHaveBeenCalledWith(expected2);
        });
      }));
      it('should emit success notification', () => {
        expect(notificationService.success).toHaveBeenCalled();
      });
    });
  });

  describe('ngOnDestroy', () => {
    it('does NOT call router.navigate', () => {
      component.ngOnDestroy();
      expect(router.navigate).toHaveBeenCalledTimes(0);
    });
  });


  describe('check form validation', () => {
    let groupCommunity;

    beforeEach(() => {
      groupName = 'testName';
      groupCommunity = 'testgroupCommunity';
      groupDescription = 'testgroupDescription';

      expected = Object.assign(new Group(), {
        name: groupName,
        metadata: {
          'dc.description': [
            {
              value: groupDescription
            }
          ],
        },
      });
      spyOn(component.submitForm, 'emit');

      fixture.detectChanges();
      component.initialisePage();
      fixture.detectChanges();
    });
    describe('groupName, groupCommunity and groupDescription should be required', () => {
      it('form should be invalid because the groupName is required', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect(component.formGroup.controls.groupName.valid).toBeFalse();
          expect(component.formGroup.controls.groupName.errors.required).toBeTrue();
        });
      }));
    });

    describe('after inserting information groupName,groupCommunity and groupDescription not required', () => {
      beforeEach(() => {
        component.formGroup.controls.groupName.setValue('test');
        fixture.detectChanges();
      });
      it('groupName should be valid because the groupName is set', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect(component.formGroup.controls.groupName.valid).toBeTrue();
          expect(component.formGroup.controls.groupName.errors).toBeNull();
        });
      }));
    });

    describe('after already utilized groupName', () => {
      beforeEach(() => {
        const groupsDataServiceStubWithGroup = Object.assign(groupsDataServiceStub,{
          searchGroups(query: string): Observable<RemoteData<PaginatedList<Group>>> {
            return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [expected]));
          }
        });
        component.formGroup.controls.groupName.setValue('testName');
        component.formGroup.controls.groupName.setAsyncValidators(ValidateGroupExists.createValidator(groupsDataServiceStubWithGroup));
        fixture.detectChanges();
      });

      it('groupName should not be valid because groupName is already taken', waitForAsync(() => {
        fixture.whenStable().then(() => {
          expect(component.formGroup.controls.groupName.valid).toBeFalse();
          expect(component.formGroup.controls.groupName.errors.groupExists).toBeTruthy();
        });
      }));
    });
  });

  describe('delete', () => {
    let deleteButton;

    beforeEach(() => {
      component.initialisePage();

      component.canEdit$ = observableOf(true);
      component.groupBeingEdited = {
        permanent: false
      } as Group;

      fixture.detectChanges();
      deleteButton = fixture.debugElement.query(By.css('.delete-button')).nativeElement;

      spyOn(groupsDataServiceStub, 'delete').and.callThrough();
      spyOn(groupsDataServiceStub, 'getActiveGroup').and.returnValue(observableOf({ id: 'active-group' }));
    });

    describe('if confirmed via modal', () => {
      beforeEach(waitForAsync(() => {
        deleteButton.click();
        fixture.detectChanges();
        (document as any).querySelector('.modal-footer .confirm').click();
      }));

      it('should call GroupDataService.delete', () => {
        expect(groupsDataServiceStub.delete).toHaveBeenCalledWith('active-group');
      });
    });

    describe('if canceled via modal', () => {
      beforeEach(waitForAsync(() => {
        deleteButton.click();
        fixture.detectChanges();
        (document as any).querySelector('.modal-footer .cancel').click();
      }));

      it('should not call GroupDataService.delete', () => {
        expect(groupsDataServiceStub.delete).not.toHaveBeenCalled();
      });
    });
  });
});
