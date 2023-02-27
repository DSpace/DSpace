import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DsoPageSubscriptionButtonComponent } from './dso-page-subscription-button.component';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';
import { of as observableOf } from 'rxjs';
import { NgbModalModule } from '@ng-bootstrap/ng-bootstrap';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { Item } from '../../../core/shared/item.model';
import { ITEM } from '../../../core/shared/item.resource-type';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateLoaderMock } from '../../mocks/translate-loader.mock';

describe('DsoPageSubscriptionButtonComponent', () => {
  let component: DsoPageSubscriptionButtonComponent;
  let fixture: ComponentFixture<DsoPageSubscriptionButtonComponent>;
  let de: DebugElement;

  const authorizationService = jasmine.createSpyObj('authorizationService', {
    isAuthorized: jasmine.createSpy('isAuthorized') // observableOf(true)
  });

  const mockItem = Object.assign(new Item(), {
    id: 'fake-id',
    uuid: 'fake-id',
    handle: 'fake/handle',
    lastModified: '2018',
    type: ITEM,
    _links: {
      self: {
        href: 'https://localhost:8000/items/fake-id'
      }
    }
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NgbModalModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      declarations: [ DsoPageSubscriptionButtonComponent ],
      providers: [
        { provide: AuthorizationDataService, useValue: authorizationService },
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DsoPageSubscriptionButtonComponent);
    component = fixture.componentInstance;
    de = fixture.debugElement;
    component.dso = mockItem;
  });

  describe('when is authorized', () => {
    beforeEach(() => {
      authorizationService.isAuthorized.and.returnValue(observableOf(true));
      fixture.detectChanges();
    });

    it('should display subscription button', () => {
      expect(de.query(By.css(' [data-test="subscription-button"]'))).toBeTruthy();
    });
  });

  describe('when is not authorized', () => {
    beforeEach(() => {
      authorizationService.isAuthorized.and.returnValue(observableOf(false));
      fixture.detectChanges();
    });

    it('should not display subscription button', () => {
      expect(de.query(By.css(' [data-test="subscription-button"]'))).toBeNull();
    });
  });
});
