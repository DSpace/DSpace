import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { of as observableOf } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';

import { NotificationsService } from '../../shared/notifications/notifications.service';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { ResearcherProfile } from '../../core/profile/model/researcher-profile.model';
import { ResearcherProfileDataService } from '../../core/profile/researcher-profile-data.service';
import { VarDirective } from '../../shared/utils/var.directive';
import { ProfilePageResearcherFormComponent } from './profile-page-researcher-form.component';
import { ProfileClaimService } from '../profile-claim/profile-claim.service';
import { AuthService } from '../../core/auth/auth.service';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { followLink } from '../../shared/utils/follow-link-config.model';

describe('ProfilePageResearcherFormComponent', () => {

  let component: ProfilePageResearcherFormComponent;
  let fixture: ComponentFixture<ProfilePageResearcherFormComponent>;
  let router: Router;

  let user: EPerson;
  let profile: ResearcherProfile;

  let researcherProfileService: jasmine.SpyObj<ResearcherProfileDataService>;

  let notificationsServiceStub: NotificationsServiceStub;

  let profileClaimService: jasmine.SpyObj<ProfileClaimService>;

  let authService: jasmine.SpyObj<AuthService>;

  function init() {

    user = Object.assign(new EPerson(), {
      id: 'beef9946-f4ce-479e-8f11-b90cbe9f7241'
    });

    profile = Object.assign(new ResearcherProfile(), {
      id: 'beef9946-f4ce-479e-8f11-b90cbe9f7241',
      visible: false,
      type: 'profile'
    });

    authService = jasmine.createSpyObj('authService', {
      getAuthenticatedUserFromStore: observableOf(user)
    });

    researcherProfileService = jasmine.createSpyObj('researcherProfileService', {
      findById: createSuccessfulRemoteDataObject$(profile),
      create: observableOf(profile),
      setVisibility: jasmine.createSpy('setVisibility'),
      delete: observableOf(true),
      findRelatedItemId: observableOf('a42557ca-cbb8-4442-af9c-3bb5cad2d075')
    });

    notificationsServiceStub = new NotificationsServiceStub();

    profileClaimService = jasmine.createSpyObj('profileClaimService', {
      hasProfilesToSuggest: observableOf(false),
    });

  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      declarations: [ProfilePageResearcherFormComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
        NgbModal,
        { provide: ResearcherProfileDataService, useValue: researcherProfileService },
        { provide: NotificationsService, useValue: notificationsServiceStub },
        { provide: ProfileClaimService, useValue: profileClaimService },
        { provide: AuthService, useValue: authService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProfilePageResearcherFormComponent);
    component = fixture.componentInstance;
    component.user = user;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should search the researcher profile for the current user', () => {
    expect(researcherProfileService.findById).toHaveBeenCalledWith(user.id, false, true, followLink('item'));
  });

  describe('createProfile', () => {

    it('should create the profile', () => {
      component.createProfile();
      expect(researcherProfileService.create).toHaveBeenCalledWith();
    });

  });

  describe('toggleProfileVisibility', () => {

    describe('', () => {

      beforeEach(() => {
        researcherProfileService.setVisibility.and.returnValue(createSuccessfulRemoteDataObject$(profile));
      });

      it('should set the profile visibility to true', () => {
        profile.visible = false;
        component.toggleProfileVisibility(profile);
        expect(researcherProfileService.setVisibility).toHaveBeenCalledWith(profile, true);
      });

      it('should set the profile visibility to false', () => {
        profile.visible = true;
        component.toggleProfileVisibility(profile);
        expect(researcherProfileService.setVisibility).toHaveBeenCalledWith(profile, false);
      });
    });

    describe('on successful', () => {
      beforeEach(() => {
        researcherProfileService.setVisibility.and.returnValue(createSuccessfulRemoteDataObject$(profile));
      });

      it('should update the profile properly', () => {
        profile.visible = true;
        component.toggleProfileVisibility(profile);
        expect(component.researcherProfile$.value).toEqual(profile);
      });

    });

    describe('on error', () => {
      beforeEach(() => {
        researcherProfileService.setVisibility.and.returnValue(createFailedRemoteDataObject$());
      });

      it('should update the profile properly', () => {
        const unchangedProfile = profile;
        profile.visible = true;
        component.toggleProfileVisibility(profile);
        expect(component.researcherProfile$.value).toEqual(unchangedProfile);
        expect((component as any).notificationService.error).toHaveBeenCalled();
      });

    });
  });

  describe('deleteProfile', () => {
    beforeEach(() => {
      const modalService = (component as any).modalService;
      spyOn(modalService, 'open').and.returnValue(Object.assign({ componentInstance: Object.assign({ response: observableOf(true) }) }));
      component.deleteProfile(profile);
      fixture.detectChanges();
    });

    it('should delete the profile', () => {

      expect(researcherProfileService.delete).toHaveBeenCalledWith(profile.id);
    });

  });

  describe('viewProfile', () => {

    it('should open the item details page', () => {
      spyOn(router, 'navigate');
      component.viewProfile(profile);
      expect(router.navigate).toHaveBeenCalledWith(['items', 'a42557ca-cbb8-4442-af9c-3bb5cad2d075']);
    });

  });

});
