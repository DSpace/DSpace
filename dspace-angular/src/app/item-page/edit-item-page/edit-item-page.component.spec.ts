/* eslint-disable max-classes-per-file */
import { ComponentFixture, fakeAsync, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateLoaderMock } from '../../shared/mocks/translate-loader.mock';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { EditItemPageComponent } from './edit-item-page.component';
import { Observable, of as observableOf } from 'rxjs';
import { By } from '@angular/platform-browser';
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { Item } from '../../core/shared/item.model';

describe('ItemPageComponent', () => {
  let comp: EditItemPageComponent;
  let fixture: ComponentFixture<EditItemPageComponent>;

  class AcceptAllGuard implements CanActivate {
    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
      return observableOf(true);
    }
  }

  class AcceptNoneGuard implements CanActivate {
    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
      return observableOf(false);
    }
  }

  const accesiblePages = ['accessible'];
  const inaccesiblePages = ['inaccessible', 'inaccessibleDoubleGuard'];
  const mockRoute = {
    snapshot: {
      firstChild: {
        routeConfig: {
          path: accesiblePages[0]
        }
      },
      routerState: {
        snapshot: undefined
      }
    },
    routeConfig: {
      children: [
        {
          path: accesiblePages[0],
          canActivate: [AcceptAllGuard]
        }, {
          path: inaccesiblePages[0],
          canActivate: [AcceptNoneGuard]
        }, {
          path: inaccesiblePages[1],
          canActivate: [AcceptAllGuard, AcceptNoneGuard]
        },
      ]
    },
    data: observableOf({dso: createSuccessfulRemoteDataObject(new Item())})
  };

  const mockRouter = {
    routerState: {
      snapshot: undefined
    },
    events: observableOf(undefined)
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useClass: TranslateLoaderMock
        }
      })],
      declarations: [EditItemPageComponent],
      providers: [
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: Router, useValue: mockRouter },
        AcceptAllGuard,
        AcceptNoneGuard,
      ],

      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(EditItemPageComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(EditItemPageComponent);
    comp = fixture.componentInstance;
    spyOn((comp as any).injector, 'get').and.callFake((a) => new a());
    fixture.detectChanges();
  }));

  describe('ngOnInit', () => {
    it('should enable tabs that the user can activate', fakeAsync(() => {
      const enabledItems = fixture.debugElement.queryAll(By.css('a.nav-link'));
      expect(enabledItems.length).toBe(accesiblePages.length);
    }));

    it('should disable tabs that the user can not activate', () => {
      const disabledItems = fixture.debugElement.queryAll(By.css('button.nav-link.disabled'));
      expect(disabledItems.length).toBe(inaccesiblePages.length);
    });
  });
});
