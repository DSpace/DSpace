import { NO_ERRORS_SCHEMA, PLATFORM_ID } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ComponentFixture, fakeAsync, TestBed, waitForAsync } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { By } from '@angular/platform-browser';

import { of as observableOf } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TestScheduler } from 'rxjs/testing';
import { getTestScheduler } from 'jasmine-marbles';

import { AuthService } from '../../core/auth/auth.service';
import { ActivatedRouteStub } from '../../shared/testing/active-router.stub';
import { OrcidPageComponent } from './orcid-page.component';
import {
  createFailedRemoteDataObject$,
  createSuccessfulRemoteDataObject,
  createSuccessfulRemoteDataObject$
} from '../../shared/remote-data.utils';
import { Item } from '../../core/shared/item.model';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { TranslateLoaderMock } from '../../shared/mocks/translate-loader.mock';
import { ItemDataService } from '../../core/data/item-data.service';
import { ResearcherProfile } from '../../core/profile/model/researcher-profile.model';
import { OrcidAuthService } from '../../core/orcid/orcid-auth.service';

describe('OrcidPageComponent test suite', () => {
  let comp: OrcidPageComponent;
  let fixture: ComponentFixture<OrcidPageComponent>;
  let scheduler: TestScheduler;
  let authService: jasmine.SpyObj<AuthService>;
  let routeStub: jasmine.SpyObj<ActivatedRouteStub>;
  let routeData: any;
  let itemDataService: jasmine.SpyObj<ItemDataService>;
  let orcidAuthService: jasmine.SpyObj<OrcidAuthService>;

  const mockResearcherProfile: ResearcherProfile = Object.assign(new ResearcherProfile(), {
    id: 'test-id',
    visible: true,
    type: 'profile',
    _links: {
      item: {
        href: 'https://rest.api/rest/api/profiles/test-id/item'
      },
      self: {
        href: 'https://rest.api/rest/api/profiles/test-id'
      },
    }
  });
  const mockItem: Item = Object.assign(new Item(), {
    id: 'test-id',
    bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    metadata: {
      'dc.title': [
        {
          language: 'en_US',
          value: 'test item'
        }
      ]
    }
  });
  const mockItemLinkedToOrcid: Item = Object.assign(new Item(), {
    id: 'test-id',
    bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    metadata: {
      'dc.title': [
        {
          value: 'test item'
        }
      ],
      'dspace.orcid.authenticated': [
        {
          value: 'true'
        }
      ]
    }
  });

  beforeEach(waitForAsync(() => {
    authService = jasmine.createSpyObj('authService', {
      isAuthenticated: jasmine.createSpy('isAuthenticated'),
      navigateByUrl: jasmine.createSpy('navigateByUrl')
    });

    routeData = {
      dso: createSuccessfulRemoteDataObject(mockItem),
    };

    routeStub = new ActivatedRouteStub({}, routeData);

    orcidAuthService = jasmine.createSpyObj('OrcidAuthService', {
      isLinkedToOrcid: jasmine.createSpy('isLinkedToOrcid'),
      linkOrcidByItem: jasmine.createSpy('linkOrcidByItem'),
    });

    itemDataService = jasmine.createSpyObj('ItemDataService', {
      findById: jasmine.createSpy('findById')
    });

    void TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
        RouterTestingModule.withRoutes([])
      ],
      declarations: [OrcidPageComponent],
      providers: [
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: OrcidAuthService, useValue: orcidAuthService },
        { provide: AuthService, useValue: authService },
        { provide: ItemDataService, useValue: itemDataService },
        { provide: PLATFORM_ID, useValue: 'browser' },
      ],

      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    scheduler = getTestScheduler();
    fixture = TestBed.createComponent(OrcidPageComponent);
    comp = fixture.componentInstance;
    authService.isAuthenticated.and.returnValue(observableOf(true));
  }));

  describe('whn has no query param', () => {
    beforeEach(waitForAsync(() => {
      fixture.detectChanges();
    }));

    it('should create', () => {
      const btn = fixture.debugElement.queryAll(By.css('[data-test="back-button"]'));
      const auth = fixture.debugElement.query(By.css('[data-test="orcid-auth"]'));
      const settings = fixture.debugElement.query(By.css('[data-test="orcid-sync-setting"]'));
      expect(comp).toBeTruthy();
      expect(btn.length).toBe(1);
      expect(auth).toBeTruthy();
      expect(settings).toBeTruthy();
      expect(comp.itemId).toBe('test-id');
    });

    it('should call isLinkedToOrcid', () => {
      comp.isLinkedToOrcid();

      expect(orcidAuthService.isLinkedToOrcid).toHaveBeenCalledWith(comp.item.value);
    });

    it('should update item', fakeAsync(() => {
      itemDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockItemLinkedToOrcid));
      scheduler.schedule(() => comp.updateItem());
      scheduler.flush();

      expect(comp.item.value).toEqual(mockItemLinkedToOrcid);
    }));
  });

  describe('when query param contains orcid code', () => {
    beforeEach(waitForAsync(() => {
      spyOn(comp, 'updateItem').and.callThrough();
      routeStub.testParams = {
        code: 'orcid-code'
      };
    }));

    describe('and linking to orcid profile is successfully', () => {
      beforeEach(waitForAsync(() => {
        orcidAuthService.linkOrcidByItem.and.returnValue(createSuccessfulRemoteDataObject$(mockResearcherProfile));
        itemDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockItemLinkedToOrcid));
        fixture.detectChanges();
      }));

      it('should call linkOrcidByItem', () => {
        expect(orcidAuthService.linkOrcidByItem).toHaveBeenCalledWith(mockItem, 'orcid-code');
        expect(comp.updateItem).toHaveBeenCalled();
      });

      it('should create', () => {
        const btn = fixture.debugElement.queryAll(By.css('[data-test="back-button"]'));
        const auth = fixture.debugElement.query(By.css('[data-test="orcid-auth"]'));
        const settings = fixture.debugElement.query(By.css('[data-test="orcid-sync-setting"]'));
        expect(comp).toBeTruthy();
        expect(btn.length).toBe(1);
        expect(auth).toBeTruthy();
        expect(settings).toBeTruthy();
        expect(comp.itemId).toBe('test-id');
      });

    });

    describe('and linking to orcid profile is failed', () => {
      beforeEach(waitForAsync(() => {
        orcidAuthService.linkOrcidByItem.and.returnValue(createFailedRemoteDataObject$());
        itemDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockItemLinkedToOrcid));
        fixture.detectChanges();
      }));

      it('should call linkOrcidByItem', () => {
        expect(orcidAuthService.linkOrcidByItem).toHaveBeenCalledWith(mockItem, 'orcid-code');
        expect(comp.updateItem).not.toHaveBeenCalled();
      });

      it('should create', () => {
        const btn = fixture.debugElement.queryAll(By.css('[data-test="back-button"]'));
        const auth = fixture.debugElement.query(By.css('[data-test="orcid-auth"]'));
        const settings = fixture.debugElement.query(By.css('[data-test="orcid-sync-setting"]'));
        const error = fixture.debugElement.query(By.css('[data-test="error-box"]'));
        expect(comp).toBeTruthy();
        expect(btn.length).toBe(1);
        expect(error).toBeTruthy();
        expect(auth).toBeFalsy();
        expect(settings).toBeFalsy();
      });

    });
  });
});
