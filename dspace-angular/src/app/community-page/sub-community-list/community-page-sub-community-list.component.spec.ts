import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { CommunityPageSubCommunityListComponent } from './community-page-sub-community-list.component';
import { Community } from '../../core/shared/community.model';
import { buildPaginatedList } from '../../core/data/paginated-list.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { SharedModule } from '../../shared/shared.module';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { HostWindowService } from '../../shared/host-window.service';
import { HostWindowServiceStub } from '../../shared/testing/host-window-service.stub';
import { CommunityDataService } from '../../core/data/community-data.service';
import { SelectableListService } from '../../shared/object-list/selectable-list/selectable-list.service';
import { PaginationService } from '../../core/pagination/pagination.service';
import { getMockThemeService } from '../../shared/mocks/theme-service.mock';
import { ThemeService } from '../../shared/theme-support/theme.service';
import { PaginationServiceStub } from '../../shared/testing/pagination-service.stub';
import { FindListOptions } from '../../core/data/find-list-options.model';
import { GroupDataService } from '../../core/eperson/group-data.service';
import { LinkHeadService } from '../../core/services/link-head.service';
import { ConfigurationDataService } from '../../core/data/configuration-data.service';
import { SearchConfigurationService } from '../../core/shared/search/search-configuration.service';
import { SearchConfigurationServiceStub } from '../../shared/testing/search-configuration-service.stub';
import { ConfigurationProperty } from '../../core/shared/configuration-property.model';
import { createPaginatedList } from '../../shared/testing/utils.test';

describe('CommunityPageSubCommunityListComponent Component', () => {
  let comp: CommunityPageSubCommunityListComponent;
  let fixture: ComponentFixture<CommunityPageSubCommunityListComponent>;
  let communityDataServiceStub: any;
  let themeService;
  let subCommList = [];

  const subcommunities = [Object.assign(new Community(), {
    id: '123456789-1',
    metadata: {
      'dc.title': [
        { language: 'en_US', value: 'SubCommunity 1' }
      ]
    }
  }),
    Object.assign(new Community(), {
      id: '123456789-2',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'SubCommunity 2' }
        ]
      }
    }),
    Object.assign(new Community(), {
      id: '123456789-3',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'SubCommunity 3' }
        ]
      }
    }),
    Object.assign(new Community(), {
      id: '12345678942',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'SubCommunity 4' }
        ]
      }
    }),
    Object.assign(new Community(), {
      id: '123456789-5',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'SubCommunity 5' }
        ]
      }
    }),
    Object.assign(new Community(), {
      id: '123456789-6',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'SubCommunity 6' }
        ]
      }
    }),
    Object.assign(new Community(), {
      id: '123456789-7',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'SubCommunity 7' }
        ]
      }
    })
  ];

  const mockCommunity = Object.assign(new Community(), {
    id: '123456789',
    metadata: {
      'dc.title': [
        { language: 'en_US', value: 'Test title' }
      ]
    }
  });

  communityDataServiceStub = {
    findByParent(parentUUID: string, options: FindListOptions = {}) {
      let currentPage = options.currentPage;
      let elementsPerPage = options.elementsPerPage;
      if (currentPage === undefined) {
        currentPage = 1;
      }
      elementsPerPage = 5;

      const startPageIndex = (currentPage - 1) * elementsPerPage;
      let endPageIndex = (currentPage * elementsPerPage);
      if (endPageIndex > subCommList.length) {
        endPageIndex = subCommList.length;
      }
      return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), subCommList.slice(startPageIndex, endPageIndex)));

    }
  };

  const linkHeadService = jasmine.createSpyObj('linkHeadService', {
    addTag: ''
  });

  const groupDataService = jasmine.createSpyObj('groupsDataService', {
    findListByHref: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    getGroupRegistryRouterLink: '',
    getUUIDFromString: '',
  });

  const configurationDataService = jasmine.createSpyObj('configurationDataService', {
    findByPropertyName: createSuccessfulRemoteDataObject$(Object.assign(new ConfigurationProperty(), {
      name: 'test',
      values: [
        'org.dspace.ctask.general.ProfileFormats = test'
      ]
    }))
  });

  const paginationService = new PaginationServiceStub();

  themeService = getMockThemeService();

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
        SharedModule,
        RouterTestingModule.withRoutes([]),
        NgbModule,
        NoopAnimationsModule
      ],
      declarations: [CommunityPageSubCommunityListComponent],
      providers: [
        { provide: CommunityDataService, useValue: communityDataServiceStub },
        { provide: HostWindowService, useValue: new HostWindowServiceStub(0) },
        { provide: PaginationService, useValue: paginationService },
        { provide: SelectableListService, useValue: {} },
        { provide: ThemeService, useValue: themeService },
        { provide: GroupDataService, useValue: groupDataService },
        { provide: LinkHeadService, useValue: linkHeadService },
        { provide: ConfigurationDataService, useValue: configurationDataService },
        { provide: SearchConfigurationService, useValue: new SearchConfigurationServiceStub() },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CommunityPageSubCommunityListComponent);
    comp = fixture.componentInstance;
    comp.community = mockCommunity;

  });


  it('should display a list of sub-communities', () => {
    waitForAsync(() => {
      subCommList = subcommunities;
      fixture.detectChanges();

      const subComList = fixture.debugElement.queryAll(By.css('li'));
      expect(subComList.length).toEqual(5);
      expect(subComList[0].nativeElement.textContent).toContain('SubCommunity 1');
      expect(subComList[1].nativeElement.textContent).toContain('SubCommunity 2');
      expect(subComList[2].nativeElement.textContent).toContain('SubCommunity 3');
      expect(subComList[3].nativeElement.textContent).toContain('SubCommunity 4');
      expect(subComList[4].nativeElement.textContent).toContain('SubCommunity 5');
    });
  });

  it('should not display the header when list of sub-communities is empty', () => {
    subCommList = [];
    fixture.detectChanges();

    const subComHead = fixture.debugElement.queryAll(By.css('h2'));
    expect(subComHead.length).toEqual(0);
  });
});
