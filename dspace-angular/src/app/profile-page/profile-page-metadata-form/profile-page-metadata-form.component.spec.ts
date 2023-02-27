import { ProfilePageMetadataFormComponent } from './profile-page-metadata-form.component';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { VarDirective } from '../../shared/utils/var.directive';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { EventEmitter, NO_ERRORS_SCHEMA } from '@angular/core';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { FormBuilderService } from '../../shared/form/builder/form-builder.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import cloneDeep from 'lodash/cloneDeep';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';

describe('ProfilePageMetadataFormComponent', () => {
  let component: ProfilePageMetadataFormComponent;
  let fixture: ComponentFixture<ProfilePageMetadataFormComponent>;

  let user;

  let epersonService;
  let notificationsService;
  let translate;

  function init() {
    user = Object.assign(new EPerson(), {
      email: 'example@gmail.com',
      metadata: {
        'eperson.firstname': [
          {
            value: 'John',
            language: null
          }
        ],
        'eperson.lastname': [
          {
            value: 'Doe',
            language: null
          }
        ],
        'eperson.language': [
          {
            value: 'de',
            language: null
          }
        ]
      }
    });

    epersonService = jasmine.createSpyObj('epersonService', {
      update: createSuccessfulRemoteDataObject$(user)
    });
    notificationsService = jasmine.createSpyObj('notificationsService', {
      success: {},
      error: {},
      warning: {}
    });
    translate = {
      instant: () => 'translated',
      onLangChange: new EventEmitter()
    };

  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      declarations: [ProfilePageMetadataFormComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
        { provide: EPersonDataService, useValue: epersonService },
        { provide: TranslateService, useValue: translate },
        { provide: NotificationsService, useValue: notificationsService },
        FormBuilderService
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProfilePageMetadataFormComponent);
    component = fixture.componentInstance;
    component.user = user;
    fixture.detectChanges();
  });

  it('should automatically fill in the user\'s email in the correct field', () => {
    expect(component.formGroup.get('email').value).toEqual(user.email);
  });

  it('should automatically fill the present metadata values and leave missing ones empty', () => {
    expect(component.formGroup.get('firstname').value).toEqual('John');
    expect(component.formGroup.get('lastname').value).toEqual('Doe');
    expect(component.formGroup.get('phone').value).toBeUndefined();
    expect(component.formGroup.get('language').value).toEqual('de');
  });

  describe('updateProfile', () => {
    describe('when no values changed', () => {
      let result;

      beforeEach(() => {
        result = component.updateProfile();
      });

      it('should return false', () => {
        expect(result).toEqual(false);
      });

      it('should not call epersonService.update', () => {
        expect(epersonService.update).not.toHaveBeenCalled();
      });
    });

    describe('when a form value changed', () => {
      let result;
      let newUser;

      beforeEach(() => {
        newUser = cloneDeep(user);
        newUser.metadata['eperson.firstname'][0].value = 'Johnny';
        setModelValue('firstname', 'Johnny');
        result = component.updateProfile();
      });

      it('should return true', () => {
        expect(result).toEqual(true);
      });

      it('should call epersonService.update', () => {
        expect(epersonService.update).toHaveBeenCalledWith(newUser);
      });
    });
  });

  function setModelValue(id: string, value: string) {
    component.formModel.filter((model) => model.id === id).forEach((model) => (model as any).value = value);
  }
});
