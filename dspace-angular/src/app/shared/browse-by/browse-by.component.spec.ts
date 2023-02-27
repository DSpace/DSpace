import { BrowseByComponent } from './browse-by.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { of as observableOf } from 'rxjs';
import { CommonModule } from '@angular/common';
import { Item } from '../../core/shared/item.model';
import { buildPaginatedList } from '../../core/data/paginated-list.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateLoaderMock } from '../mocks/translate-loader.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PaginationComponentOptions } from '../pagination/pagination-component-options.model';
import { SortDirection, SortOptions } from '../../core/cache/models/sort-options.model';
import { createSuccessfulRemoteDataObject$ } from '../remote-data.utils';
import { PaginationService } from '../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../testing/pagination-service.stub';
import {
  ListableObjectComponentLoaderComponent
} from '../object-collection/shared/listable-object/listable-object-component-loader.component';
import { ViewMode } from '../../core/shared/view-mode.model';
import {
  BrowseEntryListElementComponent
} from '../object-list/browse-entry-list-element/browse-entry-list-element.component';
import {
  DEFAULT_CONTEXT,
  listableObjectComponent,
} from '../object-collection/shared/listable-object/listable-object.decorator';
import { BrowseEntry } from '../../core/shared/browse-entry.model';
import { ITEM } from '../../core/shared/item.resource-type';
import { ThemeService } from '../theme-support/theme.service';
import { SelectableListService } from '../object-list/selectable-list/selectable-list.service';
import { HostWindowServiceStub } from '../testing/host-window-service.stub';
import { HostWindowService } from '../host-window.service';
import { RouteService } from '../../core/services/route.service';
import { routeServiceStub } from '../testing/route-service.stub';
import { GroupDataService } from '../../core/eperson/group-data.service';
import { createPaginatedList } from '../testing/utils.test';
import { LinkHeadService } from '../../core/services/link-head.service';
import { ConfigurationDataService } from '../../core/data/configuration-data.service';
import { ConfigurationProperty } from '../../core/shared/configuration-property.model';
import { SearchConfigurationServiceStub } from '../testing/search-configuration-service.stub';
import { SearchConfigurationService } from '../../core/shared/search/search-configuration.service';
import { getMockThemeService } from '../mocks/theme-service.mock';
import { SharedModule } from '../shared.module';
import { BrowseByRoutingModule } from '../../browse-by/browse-by-routing.module';
import { AccessControlRoutingModule } from '../../access-control/access-control-routing.module';

@listableObjectComponent(BrowseEntry, ViewMode.ListElement, DEFAULT_CONTEXT, 'custom')
@Component({
  selector: 'ds-browse-entry-list-element',
  template: ''
})
class MockThemedBrowseEntryListElementComponent {
}

describe('BrowseByComponent', () => {
  let comp: BrowseByComponent;
  let fixture: ComponentFixture<BrowseByComponent>;

  const mockItems = [
    Object.assign(new Item(), {
      id: 'fakeId-1',
      metadata: [
        {
          key: 'dc.title',
          value: 'First Fake Title'
        }
      ]
    }),
    Object.assign(new Item(), {
      id: 'fakeId-2',
      metadata: [
        {
          key: 'dc.title',
          value: 'Second Fake Title'
        }
      ]
    })
  ];
  const mockItemsRD$ = createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), mockItems));

  const groupDataService = jasmine.createSpyObj('groupsDataService', {
    findListByHref: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    getGroupRegistryRouterLink: '',
    getUUIDFromString: '',
  });

  const linkHeadService = jasmine.createSpyObj('linkHeadService', {
    addTag: ''
  });

  const configurationDataService = jasmine.createSpyObj('configurationDataService', {
    findByPropertyName: createSuccessfulRemoteDataObject$(Object.assign(new ConfigurationProperty(), {
      name: 'test',
      values: [
        'org.dspace.ctask.general.ProfileFormats = test'
      ]
    }))
  });

  const paginationConfig = Object.assign(new PaginationComponentOptions(), {
    id: 'test-pagination',
    currentPage: 1,
    pageSizeOptions: [5, 10, 15, 20],
    pageSize: 15
  });
  const paginationService = new PaginationServiceStub(paginationConfig);

  let themeService;

  beforeEach(waitForAsync(() => {
    themeService = getMockThemeService('dspace');
    TestBed.configureTestingModule({
      imports: [
        BrowseByRoutingModule,
        AccessControlRoutingModule,
        CommonModule,
        SharedModule,
        NgbModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
        RouterTestingModule,
        NoopAnimationsModule
      ],
      declarations: [BrowseByComponent],
      providers: [
        { provide: SearchConfigurationService, useValue: new SearchConfigurationServiceStub() },
        { provide: ConfigurationDataService, useValue: configurationDataService },
        { provide: LinkHeadService, useValue: linkHeadService },
        { provide: GroupDataService, useValue: groupDataService },
        { provide: PaginationService, useValue: paginationService },
        { provide: MockThemedBrowseEntryListElementComponent },
        { provide: ThemeService, useValue: themeService },
        { provide: RouteService, useValue: routeServiceStub},
        { provide: SelectableListService, useValue: {} },
        { provide: HostWindowService, useValue: new HostWindowServiceStub(800) },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(BrowseByComponent);
    comp = fixture.componentInstance;
    comp.paginationConfig = paginationConfig;
    fixture.detectChanges();
  }));

  it('should display a loading message when objects is empty', () => {
    (comp as any).objects = undefined;
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('ds-themed-loading'))).toBeDefined();
  });

  it('should display results when objects is not empty', () => {
    (comp as any).objects = observableOf({
      payload: {
        page: {
          length: 1
        }
      }
    });
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('ds-viewable-collection'))).toBeDefined();
  });

  describe('when showPaginator is true and browseEntries are provided', () => {
    let browseEntries;

    beforeEach(() => {
      browseEntries = [
        Object.assign(new BrowseEntry(), {
          type: ITEM,
          authority: 'authority key 1',
          value: 'browse entry 1',
          language: null,
          count: 1,
        }),
        Object.assign(new BrowseEntry(), {
          type: ITEM,
          authority: null,
          value: 'browse entry 2',
          language: null,
          count: 4,
        }),
      ];

      comp.showPaginator = true;
      comp.objects$ = createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), browseEntries));
      comp.paginationConfig = paginationConfig;
      comp.sortConfig = Object.assign(new SortOptions('dc.title', SortDirection.ASC));
      // NOTE: do NOT trigger change detection until the theme is set, such that the theme can be picked up as well
    });

    describe('when theme is base', () => {
      beforeEach(() => {
        themeService.getThemeName.and.returnValue('base');
        themeService.getThemeName$.and.returnValue(observableOf('base'));
        fixture.detectChanges();
      });

      it('should use the base component to render browse entries', () => {
        waitForAsync(() => {
          const componentLoaders = fixture.debugElement.queryAll(By.directive(ListableObjectComponentLoaderComponent));
          expect(componentLoaders.length).toEqual(browseEntries.length);
          componentLoaders.forEach((componentLoader) => {
            const browseEntry = componentLoader.query(By.css('ds-browse-entry-list-element'));
            expect(browseEntry.componentInstance).toBeInstanceOf(BrowseEntryListElementComponent);
          });
        });
      });
    });

    describe('when theme is custom', () => {
      beforeEach(() => {
        themeService.getThemeName.and.returnValue('custom');
        themeService.getThemeName$.and.returnValue(observableOf('custom'));
        fixture.detectChanges();
      });

      it('should use the themed component to render browse entries', () => {
        waitForAsync(() => {
          const componentLoaders = fixture.debugElement.queryAll(By.directive(ListableObjectComponentLoaderComponent));
          expect(componentLoaders.length).toEqual(browseEntries.length);
          componentLoaders.forEach((componentLoader) => {
            const browseEntry = componentLoader.query(By.css('ds-browse-entry-list-element'));
            expect(browseEntry.componentInstance).toBeInstanceOf(MockThemedBrowseEntryListElementComponent);
          });
        });
      });
    });
  });

  describe('reset filters button', () => {
    it('should not be present when no startsWith or value is present ', () => {
      const button = fixture.debugElement.query(By.css('reset'));
      expect(button).toBeNull();
    });
    it('should be present when a startsWith or value is present ', () => {
      comp.shouldDisplayResetButton$ = observableOf(true);
      fixture.detectChanges();

      const button = fixture.debugElement.query(By.css('reset'));
      expect(button).toBeDefined();
    });
  });

});
