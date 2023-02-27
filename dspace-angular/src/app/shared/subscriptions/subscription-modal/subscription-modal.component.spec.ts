import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { NgbActiveModal, NgbModalModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { SubscriptionModalComponent } from './subscription-modal.component';
import { TranslateLoaderMock } from '../../mocks/translate-loader.mock';
import { NotificationsService } from '../../notifications/notifications.service';
import { SubscriptionsDataService } from '../subscriptions-data.service';
import { createSuccessfulRemoteDataObject$ } from '../../remote-data.utils';
import { Item } from '../../../core/shared/item.model';
import { AuthService } from '../../../core/auth/auth.service';
import { EPerson } from '../../../core/eperson/models/eperson.model';
import { PageInfo } from '../../../core/shared/page-info.model';
import { buildPaginatedList } from '../../../core/data/paginated-list.model';
import { By } from '@angular/platform-browser';
import { subscriptionMock, subscriptionMock2 } from '../../testing/subscriptions-data.mock';

describe('SubscriptionModalComponent', () => {
  let component: SubscriptionModalComponent;
  let fixture: ComponentFixture<SubscriptionModalComponent>;
  let de: DebugElement;

  let subscriptionServiceStub;

  const notificationServiceStub = jasmine.createSpyObj('authService', {
    notificationWithAnchor: true,
    success: undefined,
  });

  const emptyPageInfo = Object.assign(new PageInfo(), {
    'elementsPerPage': 0,
    'totalElements': 0
  });


  const pageInfo = Object.assign(new PageInfo(), {
    'elementsPerPage': 2,
    'totalElements': 2
  });

  const mockEperson = Object.assign(new EPerson(), {
    id: 'fake-id',
    uuid: 'fake-id',
    _links: {
      self: {
        href: 'https://localhost:8000/eperson/fake-id'
      }
    }
  });

  const mockItem = Object.assign(new Item(), {
    id: 'fake-id',
    uuid: 'fake-id',
    handle: 'fake/handle',
    lastModified: '2018',
    _links: {
      self: {
        href: 'https://localhost:8000/items/fake-id'
      }
    }
  });

  const authService = jasmine.createSpyObj('authService', {
    getAuthenticatedUserFromStore: createSuccessfulRemoteDataObject$(mockEperson)
  });

  subscriptionServiceStub = jasmine.createSpyObj('SubscriptionsDataService', {
    getSubscriptionsByPersonDSO: jasmine.createSpy('getSubscriptionsByPersonDSO'),
    createSubscription: createSuccessfulRemoteDataObject$({}),
    updateSubscription: createSuccessfulRemoteDataObject$({}),
  });

  beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        NgbModalModule,
        ReactiveFormsModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
      ],
      declarations: [SubscriptionModalComponent],
      providers: [
        NgbActiveModal,
        { provide: AuthService, useValue: authService },
        { provide: NotificationsService, useValue: notificationServiceStub },
        { provide: SubscriptionsDataService, useValue: subscriptionServiceStub },
      ],
      schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();

  }));

  describe('when submitting subscriptions', () => {

    const testSubscriptionId = 'test-subscription-id';
    const testTypes = ['test1', 'test2'];
    const testFrequencies = ['f', 'g'];

    beforeEach(() => {
      fixture = TestBed.createComponent(SubscriptionModalComponent);
      component = fixture.componentInstance;
      component.dso = mockItem;
      (component as any).subscriptionDefaultTypes = testTypes;
      (component as any).frequencyDefaultValues = testFrequencies;
      de = fixture.debugElement;
      subscriptionServiceStub.createSubscription.calls.reset();
      subscriptionServiceStub.updateSubscription.calls.reset();
      fixture.detectChanges();
    });

    it('should edit an existing subscription', () => {
      component.subscriptionForm = new FormGroup({});
      for (let t of testTypes) {
        const formGroup = new FormGroup({
          subscriptionId: new FormControl(testSubscriptionId),
          frequencies: new FormGroup({
            f: new FormControl(false),
            g: new FormControl(true),
          })
        });
        component.subscriptionForm.addControl(t, formGroup);
        component.subscriptionForm.get('test1').markAsDirty();
        component.subscriptionForm.get('test1').markAsTouched();
      }

      fixture.detectChanges();
      component.submit();

      expect(subscriptionServiceStub.createSubscription).not.toHaveBeenCalled();
      expect(subscriptionServiceStub.updateSubscription).toHaveBeenCalled();
      expect(component.subscriptionForm.controls).toBeTruthy();
    });

    it('should create a new subscription', () => {
      component.subscriptionForm = new FormGroup({});
      for (let t of testTypes) {
        const formGroup = new FormGroup({
          subscriptionId: new FormControl(undefined),
          frequencies: new FormGroup({
            f: new FormControl(false),
            g: new FormControl(true),
          })
        });
        component.subscriptionForm.addControl(t, formGroup);
        component.subscriptionForm.get('test1').markAsDirty();
        component.subscriptionForm.get('test1').markAsTouched();
      }

      fixture.detectChanges();
      component.submit();

      expect(subscriptionServiceStub.createSubscription).toHaveBeenCalled();
      expect(subscriptionServiceStub.updateSubscription).not.toHaveBeenCalled();
      expect(component.subscriptionForm.controls).toBeTruthy();
    });

  });

  describe('when no subscription is given', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(SubscriptionModalComponent);
      component = fixture.componentInstance;
      component.dso = mockItem;
      (component as any).subscriptionDefaultTypes = ['test1', 'test2'];
      de = fixture.debugElement;
    });

    describe('and no subscriptions are present for the given dso', () => {
      beforeEach(() => {
        subscriptionServiceStub.getSubscriptionsByPersonDSO.and.returnValue(createSuccessfulRemoteDataObject$(buildPaginatedList(emptyPageInfo, [])));
        fixture.detectChanges();
      });

      it('should create', () => {
        expect(component).toBeTruthy();
      });

      it('should init form properly', () => {
        expect(de.query(By.css(' [data-test="subscription-form"]'))).toBeTruthy();
        expect(component.subscriptionForm).toBeTruthy();
        expect(component.subscriptionForm.get('test1')).toBeTruthy();
        expect(component.subscriptionForm.get('test2')).toBeTruthy();
        (component as any).frequencyDefaultValues.forEach((frequency) => {
          expect(component.subscriptionForm.get('test1').get('frequencies').get(frequency)).toBeTruthy();
          expect(component.subscriptionForm.get('test2').get('frequencies').get(frequency)).toBeTruthy();
        });
      });
    });

    describe('and subscriptions are present for the given dso', () => {
      beforeEach(() => {
        subscriptionServiceStub.getSubscriptionsByPersonDSO.and.returnValue(createSuccessfulRemoteDataObject$(buildPaginatedList(pageInfo, [subscriptionMock, subscriptionMock2])));
        fixture.detectChanges();
      });

      it('should create', () => {
        expect(component).toBeTruthy();
      });

      it('should init form properly', () => {
        expect(de.query(By.css(' [data-test="subscription-form"]'))).toBeTruthy();
        expect(component.subscriptionForm).toBeTruthy();
        expect(component.subscriptionForm.get('test1')).toBeTruthy();
        expect(component.subscriptionForm.get('test2')).toBeTruthy();
        (component as any).frequencyDefaultValues.forEach((frequency) => {
          expect(component.subscriptionForm.get('test1').get('frequencies').get(frequency)).toBeTruthy();

          expect(component.subscriptionForm.get('test2').get('frequencies').get(frequency)).toBeTruthy();
        });
        expect(component.subscriptionForm.get('test1').get('frequencies').get('D').value).toBeTrue();
        expect(component.subscriptionForm.get('test1').get('frequencies').get('M').value).toBeTrue();
        expect(component.subscriptionForm.get('test1').get('frequencies').get('W').value).toBeFalse();

        expect(component.subscriptionForm.get('test2').get('frequencies').get('D').value).toBeTrue();
        expect(component.subscriptionForm.get('test2').get('frequencies').get('M').value).toBeFalse();
        expect(component.subscriptionForm.get('test2').get('frequencies').get('W').value).toBeFalse();
      });
    });
  });

  describe('when no subscription is given', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(SubscriptionModalComponent);
      component = fixture.componentInstance;
      component.dso = mockItem;
      component.subscription = subscriptionMock as any;
      (component as any).subscriptionDefaultTypes = ['test1', 'test2'];
      de = fixture.debugElement;
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should init form properly', () => {
      expect(de.query(By.css(' [data-test="subscription-form"]'))).toBeTruthy();
      expect(component.subscriptionForm).toBeTruthy();
      expect(component.subscriptionForm.get('test1')).toBeTruthy();
      (component as any).frequencyDefaultValues.forEach((frequency) => {
        expect(component.subscriptionForm.get('test1').get('frequencies').get(frequency)).toBeTruthy();
      });
      expect(component.subscriptionForm.get('test1').get('frequencies').get('D').value).toBeTrue();
      expect(component.subscriptionForm.get('test1').get('frequencies').get('M').value).toBeTrue();
      expect(component.subscriptionForm.get('test1').get('frequencies').get('W').value).toBeFalse();
    });
  });

});
