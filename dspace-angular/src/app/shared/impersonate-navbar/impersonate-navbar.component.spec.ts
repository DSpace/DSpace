import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';

import { TranslateModule } from '@ngx-translate/core';
import { Store, StoreModule } from '@ngrx/store';
import { By } from '@angular/platform-browser';
import { provideMockStore } from '@ngrx/store/testing';

import { ImpersonateNavbarComponent } from './impersonate-navbar.component';
import { VarDirective } from '../utils/var.directive';
import { AuthService } from '../../core/auth/auth.service';
import { authReducer } from '../../core/auth/auth.reducer';
import { AuthTokenInfo } from '../../core/auth/models/auth-token-info.model';
import { EPersonMock } from '../testing/eperson.mock';
import { AppState, storeModuleConfig } from '../../app.reducer';

describe('ImpersonateNavbarComponent', () => {
  let component: ImpersonateNavbarComponent;
  let fixture: ComponentFixture<ImpersonateNavbarComponent>;
  let authService: AuthService;
  let initialState: any;
  let store: Store<AppState>;

  beforeEach(waitForAsync(() => {
    authService = jasmine.createSpyObj('authService', {
      isImpersonating: false,
      stopImpersonatingAndRefresh: {}
    });
    initialState = {
      core: {
        auth: {
          authenticated: true,
          loaded: true,
          blocking: false,
          loading: false,
          authToken: new AuthTokenInfo('test_token'),
          userId: EPersonMock.id,
          authMethods: []
        }
      }
    };

    TestBed.configureTestingModule({
      declarations: [ImpersonateNavbarComponent, VarDirective],
      imports: [
        TranslateModule.forRoot(),
        RouterTestingModule.withRoutes([]),
        StoreModule.forRoot({ auth: authReducer }, storeModuleConfig),
      ],
      providers: [
        { provide: AuthService, useValue: authService },
        provideMockStore({ initialState }),
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    store = TestBed.inject(Store);
    fixture = TestBed.createComponent(ImpersonateNavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('when the user is impersonating another user', () => {
    beforeEach(() => {
      component.isImpersonating = true;
      fixture.detectChanges();
    });

    it('should display a button', () => {
      const button = fixture.debugElement.query(By.css('button'));
      expect(button).not.toBeNull();
    });

    it('should call authService\'s stopImpersonatingAndRefresh upon clicking the button', () => {
      const button = fixture.debugElement.query(By.css('button')).nativeElement;
      button.click();
      expect(authService.stopImpersonatingAndRefresh).toHaveBeenCalled();
    });
  });

  describe('when the user is not impersonating another user', () => {
    it('should not display a button', () => {
      const button = fixture.debugElement.query(By.css('button'));
      expect(button).toBeNull();
    });
  });
});
