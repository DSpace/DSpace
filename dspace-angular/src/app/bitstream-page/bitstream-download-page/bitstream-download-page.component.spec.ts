import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { FileService } from '../../core/shared/file.service';
import { of as observableOf } from 'rxjs';
import { Bitstream } from '../../core/shared/bitstream.model';
import { BitstreamDownloadPageComponent } from './bitstream-download-page.component';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { HardRedirectService } from '../../core/services/hard-redirect.service';
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { ActivatedRoute, Router } from '@angular/router';
import { getForbiddenRoute } from '../../app-routing-paths';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';

describe('BitstreamDownloadPageComponent', () => {
  let component: BitstreamDownloadPageComponent;
  let fixture: ComponentFixture<BitstreamDownloadPageComponent>;

  let authService: AuthService;
  let fileService: FileService;
  let authorizationService: AuthorizationDataService;
  let hardRedirectService: HardRedirectService;
  let activatedRoute;
  let router;

  let bitstream: Bitstream;

  function init() {
    authService = jasmine.createSpyObj('authService', {
      isAuthenticated: observableOf(true),
      setRedirectUrl: {}
    });
    authorizationService = jasmine.createSpyObj('authorizationSerivice', {
      isAuthorized: observableOf(true)
    });

    fileService = jasmine.createSpyObj('fileService', {
      retrieveFileDownloadLink: observableOf('content-url-with-headers')
    });

    hardRedirectService = jasmine.createSpyObj('fileService', {
      redirect: {}
    });
    bitstream = Object.assign(new Bitstream(), {
      uuid: 'bitstreamUuid',
      _links: {
        content: {href: 'bitstream-content-link'},
        self: {href: 'bitstream-self-link'},
      }
    });

    activatedRoute = {
      data: observableOf({
        bitstream: createSuccessfulRemoteDataObject(
          bitstream
        )
      })
    };

    router = jasmine.createSpyObj('router', ['navigateByUrl']);
  }

  function initTestbed() {
    TestBed.configureTestingModule({
      imports: [CommonModule, TranslateModule.forRoot()],
      declarations: [BitstreamDownloadPageComponent],
      providers: [
        {provide: ActivatedRoute, useValue: activatedRoute},
        {provide: Router, useValue: router},
        {provide: AuthorizationDataService, useValue: authorizationService},
        {provide: AuthService, useValue: authService},
        {provide: FileService, useValue: fileService},
        {provide: HardRedirectService, useValue: hardRedirectService},
      ]
    })
      .compileComponents();
  }

  describe('init', () => {
    beforeEach(waitForAsync(() => {
      init();
      initTestbed();
    }));
    beforeEach(() => {
      fixture = TestBed.createComponent(BitstreamDownloadPageComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });
    it('should init the comp', () => {
      expect(component).toBeTruthy();
    });
  });

  describe('bitstream retrieval', () => {
    describe('when the user is authorized and not logged in', () => {
      beforeEach(waitForAsync(() => {
        init();
        (authService.isAuthenticated as jasmine.Spy).and.returnValue(observableOf(false));

        initTestbed();
      }));
      beforeEach(() => {
        fixture = TestBed.createComponent(BitstreamDownloadPageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
      });
      it('should redirect to the content link', () => {
        expect(hardRedirectService.redirect).toHaveBeenCalledWith('bitstream-content-link');
      });
    });
    describe('when the user is authorized and logged in', () => {
      beforeEach(waitForAsync(() => {
        init();
        initTestbed();
      }));
      beforeEach(() => {
        fixture = TestBed.createComponent(BitstreamDownloadPageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
      });
      it('should redirect to an updated content link', () => {
        expect(hardRedirectService.redirect).toHaveBeenCalledWith('content-url-with-headers');
      });
    });
    describe('when the user is not authorized and logged in', () => {
      beforeEach(waitForAsync(() => {
        init();
        (authorizationService.isAuthorized as jasmine.Spy).and.returnValue(observableOf(false));
        initTestbed();
      }));
      beforeEach(() => {
        fixture = TestBed.createComponent(BitstreamDownloadPageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
      });
      it('should navigate to the forbidden route', () => {
        expect(router.navigateByUrl).toHaveBeenCalledWith(getForbiddenRoute(), {skipLocationChange: true});
      });
    });
    describe('when the user is not authorized and not logged in', () => {
      beforeEach(waitForAsync(() => {
        init();
        (authService.isAuthenticated as jasmine.Spy).and.returnValue(observableOf(false));
        (authorizationService.isAuthorized as jasmine.Spy).and.returnValue(observableOf(false));
        initTestbed();
      }));
      beforeEach(() => {
        fixture = TestBed.createComponent(BitstreamDownloadPageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
      });
      it('should navigate to the login page', () => {
        expect(authService.setRedirectUrl).toHaveBeenCalled();
        expect(router.navigateByUrl).toHaveBeenCalledWith('login');
      });
    });
  });
});
