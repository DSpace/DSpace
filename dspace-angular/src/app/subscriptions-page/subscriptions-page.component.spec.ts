import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrowserModule, By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { of as observableOf } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { SubscriptionsPageComponent } from './subscriptions-page.component';
import { PaginationService } from '../core/pagination/pagination.service';
import { SubscriptionsDataService } from '../shared/subscriptions/subscriptions-data.service';
import { PaginationServiceStub } from '../shared/testing/pagination-service.stub';
import { AuthService } from '../core/auth/auth.service';
import { TranslateLoaderMock } from '../shared/mocks/translate-loader.mock';
import {
  mockSubscriptionEperson,
  subscriptionMock,
  subscriptionMock2
} from '../shared/testing/subscriptions-data.mock';
import { MockActivatedRoute } from '../shared/mocks/active-router.mock';
import { VarDirective } from '../shared/utils/var.directive';
import { SubscriptionViewComponent } from '../shared/subscriptions/subscription-view/subscription-view.component';
import { PageInfo } from '../core/shared/page-info.model';
import { createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';
import { buildPaginatedList } from '../core/data/paginated-list.model';

describe('SubscriptionsPageComponent', () => {
  let component: SubscriptionsPageComponent;
  let fixture: ComponentFixture<SubscriptionsPageComponent>;
  let de: DebugElement;

  const authServiceStub = jasmine.createSpyObj('authorizationService', {
    getAuthenticatedUserFromStore: observableOf(mockSubscriptionEperson)
  });

  const subscriptionServiceStub = jasmine.createSpyObj('SubscriptionsDataService', {
    findByEPerson: jasmine.createSpy('findByEPerson')
  });

  const paginationService = new PaginationServiceStub();

  const mockSubscriptionList = [subscriptionMock, subscriptionMock2];

  const emptyPageInfo = Object.assign(new PageInfo(), {
    totalElements: 0
  });

  const pageInfo = Object.assign(new PageInfo(), {
    totalElements: 2
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        BrowserModule,
        RouterTestingModule.withRoutes([]),
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
        NoopAnimationsModule
      ],
      declarations: [SubscriptionsPageComponent, SubscriptionViewComponent, VarDirective],
      providers: [
        { provide: SubscriptionsDataService, useValue: subscriptionServiceStub },
        { provide: ActivatedRoute, useValue: new MockActivatedRoute() },
        { provide: AuthService, useValue: authServiceStub },
        { provide: PaginationService, useValue: paginationService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SubscriptionsPageComponent);
    component = fixture.componentInstance;
    de = fixture.debugElement;
  });

  describe('when there are subscriptions', () => {

    beforeEach(() => {
      subscriptionServiceStub.findByEPerson.and.returnValue(createSuccessfulRemoteDataObject$(buildPaginatedList(pageInfo, mockSubscriptionList)));
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should show table', () => {
      expect(de.query(By.css('[data-test="subscription-table"]'))).toBeTruthy();
      expect(de.query(By.css('[data-test="empty-alert"]'))).toBeNull();
    });

    it('should show a row for each results entry',() => {
      expect(de.query(By.css('[data-test="subscription-table"]'))).toBeTruthy();
      expect(de.query(By.css('[data-test="empty-alert"]'))).toBeNull();
      expect(de.queryAll(By.css('tbody > tr')).length).toEqual(2);
    });
  });

  describe('when there are no subscriptions', () => {

    beforeEach(() => {
      subscriptionServiceStub.findByEPerson.and.returnValue(createSuccessfulRemoteDataObject$(buildPaginatedList(emptyPageInfo, [])));
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should not show table', () => {
      expect(de.query(By.css('[data-test="subscription-table"]'))).toBeNull();
      expect(de.query(By.css('[data-test="empty-alert"]'))).toBeTruthy();
    });
  });

});
