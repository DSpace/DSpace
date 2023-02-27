import { DenyRequestCopyComponent } from './deny-request-copy.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VarDirective } from '../../shared/utils/var.directive';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ItemDataService } from '../../core/data/item-data.service';
import { DSONameService } from '../../core/breadcrumbs/dso-name.service';
import { ItemRequestDataService } from '../../core/data/item-request-data.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { of as observableOf } from 'rxjs';
import {
  createFailedRemoteDataObject$,
  createSuccessfulRemoteDataObject,
  createSuccessfulRemoteDataObject$
} from '../../shared/remote-data.utils';
import { ItemRequest } from '../../core/shared/item-request.model';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { Item } from '../../core/shared/item.model';
import { RequestCopyEmail } from '../email-request-copy/request-copy-email.model';

describe('DenyRequestCopyComponent', () => {
  let component: DenyRequestCopyComponent;
  let fixture: ComponentFixture<DenyRequestCopyComponent>;

  let router: Router;
  let route: ActivatedRoute;
  let authService: AuthService;
  let translateService: TranslateService;
  let itemDataService: ItemDataService;
  let nameService: DSONameService;
  let itemRequestService: ItemRequestDataService;
  let notificationsService: NotificationsService;

  let itemRequest: ItemRequest;
  let user: EPerson;
  let item: Item;
  let itemName: string;
  let itemUrl: string;

  beforeEach(waitForAsync(() => {
    itemRequest = Object.assign(new ItemRequest(), {
      token: 'item-request-token',
      requestName: 'requester name'
    });
    user = Object.assign(new EPerson(), {
      metadata: {
        'eperson.firstname': [
          {
            value: 'first'
          }
        ],
        'eperson.lastname': [
          {
            value: 'last'
          }
        ]
      },
      email: 'user-email',
    });
    itemName = 'item-name';
    itemUrl = 'item-url';
    item = Object.assign(new Item(), {
      id: 'item-id',
      metadata: {
        'dc.identifier.uri': [
          {
            value: itemUrl
          }
        ],
        'dc.title': [
          {
            value: itemName
          }
        ]
      }
    });

    router = jasmine.createSpyObj('router', {
      navigateByUrl: jasmine.createSpy('navigateByUrl'),
    });
    route = jasmine.createSpyObj('route', {}, {
      data: observableOf({
        request: createSuccessfulRemoteDataObject(itemRequest),
      }),
    });
    authService = jasmine.createSpyObj('authService', {
      isAuthenticated: observableOf(true),
      getAuthenticatedUserFromStore: observableOf(user),
    });
    itemDataService = jasmine.createSpyObj('itemDataService', {
      findById: createSuccessfulRemoteDataObject$(item),
    });
    nameService = jasmine.createSpyObj('nameService', {
      getName: itemName,
    });
    itemRequestService = jasmine.createSpyObj('itemRequestService', {
      deny: createSuccessfulRemoteDataObject$(itemRequest),
    });
    notificationsService = jasmine.createSpyObj('notificationsService', ['success', 'error']);

    TestBed.configureTestingModule({
      declarations: [DenyRequestCopyComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: route },
        { provide: AuthService, useValue: authService },
        { provide: ItemDataService, useValue: itemDataService },
        { provide: DSONameService, useValue: nameService },
        { provide: ItemRequestDataService, useValue: itemRequestService },
        { provide: NotificationsService, useValue: notificationsService },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DenyRequestCopyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    translateService = (component as any).translateService;
    spyOn(translateService, 'get').and.returnValue(observableOf('translated-message'));
  });

  it('message$ should be parameterized correctly', (done) => {
    component.message$.subscribe(() => {
      expect(translateService.get).toHaveBeenCalledWith(jasmine.anything(), Object.assign({
        recipientName: itemRequest.requestName,
        itemUrl: itemUrl,
        itemName: itemName,
        authorName: user.name,
        authorEmail: user.email,
      }));
      done();
    });
  });

  describe('deny', () => {
    let email: RequestCopyEmail;

    describe('when the request is successful', () => {
      beforeEach(() => {
        email = new RequestCopyEmail('subject', 'message');
        (itemRequestService.deny as jasmine.Spy).and.returnValue(createSuccessfulRemoteDataObject$(itemRequest));
        component.deny(email);
      });

      it('should display a success notification', () => {
        expect(notificationsService.success).toHaveBeenCalled();
      });

      it('should navigate to the homepage', () => {
        expect(router.navigateByUrl).toHaveBeenCalledWith('/');
      });
    });

    describe('when the request is unsuccessful', () => {
      beforeEach(() => {
        email = new RequestCopyEmail('subject', 'message');
        (itemRequestService.deny as jasmine.Spy).and.returnValue(createFailedRemoteDataObject$());
        component.deny(email);
      });

      it('should display a success notification', () => {
        expect(notificationsService.error).toHaveBeenCalled();
      });

      it('should not navigate', () => {
        expect(router.navigateByUrl).not.toHaveBeenCalled();
      });
    });
  });
});
