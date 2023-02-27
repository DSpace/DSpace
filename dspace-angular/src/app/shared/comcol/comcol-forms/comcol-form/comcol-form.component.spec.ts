import { Location } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { DynamicFormControlModel, DynamicFormService, DynamicInputModel } from '@ng-dynamic-forms/core';
import { TranslateModule } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { ObjectCacheService } from '../../../../core/cache/object-cache.service';
import { RequestService } from '../../../../core/data/request.service';
import { RestRequestMethod } from '../../../../core/data/rest-request-method';
import { Community } from '../../../../core/shared/community.model';
import { hasValue } from '../../../empty.util';
import { AuthServiceMock } from '../../../mocks/auth.service.mock';
import { NotificationsService } from '../../../notifications/notifications.service';
import { NotificationsServiceStub } from '../../../testing/notifications-service.stub';
import { VarDirective } from '../../../utils/var.directive';
import { ComColFormComponent } from './comcol-form.component';
import { Operation } from 'fast-json-patch';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../../remote-data.utils';

describe('ComColFormComponent', () => {
  let comp: ComColFormComponent<any>;
  let fixture: ComponentFixture<ComColFormComponent<any>>;
  let location: Location;
  const formServiceStub: any = {
    createFormGroup: (fModel: DynamicFormControlModel[]) => {
      const controls = {};
      if (hasValue(fModel)) {
        fModel.forEach((controlModel) => {
          controls[controlModel.id] = new FormControl((controlModel as any).value);
        });
        return new FormGroup(controls);
      }
      return undefined;
    }
  };
  const dcTitle = 'dc.title';
  const dcAbstract = 'dc.description.abstract';

  const abstractMD = { [dcAbstract]: [{ value: 'Community description', language: null }] };
  const newTitleMD = { [dcTitle]: [{ value: 'New Community Title', language: null }] };
  const formModel = [
    new DynamicInputModel({
      id: 'title',
      name: dcTitle,
      value: newTitleMD[dcTitle][0].value
    }),
    new DynamicInputModel({
      id: 'abstract',
      name: dcAbstract,
      value: abstractMD[dcAbstract][0].value
    })
  ];

  const logo = {
    id: 'logo'
  };
  const logoEndpoint = 'rest/api/logo/endpoint';
  const dsoService = Object.assign({
    getLogoEndpoint: () => observableOf(logoEndpoint),
    deleteLogo: () => createSuccessfulRemoteDataObject$({})
  });
  const notificationsService = new NotificationsServiceStub();

  /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
  const locationStub = jasmine.createSpyObj('location', ['back']);
  /* eslint-enable no-empty, @typescript-eslint/no-empty-function */

  const requestServiceStub = jasmine.createSpyObj('requestService', {
    removeByHrefSubstring: {}
  });
  const objectCacheStub = jasmine.createSpyObj('objectCache', {
    remove: {}
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), RouterTestingModule],
      declarations: [ComColFormComponent, VarDirective],
      providers: [
        { provide: Location, useValue: locationStub },
        { provide: DynamicFormService, useValue: formServiceStub },
        { provide: NotificationsService, useValue: notificationsService },
        { provide: AuthService, useValue: new AuthServiceMock() },
        { provide: RequestService, useValue: requestServiceStub },
        { provide: ObjectCacheService, useValue: objectCacheStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  describe('when the dso doesn\'t contain an ID (newly created)', () => {
    beforeEach(() => {
      initComponent(Object.assign(new Community(), {
        _links: { self: { href: 'community-self' } }
      }));
    });

    it('should initialize the uploadFilesOptions with a placeholder url', () => {
      expect(comp.uploadFilesOptions.url.length).toBeGreaterThan(0);
    });

    describe('onSubmit', () => {
      beforeEach(() => {
        spyOn(comp.submitForm, 'emit');
        comp.formModel = formModel;
      });

      it('should emit the new version of the community', () => {
        comp.dso = new Community();
        comp.onSubmit();

        const operations: Operation[] = [
          {
            op: 'replace',
            path: '/metadata/dc.title',
            value: {
              value: 'New Community Title',
              language: null,
            },
          },
          {
            op: 'replace',
            path: '/metadata/dc.description.abstract',
            value: {
              value: 'Community description',
              language: null,
            },
          },
        ];

        expect(comp.submitForm.emit).toHaveBeenCalledWith(
          {
            dso: Object.assign({}, comp.dso, {
                metadata: {
                  'dc.title': [{
                    value: 'New Community Title',
                    language: null,
                  }],
                  'dc.description.abstract': [{
                    value: 'Community description',
                    language: null,
                  }],
                },
                type: Community.type,
              }
            ),
            uploader: undefined,
            deleteLogo: false,
            operations: operations,
          }
        );
      });
    });

    describe('onCompleteItem', () => {
      beforeEach(() => {
        spyOn(comp.finish, 'emit');
        comp.onCompleteItem();
      });

      it('should show a success notification', () => {
        expect(notificationsService.success).toHaveBeenCalled();
      });

      it('should emit finish', () => {
        expect(comp.finish.emit).toHaveBeenCalled();
      });
    });

    describe('onUploadError', () => {
      beforeEach(() => {
        spyOn(comp.finish, 'emit');
        comp.onUploadError();
      });

      it('should show an error notification', () => {
        expect(notificationsService.error).toHaveBeenCalled();
      });

      it('should emit finish', () => {
        expect(comp.finish.emit).toHaveBeenCalled();
      });
    });
  });

  describe('when the dso contains an ID (being edited)', () => {
    describe('and the dso doesn\'t contain a logo', () => {
      beforeEach(() => {
        initComponent(Object.assign(new Community(), {
          id: 'community-id',
          logo: createSuccessfulRemoteDataObject$(undefined),
          _links: { self: { href: 'community-self' } }
        }));
      });

      it('should initialize the uploadFilesOptions with the logo\'s endpoint url', () => {
        expect(comp.uploadFilesOptions.url).toEqual(logoEndpoint);
      });

      it('should initialize the uploadFilesOptions with a POST method', () => {
        expect(comp.uploadFilesOptions.method).toEqual(RestRequestMethod.POST);
      });
    });

    describe('and the dso contains a logo', () => {
      beforeEach(() => {
        initComponent(Object.assign(new Community(), {
          id: 'community-id',
          logo: createSuccessfulRemoteDataObject$(logo),
          _links: {
            self: { href: 'community-self' },
            logo: { href: 'community-logo' },
          }
        }));
      });

      it('should initialize the uploadFilesOptions with the logo\'s endpoint url', () => {
        expect(comp.uploadFilesOptions.url).toEqual(logoEndpoint);
      });

      it('should initialize the uploadFilesOptions with a PUT method', () => {
        expect(comp.uploadFilesOptions.method).toEqual(RestRequestMethod.PUT);
      });

      describe('submit with logo marked for deletion', () => {
        beforeEach(() => {
          spyOn(dsoService, 'deleteLogo').and.callThrough();
          comp.markLogoForDeletion = true;
        });

        it('should call dsoService.deleteLogo on the DSO', () => {
          comp.onSubmit();
          fixture.detectChanges();

          expect(dsoService.deleteLogo).toHaveBeenCalledWith(comp.dso);
        });

        describe('when dsoService.deleteLogo returns a successful response', () => {
          beforeEach(() => {
            dsoService.deleteLogo.and.returnValue(createSuccessfulRemoteDataObject$({}));
            comp.onSubmit();
          });

          it('should display a success notification', () => {
            expect(notificationsService.success).toHaveBeenCalled();
          });
        });

        describe('when dsoService.deleteLogo returns an error response', () => {
          beforeEach(() => {
            dsoService.deleteLogo.and.returnValue(createFailedRemoteDataObject$('Error', 500));
            comp.onSubmit();
          });

          it('should display an error notification', () => {
            expect(notificationsService.error).toHaveBeenCalled();
          });
        });
      });

      describe('deleteLogo', () => {
        beforeEach(() => {
          comp.deleteLogo();
          fixture.detectChanges();
        });

        it('should set markLogoForDeletion to true', () => {
          expect(comp.markLogoForDeletion).toEqual(true);
        });

        it('should mark the logo section with a danger alert', () => {
          const logoSection = fixture.debugElement.query(By.css('#logo-section.alert-danger'));
          expect(logoSection).toBeTruthy();
        });

        it('should hide the delete button', () => {
          const button = fixture.debugElement.query(By.css('#logo-section .btn-danger'));
          expect(button).not.toBeTruthy();
        });

        it('should show the undo button', () => {
          const button = fixture.debugElement.query(By.css('#logo-section .btn-warning'));
          expect(button).toBeTruthy();
        });
      });

      describe('undoDeleteLogo', () => {
        beforeEach(() => {
          comp.markLogoForDeletion = true;
          comp.undoDeleteLogo();
          fixture.detectChanges();
        });

        it('should set markLogoForDeletion to false', () => {
          expect(comp.markLogoForDeletion).toEqual(false);
        });

        it('should disable the danger alert on the logo section', () => {
          const logoSection = fixture.debugElement.query(By.css('#logo-section.alert-danger'));
          expect(logoSection).not.toBeTruthy();
        });

        it('should show the delete button', () => {
          const button = fixture.debugElement.query(By.css('#logo-section .btn-danger'));
          expect(button).toBeTruthy();
        });

        it('should hide the undo button', () => {
          const button = fixture.debugElement.query(By.css('#logo-section .btn-warning'));
          expect(button).not.toBeTruthy();
        });
      });
    });
  });

  function initComponent(dso: Community) {
    fixture = TestBed.createComponent(ComColFormComponent);
    comp = fixture.componentInstance;
    comp.formModel = [];
    comp.dso = dso;
    (comp as any).type = Community.type;
    comp.uploaderComponent = { uploader: {} } as any;

    (comp as any).dsoService = dsoService;
    fixture.detectChanges();
    location = (comp as any).location;
  }
});
