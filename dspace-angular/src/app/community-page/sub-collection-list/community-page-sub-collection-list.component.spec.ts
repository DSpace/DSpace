import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { CommunityPageSubCollectionListComponent } from './community-page-sub-collection-list.component';
import { Community } from '../../core/shared/community.model';
import { SharedModule } from '../../shared/shared.module';
import { CollectionDataService } from '../../core/data/collection-data.service';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { buildPaginatedList } from '../../core/data/paginated-list.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { HostWindowService } from '../../shared/host-window.service';
import { HostWindowServiceStub } from '../../shared/testing/host-window-service.stub';
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
import { ConfigurationProperty } from '../../core/shared/configuration-property.model';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { SearchConfigurationServiceStub } from '../../shared/testing/search-configuration-service.stub';

describe('CommunityPageSubCollectionList Component', () => {
  let comp: CommunityPageSubCollectionListComponent;
  let fixture: ComponentFixture<CommunityPageSubCollectionListComponent>;
  let collectionDataServiceStub: any;
  let themeService;
  let subCollList = [];

  const collections = [Object.assign(new Community(), {
    id: '123456789-1',
    metadata: {
      'dc.title': [
        { language: 'en_US', value: 'Collection 1' }
      ]
    }
  }),
    Object.assign(new Community(), {
      id: '123456789-2',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'Collection 2' }
        ]
      }
    }),
    Object.assign(new Community(), {
      id: '123456789-3',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'Collection 3' }
        ]
      }
    }),
    Object.assign(new Community(), {
      id: '123456789-4',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'Collection 4' }
        ]
      }
    }),
    Object.assign(new Community(), {
      id: '123456789-5',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'Collection 5' }
        ]
      }
    }),
    Object.assign(new Community(), {
      id: '123456789-6',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'Collection 6' }
        ]
      }
    }),
    Object.assign(new Community(), {
      id: '123456789-7',
      metadata: {
        'dc.title': [
          { language: 'en_US', value: 'Collection 7' }
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

  collectionDataServiceStub = {
    findByParent(parentUUID: string, options: FindListOptions = {}) {
      let currentPage = options.currentPage;
      let elementsPerPage = options.elementsPerPage;
      if (currentPage === undefined) {
        currentPage = 1;
      }
      elementsPerPage = 5;
      const startPageIndex = (currentPage - 1) * elementsPerPage;
      let endPageIndex = (currentPage * elementsPerPage);
      if (endPageIndex > subCollList.length) {
        endPageIndex = subCollList.length;
      }
      return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), subCollList.slice(startPageIndex, endPageIndex)));

    }
  };

  const paginationService = new PaginationServiceStub();

  themeService = getMockThemeService();

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

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
        SharedModule,
        RouterTestingModule.withRoutes([]),
        NgbModule,
        NoopAnimationsModule
      ],
      declarations: [CommunityPageSubCollectionListComponent],
      providers: [
        { provide: CollectionDataService, useValue: collectionDataServiceStub },
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
    fixture = TestBed.createComponent(CommunityPageSubCollectionListComponent);
    comp = fixture.componentInstance;
    comp.community = mockCommunity;
  });


  it('should display a list of collections', () => {
    waitForAsync(() => {
      subCollList = collections;
      fixture.detectChanges();

      const collList = fixture.debugElement.queryAll(By.css('li'));
      expect(collList.length).toEqual(5);
      expect(collList[0].nativeElement.textContent).toContain('Collection 1');
      expect(collList[1].nativeElement.textContent).toContain('Collection 2');
      expect(collList[2].nativeElement.textContent).toContain('Collection 3');
      expect(collList[3].nativeElement.textContent).toContain('Collection 4');
      expect(collList[4].nativeElement.textContent).toContain('Collection 5');
    });
  });

  it('should not display the header when list of collections is empty', () => {
    subCollList = [];
    fixture.detectChanges();

    const subComHead = fixture.debugElement.queryAll(By.css('h2'));
    expect(subComHead.length).toEqual(0);
  });
});
