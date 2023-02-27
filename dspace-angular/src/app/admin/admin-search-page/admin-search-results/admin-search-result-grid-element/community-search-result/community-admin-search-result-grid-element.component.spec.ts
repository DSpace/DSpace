import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { BitstreamDataService } from '../../../../../core/data/bitstream-data.service';
import { mockTruncatableService } from '../../../../../shared/mocks/mock-trucatable.service';
import { SharedModule } from '../../../../../shared/shared.module';
import { TruncatableService } from '../../../../../shared/truncatable/truncatable.service';
import { CollectionElementLinkType } from '../../../../../shared/object-collection/collection-element-link.type';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { CommunityAdminSearchResultGridElementComponent } from './community-admin-search-result-grid-element.component';
import { CommunitySearchResult } from '../../../../../shared/object-collection/shared/community-search-result.model';
import { Community } from '../../../../../core/shared/community.model';
import { getCommunityEditRoute } from '../../../../../community-page/community-page-routing-paths';
import { LinkService } from '../../../../../core/cache/builders/link.service';
import { AuthService } from '../../../../../core/auth/auth.service';
import { AuthServiceStub } from '../../../../../shared/testing/auth-service.stub';
import { FileService } from '../../../../../core/shared/file.service';
import { FileServiceStub } from '../../../../../shared/testing/file-service.stub';
import { AuthorizationDataService } from '../../../../../core/data/feature-authorization/authorization-data.service';
import { AuthorizationDataServiceStub } from '../../../../../shared/testing/authorization-service.stub';
import { ThemeService } from '../../../../../shared/theme-support/theme.service';
import { getMockThemeService } from '../../../../../shared/mocks/theme-service.mock';

describe('CommunityAdminSearchResultGridElementComponent', () => {
  let component: CommunityAdminSearchResultGridElementComponent;
  let fixture: ComponentFixture<CommunityAdminSearchResultGridElementComponent>;
  let id;
  let searchResult;

  function init() {
    id = '780b2588-bda5-4112-a1cd-0b15000a5339';
    searchResult = new CommunitySearchResult();
    searchResult.indexableObject = new Community();
    searchResult.indexableObject.uuid = id;
  }

  const linkService = jasmine.createSpyObj('linkService', {
    resolveLink: {}
  });

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        RouterTestingModule.withRoutes([]),
        SharedModule
      ],
      declarations: [CommunityAdminSearchResultGridElementComponent],
      providers: [
        { provide: TruncatableService, useValue: mockTruncatableService },
        { provide: BitstreamDataService, useValue: {} },
        { provide: LinkService, useValue: linkService },
        { provide: AuthService, useClass: AuthServiceStub },
        { provide: FileService, useClass: FileServiceStub },
        { provide: AuthorizationDataService, useClass: AuthorizationDataServiceStub },
        { provide: ThemeService, useValue: getMockThemeService() },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CommunityAdminSearchResultGridElementComponent);
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

  it('should render an edit button with the correct link', () => {
    const a = fixture.debugElement.query(By.css('a.edit-link'));
    const link = a.nativeElement.href;
    expect(link).toContain(getCommunityEditRoute(id));
  });
});
