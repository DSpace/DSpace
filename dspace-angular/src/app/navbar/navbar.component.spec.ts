import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { of as observableOf } from 'rxjs';

import { NavbarComponent } from './navbar.component';
import { ReactiveFormsModule } from '@angular/forms';
import { HostWindowService } from '../shared/host-window.service';
import { HostWindowServiceStub } from '../shared/testing/host-window-service.stub';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Injector, NO_ERRORS_SCHEMA } from '@angular/core';
import { MenuService } from '../shared/menu/menu.service';
import { MenuServiceStub } from '../shared/testing/menu-service.stub';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowseService } from '../core/browse/browse.service';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';
import { buildPaginatedList } from '../core/data/paginated-list.model';
import { BrowseDefinition } from '../core/shared/browse-definition.model';
import { BrowseByDataType } from '../browse-by/browse-by-switcher/browse-by-decorator';
import { Item } from '../core/shared/item.model';
import { AuthorizationDataService } from '../core/data/feature-authorization/authorization-data.service';
import { ThemeService } from '../shared/theme-support/theme.service';
import { getMockThemeService } from '../shared/mocks/theme-service.mock';
import { Store, StoreModule } from '@ngrx/store';
import { AppState, storeModuleConfig } from '../app.reducer';
import { authReducer } from '../core/auth/auth.reducer';
import { provideMockStore } from '@ngrx/store/testing';
import { AuthTokenInfo } from '../core/auth/models/auth-token-info.model';
import { EPersonMock } from '../shared/testing/eperson.mock';

let comp: NavbarComponent;
let fixture: ComponentFixture<NavbarComponent>;
let store: Store<AppState>;
let initialState: any;

const authorizationService = jasmine.createSpyObj('authorizationService', {
  isAuthorized: observableOf(true)
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

const routeStub = {
  data: observableOf({
    dso: createSuccessfulRemoteDataObject(mockItem)
  }),
  children: []
};



describe('NavbarComponent', () => {
  const menuService = new MenuServiceStub();
  let browseDefinitions;
  // waitForAsync beforeEach
  beforeEach(waitForAsync(() => {
    browseDefinitions = [
      Object.assign(
        new BrowseDefinition(), {
          id: 'title',
          dataType: BrowseByDataType.Title,
        }
      ),
      Object.assign(
        new BrowseDefinition(), {
          id: 'dateissued',
          dataType: BrowseByDataType.Date,
          metadataKeys: ['dc.date.issued']
        }
      ),
      Object.assign(
        new BrowseDefinition(), {
          id: 'author',
          dataType: BrowseByDataType.Metadata,
        }
      ),
      Object.assign(
        new BrowseDefinition(), {
          id: 'subject',
          dataType: BrowseByDataType.Metadata,
        }
      ),
    ];
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
      imports: [
        TranslateModule.forRoot(),
        StoreModule.forRoot({ auth: authReducer }, storeModuleConfig),
        NoopAnimationsModule,
        ReactiveFormsModule,
        RouterTestingModule],
      declarations: [NavbarComponent],
      providers: [
        Injector,
        { provide: ThemeService, useValue: getMockThemeService() },
        { provide: MenuService, useValue: menuService },
        { provide: HostWindowService, useValue: new HostWindowServiceStub(800) },
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: BrowseService, useValue: { getBrowseDefinitions: createSuccessfulRemoteDataObject$(buildPaginatedList(undefined, browseDefinitions)) } },
        { provide: AuthorizationDataService, useValue: authorizationService },
        provideMockStore({ initialState }),
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();  // compile template and css
  }));

  // synchronous beforeEach
  beforeEach(() => {
    store = TestBed.inject(Store);
    fixture = TestBed.createComponent(NavbarComponent);

    comp = fixture.componentInstance;

  });

  it('should create', () => {
    expect(comp).toBeTruthy();
  });


});
