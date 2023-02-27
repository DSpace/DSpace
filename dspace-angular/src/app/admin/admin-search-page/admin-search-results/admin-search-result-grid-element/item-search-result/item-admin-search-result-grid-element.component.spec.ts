import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { BitstreamDataService } from '../../../../../core/data/bitstream-data.service';
import { RemoteData } from '../../../../../core/data/remote-data';
import { Bitstream } from '../../../../../core/shared/bitstream.model';
import { Item } from '../../../../../core/shared/item.model';
import { mockTruncatableService } from '../../../../../shared/mocks/mock-trucatable.service';
import { SharedModule } from '../../../../../shared/shared.module';
import { TruncatableService } from '../../../../../shared/truncatable/truncatable.service';
import { CollectionElementLinkType } from '../../../../../shared/object-collection/collection-element-link.type';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { RouterTestingModule } from '@angular/router/testing';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { ItemAdminSearchResultGridElementComponent } from './item-admin-search-result-grid-element.component';
import { createSuccessfulRemoteDataObject$ } from '../../../../../shared/remote-data.utils';
import { getMockThemeService } from '../../../../../shared/mocks/theme-service.mock';
import { ThemeService } from '../../../../../shared/theme-support/theme.service';
import { AccessStatusDataService } from '../../../../../core/data/access-status-data.service';
import { AccessStatusObject } from '../../../../../shared/object-list/access-status-badge/access-status.model';
import { AuthService } from '../../../../../core/auth/auth.service';
import { AuthServiceStub } from '../../../../../shared/testing/auth-service.stub';
import { FileService } from '../../../../../core/shared/file.service';
import { FileServiceStub } from '../../../../../shared/testing/file-service.stub';
import { AuthorizationDataService } from '../../../../../core/data/feature-authorization/authorization-data.service';
import { AuthorizationDataServiceStub } from '../../../../../shared/testing/authorization-service.stub';

describe('ItemAdminSearchResultGridElementComponent', () => {
  let component: ItemAdminSearchResultGridElementComponent;
  let fixture: ComponentFixture<ItemAdminSearchResultGridElementComponent>;
  let id;
  let searchResult;

  const mockBitstreamDataService = {
    getThumbnailFor(item: Item): Observable<RemoteData<Bitstream>> {
      return createSuccessfulRemoteDataObject$(new Bitstream());
    }
  };

  const mockAccessStatusDataService = {
    findAccessStatusFor(item: Item): Observable<RemoteData<AccessStatusObject>> {
      return createSuccessfulRemoteDataObject$(new AccessStatusObject());
    }
  };

  const mockThemeService = getMockThemeService();

  function init() {
    id = '780b2588-bda5-4112-a1cd-0b15000a5339';
    searchResult = new ItemSearchResult();
    searchResult.indexableObject = new Item();
    searchResult.indexableObject.uuid = id;
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule(
      {
        declarations: [ItemAdminSearchResultGridElementComponent],
        imports: [
          NoopAnimationsModule,
          TranslateModule.forRoot(),
          RouterTestingModule.withRoutes([]),
          SharedModule
        ],
        providers: [
          { provide: TruncatableService, useValue: mockTruncatableService },
          { provide: BitstreamDataService, useValue: mockBitstreamDataService },
          { provide: ThemeService, useValue: mockThemeService },
          { provide: AccessStatusDataService, useValue: mockAccessStatusDataService },
          { provide: AuthService, useClass: AuthServiceStub },
          { provide: FileService, useClass: FileServiceStub },
          { provide: AuthorizationDataService, useClass: AuthorizationDataServiceStub },
        ],
        schemas: [NO_ERRORS_SCHEMA]
      })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemAdminSearchResultGridElementComponent);
    component = fixture.componentInstance;
    component.object = searchResult;
    component.linkTypes = CollectionElementLinkType;
    component.index = 0;
    component.viewModes = ViewMode;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
