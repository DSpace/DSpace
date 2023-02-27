import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VarDirective } from '../../shared/utils/var.directive';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ItemDataService } from '../../core/data/item-data.service';
import { DSONameService } from '../../core/breadcrumbs/dso-name.service';
import { of as observableOf } from 'rxjs';
import {
  createSuccessfulRemoteDataObject,
  createSuccessfulRemoteDataObject$
} from '../../shared/remote-data.utils';
import { ItemRequest } from '../../core/shared/item-request.model';
import { Item } from '../../core/shared/item.model';
import { GrantDenyRequestCopyComponent } from './grant-deny-request-copy.component';
import { getItemPageRoute } from '../../item-page/item-page-routing-paths';
import { getRequestCopyDenyRoute, getRequestCopyGrantRoute } from '../request-copy-routing-paths';
import { By } from '@angular/platform-browser';

describe('GrantDenyRequestCopyComponent', () => {
  let component: GrantDenyRequestCopyComponent;
  let fixture: ComponentFixture<GrantDenyRequestCopyComponent>;

  let router: Router;
  let route: ActivatedRoute;
  let authService: AuthService;
  let itemDataService: ItemDataService;
  let nameService: DSONameService;

  let itemRequest: ItemRequest;
  let item: Item;
  let itemName: string;
  let itemUrl: string;

  beforeEach(waitForAsync(() => {
    itemRequest = Object.assign(new ItemRequest(), {
      token: 'item-request-token',
      requestName: 'requester name'
    });
    itemName = 'item-name';
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
    itemUrl = getItemPageRoute(item);

    route = jasmine.createSpyObj('route', {}, {
      data: observableOf({
        request: createSuccessfulRemoteDataObject(itemRequest),
      }),
    });
    authService = jasmine.createSpyObj('authService', {
      isAuthenticated: observableOf(true),
    });
    itemDataService = jasmine.createSpyObj('itemDataService', {
      findById: createSuccessfulRemoteDataObject$(item),
    });
    nameService = jasmine.createSpyObj('nameService', {
      getName: itemName,
    });

    TestBed.configureTestingModule({
      declarations: [GrantDenyRequestCopyComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
        { provide: ActivatedRoute, useValue: route },
        { provide: AuthService, useValue: authService },
        { provide: ItemDataService, useValue: itemDataService },
        { provide: DSONameService, useValue: nameService },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(GrantDenyRequestCopyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    router = (component as any).router;
    spyOn(router, 'navigateByUrl').and.stub();
  });

  it('should initialise itemName$', (done) => {
    component.itemName$.subscribe((result) => {
      expect(result).toEqual(itemName);
      done();
    });
  });

  it('should initialise itemUrl$', (done) => {
    component.itemUrl$.subscribe((result) => {
      expect(result).toEqual(itemUrl);
      done();
    });
  });

  it('should initialise denyRoute$', (done) => {
    component.denyRoute$.subscribe((result) => {
      expect(result).toEqual(getRequestCopyDenyRoute(itemRequest.token));
      done();
    });
  });

  it('should initialise grantRoute$', (done) => {
    component.grantRoute$.subscribe((result) => {
      expect(result).toEqual(getRequestCopyGrantRoute(itemRequest.token));
      done();
    });
  });

  describe('processed message', () => {
    it('should not be displayed when decisionDate is undefined', () => {
      const message = fixture.debugElement.query(By.css('.processed-message'));
      expect(message).toBeNull();
    });

    it('should be displayed when decisionDate is defined', () => {
      component.itemRequestRD$ = createSuccessfulRemoteDataObject$(Object.assign(new ItemRequest(), itemRequest, {
        decisionDate: 'defined-date'
      }));
      fixture.detectChanges();

      const message = fixture.debugElement.query(By.css('.processed-message'));
      expect(message).not.toBeNull();
    });
  });
});
