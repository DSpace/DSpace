import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { EndUserAgreementComponent } from './end-user-agreement.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { EndUserAgreementService } from '../../core/end-user-agreement/end-user-agreement.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of as observableOf } from 'rxjs';
import { Store } from '@ngrx/store';
import { By } from '@angular/platform-browser';
import { LogOutAction } from '../../core/auth/auth.actions';
import { ActivatedRouteStub } from '../../shared/testing/active-router.stub';

describe('EndUserAgreementComponent', () => {
  let component: EndUserAgreementComponent;
  let fixture: ComponentFixture<EndUserAgreementComponent>;

  let endUserAgreementService: EndUserAgreementService;
  let notificationsService: NotificationsService;
  let authService: AuthService;
  let store;
  let router: Router;
  let route: ActivatedRoute;

  let redirectUrl;

  function init() {
    redirectUrl = 'redirect/url';

    endUserAgreementService = jasmine.createSpyObj('endUserAgreementService', {
      hasCurrentUserOrCookieAcceptedAgreement: observableOf(false),
      setUserAcceptedAgreement: observableOf(true)
    });
    notificationsService = jasmine.createSpyObj('notificationsService', ['success', 'error']);
    authService = jasmine.createSpyObj('authService', {
      isAuthenticated: observableOf(true)
    });
    store = jasmine.createSpyObj('store', ['dispatch']);
    router = jasmine.createSpyObj('router', ['navigate', 'navigateByUrl']);
    route = Object.assign(new ActivatedRouteStub(), {
      queryParams: observableOf({
        redirect: redirectUrl
      })
    }) as any;
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [EndUserAgreementComponent],
      providers: [
        { provide: EndUserAgreementService, useValue: endUserAgreementService },
        { provide: NotificationsService, useValue: notificationsService },
        { provide: AuthService, useValue: authService },
        { provide: Store, useValue: store },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: route }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EndUserAgreementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('when the user hasn\'t accepted the agreement', () => {
    beforeEach(() => {
      (endUserAgreementService.hasCurrentUserOrCookieAcceptedAgreement as jasmine.Spy).and.returnValue(observableOf(false));
      component.ngOnInit();
      fixture.detectChanges();
    });

    it('should initialize the accepted property', () => {
      expect(component.accepted).toEqual(false);
    });

    it('should disable the save button', () => {
      const button = fixture.debugElement.query(By.css('#button-save')).nativeElement;
      expect(button.disabled).toBeTruthy();
    });
  });

  describe('when the user has accepted the agreement', () => {
    beforeEach(() => {
      (endUserAgreementService.hasCurrentUserOrCookieAcceptedAgreement as jasmine.Spy).and.returnValue(observableOf(true));
      component.ngOnInit();
      fixture.detectChanges();
    });

    it('should initialize the accepted property', () => {
      expect(component.accepted).toEqual(true);
    });

    it('should enable the save button', () => {
      const button = fixture.debugElement.query(By.css('#button-save')).nativeElement;
      expect(button.disabled).toBeFalsy();
    });

    describe('submit', () => {
      describe('when accepting the agreement was successful', () => {
        beforeEach(() => {
          (endUserAgreementService.setUserAcceptedAgreement as jasmine.Spy).and.returnValue(observableOf(true));
          component.submit();
        });

        it('should display a success notification', () => {
          expect(notificationsService.success).toHaveBeenCalled();
        });

        it('should navigate the user to the redirect url', () => {
          expect(router.navigateByUrl).toHaveBeenCalledWith(redirectUrl);
        });
      });

      describe('when accepting the agreement was unsuccessful', () => {
        beforeEach(() => {
          (endUserAgreementService.setUserAcceptedAgreement as jasmine.Spy).and.returnValue(observableOf(false));
          component.submit();
        });

        it('should display an error notification', () => {
          expect(notificationsService.error).toHaveBeenCalled();
        });
      });
    });
  });

  describe('cancel', () => {
    describe('when the user is authenticated', () => {
      beforeEach(() => {
        (authService.isAuthenticated as jasmine.Spy).and.returnValue(observableOf(true));
        component.cancel();
      });

      it('should logout the user', () => {
        expect(store.dispatch).toHaveBeenCalledWith(new LogOutAction());
      });
    });

    describe('when the user is not authenticated', () => {
      beforeEach(() => {
        (authService.isAuthenticated as jasmine.Spy).and.returnValue(observableOf(false));
        component.cancel();
      });

      it('should navigate the user to the homepage', () => {
        expect(router.navigate).toHaveBeenCalledWith(['home']);
      });
    });
  });
});
