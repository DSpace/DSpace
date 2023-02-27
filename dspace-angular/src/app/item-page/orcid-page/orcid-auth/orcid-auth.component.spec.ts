import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, waitForAsync } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { By } from '@angular/platform-browser';

import { getTestScheduler } from 'jasmine-marbles';
import { NgbAccordionModule } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { OrcidAuthService } from '../../../core/orcid/orcid-auth.service';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { Item } from '../../../core/shared/item.model';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { TranslateLoaderMock } from '../../../shared/mocks/translate-loader.mock';
import { OrcidAuthComponent } from './orcid-auth.component';
import { NativeWindowService } from '../../../core/services/window.service';
import { NativeWindowMockFactory } from '../../../shared/mocks/mock-native-window-ref';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { ResearcherProfile } from '../../../core/profile/model/researcher-profile.model';

describe('OrcidAuthComponent test suite', () => {
  let comp: OrcidAuthComponent;
  let fixture: ComponentFixture<OrcidAuthComponent>;
  let scheduler: TestScheduler;
  let orcidAuthService: jasmine.SpyObj<OrcidAuthService>;
  let nativeWindowRef;
  let notificationsService;

  const orcidScopes = [
    '/authenticate',
    '/read-limited',
    '/activities/update',
    '/person/update'
  ];

  const partialOrcidScopes = [
    '/authenticate',
    '/read-limited',
  ];

  const mockItemUnlinkedToOrcid: Item = Object.assign(new Item(), {
    bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    metadata: {
      'dc.title': [{
        value: 'test person'
      }],
      'dspace.entity.type': [{
        'value': 'Person'
      }]
    }
  });

  const mockItemLinkedToOrcid: Item = Object.assign(new Item(), {
    bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    metadata: {
      'dc.title': [{
        value: 'test person'
      }],
      'dspace.entity.type': [{
        'value': 'Person'
      }],
      'dspace.object.owner': [{
        'value': 'test person',
        'language': null,
        'authority': 'deced3e7-68e2-495d-bf98-7c44fc33b8ff',
        'confidence': 600,
        'place': 0
      }],
      'dspace.orcid.authenticated': [{
        'value': '2022-06-10T15:15:12.952872',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }],
      'dspace.orcid.scope': [{
        'value': '/authenticate',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }, {
        'value': '/read-limited',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 1
      }, {
        'value': '/activities/update',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 2
      }, {
        'value': '/person/update',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 3
      }],
      'person.identifier.orcid': [{
        'value': 'orcid-id',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }]
    }
  });

  beforeEach(waitForAsync(() => {
    orcidAuthService = jasmine.createSpyObj('researcherProfileService', {
      getOrcidAuthorizationScopes: jasmine.createSpy('getOrcidAuthorizationScopes'),
      getOrcidAuthorizationScopesByItem: jasmine.createSpy('getOrcidAuthorizationScopesByItem'),
      getOrcidAuthorizeUrl: jasmine.createSpy('getOrcidAuthorizeUrl'),
      isLinkedToOrcid: jasmine.createSpy('isLinkedToOrcid'),
      onlyAdminCanDisconnectProfileFromOrcid: jasmine.createSpy('onlyAdminCanDisconnectProfileFromOrcid'),
      ownerCanDisconnectProfileFromOrcid: jasmine.createSpy('ownerCanDisconnectProfileFromOrcid'),
      unlinkOrcidByItem: jasmine.createSpy('unlinkOrcidByItem')
    });

    void TestBed.configureTestingModule({
      imports: [
        NgbAccordionModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
        RouterTestingModule.withRoutes([])
      ],
      declarations: [OrcidAuthComponent],
      providers: [
        { provide: NativeWindowService, useFactory: NativeWindowMockFactory },
        { provide: NotificationsService, useClass: NotificationsServiceStub },
        { provide: OrcidAuthService, useValue: orcidAuthService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(OrcidAuthComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    scheduler = getTestScheduler();
    fixture = TestBed.createComponent(OrcidAuthComponent);
    comp = fixture.componentInstance;
    orcidAuthService.getOrcidAuthorizationScopes.and.returnValue(of(orcidScopes));
  }));

  describe('when orcid profile is not linked', () => {
    beforeEach(waitForAsync(() => {
      comp.item = mockItemUnlinkedToOrcid;
      orcidAuthService.getOrcidAuthorizationScopesByItem.and.returnValue([]);
      orcidAuthService.isLinkedToOrcid.and.returnValue(false);
      orcidAuthService.onlyAdminCanDisconnectProfileFromOrcid.and.returnValue(of(false));
      orcidAuthService.ownerCanDisconnectProfileFromOrcid.and.returnValue(of(true));
      orcidAuthService.getOrcidAuthorizeUrl.and.returnValue(of('oarcidUrl'));
      fixture.detectChanges();
    }));

    it('should create', fakeAsync(() => {
      const orcidLinked = fixture.debugElement.query(By.css('[data-test="orcidLinked"]'));
      const orcidNotLinked = fixture.debugElement.query(By.css('[data-test="orcidNotLinked"]'));
      expect(orcidLinked).toBeFalsy();
      expect(orcidNotLinked).toBeTruthy();
    }));

    it('should change location on link', () => {
      nativeWindowRef = (comp as any)._window;
      scheduler.schedule(() => comp.linkOrcid());
      scheduler.flush();

      expect(nativeWindowRef.nativeWindow.location.href).toBe('oarcidUrl');
    });

  });

  describe('when orcid profile is linked', () => {
    beforeEach(waitForAsync(() => {
      comp.item = mockItemLinkedToOrcid;
      orcidAuthService.isLinkedToOrcid.and.returnValue(true);
    }));

    describe('', () => {

      beforeEach(waitForAsync(() => {
        comp.item = mockItemLinkedToOrcid;
        notificationsService = (comp as any).notificationsService;
        orcidAuthService.getOrcidAuthorizationScopesByItem.and.returnValue([...orcidScopes]);
        orcidAuthService.isLinkedToOrcid.and.returnValue(true);
        orcidAuthService.onlyAdminCanDisconnectProfileFromOrcid.and.returnValue(of(false));
        orcidAuthService.ownerCanDisconnectProfileFromOrcid.and.returnValue(of(true));
      }));

      describe('and unlink is successfully', () => {
        beforeEach(waitForAsync(() => {
          comp.item = mockItemLinkedToOrcid;
          orcidAuthService.unlinkOrcidByItem.and.returnValue(createSuccessfulRemoteDataObject$(new ResearcherProfile()));
          spyOn(comp.unlink, 'emit');
          fixture.detectChanges();
        }));

        it('should show success notification', () => {
          scheduler.schedule(() => comp.unlinkOrcid());
          scheduler.flush();

          expect(notificationsService.success).toHaveBeenCalled();
          expect(comp.unlink.emit).toHaveBeenCalled();
        });
      });

      describe('and unlink is failed', () => {
        beforeEach(waitForAsync(() => {
          comp.item = mockItemLinkedToOrcid;
          orcidAuthService.unlinkOrcidByItem.and.returnValue(createFailedRemoteDataObject$());
          fixture.detectChanges();
        }));

        it('should show success notification', () => {
          scheduler.schedule(() => comp.unlinkOrcid());
          scheduler.flush();

          expect(notificationsService.error).toHaveBeenCalled();
        });
      });
    });

    describe('and has orcid authorization scopes', () => {

      beforeEach(waitForAsync(() => {
        comp.item = mockItemLinkedToOrcid;
        orcidAuthService.getOrcidAuthorizationScopesByItem.and.returnValue([...orcidScopes]);
        orcidAuthService.isLinkedToOrcid.and.returnValue(true);
        orcidAuthService.onlyAdminCanDisconnectProfileFromOrcid.and.returnValue(of(false));
        orcidAuthService.ownerCanDisconnectProfileFromOrcid.and.returnValue(of(true));
        fixture.detectChanges();
      }));

      it('should create', fakeAsync(() => {
        const orcidLinked = fixture.debugElement.query(By.css('[data-test="orcidLinked"]'));
        const orcidNotLinked = fixture.debugElement.query(By.css('[data-test="orcidNotLinked"]'));
        expect(orcidLinked).toBeTruthy();
        expect(orcidNotLinked).toBeFalsy();
      }));

      it('should display orcid authorizations', fakeAsync(() => {
        const orcidAuthorizations = fixture.debugElement.query(By.css('[data-test="hasOrcidAuthorizations"]'));
        const noMissingOrcidAuthorizations = fixture.debugElement.query(By.css('[data-test="noMissingOrcidAuthorizations"]'));
        const orcidAuthorizationsList = fixture.debugElement.queryAll(By.css('[data-test="orcidAuthorization"]'));

        expect(orcidAuthorizations).toBeTruthy();
        expect(noMissingOrcidAuthorizations).toBeTruthy();
        expect(orcidAuthorizationsList.length).toBe(4);
      }));
    });

    describe('and has missing orcid authorization scopes', () => {

      beforeEach(waitForAsync(() => {
        comp.item = mockItemLinkedToOrcid;
        orcidAuthService.getOrcidAuthorizationScopesByItem.and.returnValue([...partialOrcidScopes]);
        orcidAuthService.isLinkedToOrcid.and.returnValue(true);
        orcidAuthService.onlyAdminCanDisconnectProfileFromOrcid.and.returnValue(of(false));
        orcidAuthService.ownerCanDisconnectProfileFromOrcid.and.returnValue(of(true));
        fixture.detectChanges();
      }));

      it('should create', fakeAsync(() => {
        const orcidLinked = fixture.debugElement.query(By.css('[data-test="orcidLinked"]'));
        const orcidNotLinked = fixture.debugElement.query(By.css('[data-test="orcidNotLinked"]'));
        expect(orcidLinked).toBeTruthy();
        expect(orcidNotLinked).toBeFalsy();
      }));

      it('should display orcid authorizations', fakeAsync(() => {
        const orcidAuthorizations = fixture.debugElement.query(By.css('[data-test="hasOrcidAuthorizations"]'));
        const missingOrcidAuthorizations = fixture.debugElement.query(By.css('[data-test="missingOrcidAuthorizations"]'));
        const orcidAuthorizationsList = fixture.debugElement.queryAll(By.css('[data-test="orcidAuthorization"]'));
        const missingOrcidAuthorizationsList = fixture.debugElement.queryAll(By.css('[data-test="missingOrcidAuthorization"]'));

        expect(orcidAuthorizations).toBeTruthy();
        expect(missingOrcidAuthorizations).toBeTruthy();
        expect(orcidAuthorizationsList.length).toBe(2);
        expect(missingOrcidAuthorizationsList.length).toBe(2);
      }));
    });

    describe('and only admin can unlink scopes', () => {

      beforeEach(waitForAsync(() => {
        comp.item = mockItemLinkedToOrcid;
        orcidAuthService.getOrcidAuthorizationScopesByItem.and.returnValue([...orcidScopes]);
        orcidAuthService.isLinkedToOrcid.and.returnValue(true);
        orcidAuthService.onlyAdminCanDisconnectProfileFromOrcid.and.returnValue(of(true));
        orcidAuthService.ownerCanDisconnectProfileFromOrcid.and.returnValue(of(false));
        fixture.detectChanges();
      }));

      it('should display warning panel', fakeAsync(() => {
        const unlinkOnlyAdmin = fixture.debugElement.query(By.css('[data-test="unlinkOnlyAdmin"]'));
        const unlinkOwner = fixture.debugElement.query(By.css('[data-test="unlinkOwner"]'));
        expect(unlinkOnlyAdmin).toBeTruthy();
        expect(unlinkOwner).toBeFalsy();
      }));

    });

    describe('and owner can unlink scopes', () => {

      beforeEach(waitForAsync(() => {
        comp.item = mockItemLinkedToOrcid;
        orcidAuthService.getOrcidAuthorizationScopesByItem.and.returnValue([...orcidScopes]);
        orcidAuthService.isLinkedToOrcid.and.returnValue(true);
        orcidAuthService.onlyAdminCanDisconnectProfileFromOrcid.and.returnValue(of(true));
        orcidAuthService.ownerCanDisconnectProfileFromOrcid.and.returnValue(of(true));
        fixture.detectChanges();
      }));

      it('should display warning panel', fakeAsync(() => {
        const unlinkOnlyAdmin = fixture.debugElement.query(By.css('[data-test="unlinkOnlyAdmin"]'));
        const unlinkOwner = fixture.debugElement.query(By.css('[data-test="unlinkOwner"]'));
        expect(unlinkOnlyAdmin).toBeFalsy();
        expect(unlinkOwner).toBeTruthy();
      }));

    });

  });


});
