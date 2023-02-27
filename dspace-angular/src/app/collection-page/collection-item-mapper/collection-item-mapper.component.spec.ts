import { CollectionItemMapperComponent } from './collection-item-mapper.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { SearchFormComponent } from '../../shared/search-form/search-form.component';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterStub } from '../../shared/testing/router.stub';
import { SearchServiceStub } from '../../shared/testing/search-service.stub';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { ItemDataService } from '../../core/data/item-data.service';
import { FormsModule } from '@angular/forms';
import { Collection } from '../../core/shared/collection.model';
import { RemoteData } from '../../core/data/remote-data';
import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import { SortDirection, SortOptions } from '../../core/cache/models/sort-options.model';
import { EventEmitter } from '@angular/core';
import { HostWindowService } from '../../shared/host-window.service';
import { HostWindowServiceStub } from '../../shared/testing/host-window-service.stub';
import { By } from '@angular/platform-browser';
import { CollectionDataService } from '../../core/data/collection-data.service';
import { PaginationComponent } from '../../shared/pagination/pagination.component';
import { EnumKeysPipe } from '../../shared/utils/enum-keys-pipe';
import { ItemSelectComponent } from '../../shared/object-select/item-select/item-select.component';
import { ObjectSelectService } from '../../shared/object-select/object-select.service';
import { ObjectSelectServiceStub } from '../../shared/testing/object-select-service.stub';
import { VarDirective } from '../../shared/utils/var.directive';
import { of as observableOf } from 'rxjs';
import { RouteService } from '../../core/services/route.service';
import { ErrorComponent } from '../../shared/error/error.component';
import { LoadingComponent } from '../../shared/loading/loading.component';
import { SearchConfigurationService } from '../../core/shared/search/search-configuration.service';
import { SearchService } from '../../core/shared/search/search.service';
import { PaginatedSearchOptions } from '../../shared/search/models/paginated-search-options.model';
import {
  createFailedRemoteDataObject$,
  createSuccessfulRemoteDataObject,
  createSuccessfulRemoteDataObject$
} from '../../shared/remote-data.utils';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { SEARCH_CONFIG_SERVICE } from '../../my-dspace-page/my-dspace-page.component';
import { SearchConfigurationServiceStub } from '../../shared/testing/search-configuration-service.stub';
import { GroupDataService } from '../../core/eperson/group-data.service';
import { LinkHeadService } from '../../core/services/link-head.service';
import { ConfigurationDataService } from '../../core/data/configuration-data.service';
import { ConfigurationProperty } from '../../core/shared/configuration-property.model';

describe('CollectionItemMapperComponent', () => {
  let comp: CollectionItemMapperComponent;
  let fixture: ComponentFixture<CollectionItemMapperComponent>;

  let route: ActivatedRoute;
  let router: Router;
  let searchConfigService: SearchConfigurationService;
  let searchService: SearchService;
  let notificationsService: NotificationsService;
  let itemDataService: ItemDataService;

  const mockCollection: Collection = Object.assign(new Collection(), {
    id: 'ce41d451-97ed-4a9c-94a1-7de34f16a9f4',
    name: 'test-collection',
    _links: {
      mappedItems: {
        href: 'https://rest.api/collections/ce41d451-97ed-4a9c-94a1-7de34f16a9f4/mappedItems'
      },
      self: {
        href: 'https://rest.api/collections/ce41d451-97ed-4a9c-94a1-7de34f16a9f4'
      }
    }
  });
  const mockCollectionRD: RemoteData<Collection> = createSuccessfulRemoteDataObject(mockCollection);
  const mockSearchOptions = observableOf(new PaginatedSearchOptions({
    pagination: Object.assign(new PaginationComponentOptions(), {
      id: 'search-page-configuration',
      pageSize: 10,
      currentPage: 1
    }),
    sort: new SortOptions('dc.title', SortDirection.ASC),
    scope: mockCollection.id
  }));
  const url = 'http://test.url';
  const urlWithParam = url + '?param=value';
  const routerStub = Object.assign(new RouterStub(), {
    url: urlWithParam,
    navigateByUrl: {},
    navigate: {}
  });
  const searchConfigServiceStub = {
    paginatedSearchOptions: mockSearchOptions
  };
  const emptyList = createSuccessfulRemoteDataObject(createPaginatedList([]));
  const itemDataServiceStub = {
    mapToCollection: () => createSuccessfulRemoteDataObject$({}),
    findListByHref: () => observableOf(emptyList),
  };
  const activatedRouteStub = {
    parent: {
      data: observableOf({
        dso: mockCollectionRD
      })
    },
    snapshot: {
      queryParamMap: new Map([
        ['query', 'test'],
      ])
    }
  };
  const translateServiceStub = {
    get: () => observableOf('test-message of collection ' + mockCollection.name),
    onLangChange: new EventEmitter(),
    onTranslationChange: new EventEmitter(),
    onDefaultLangChange: new EventEmitter()
  };
  const searchServiceStub = Object.assign(new SearchServiceStub(), {
    search: () => observableOf(emptyList),
    /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
    clearDiscoveryRequests: () => {}
    /* eslint-enable no-empty,@typescript-eslint/no-empty-function */
  });
  const collectionDataServiceStub = {
    getMappedItems: () => observableOf(emptyList),
    /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
    clearMappedItemsRequests: () => {}
    /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
  };
  const routeServiceStub = {
    getRouteParameterValue: () => {
      return observableOf('');
    },
    getQueryParameterValue: () => {
      return observableOf('');
    },
    getQueryParamsWithPrefix: () => {
      return observableOf('');
    }
  };
  const fixedFilterServiceStub = {
    getQueryByFilterName: () => {
      return observableOf('');
    }
  };

  const authorizationDataService = jasmine.createSpyObj('authorizationDataService', {
    isAuthorized: observableOf(true)
  });

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
      imports: [CommonModule, FormsModule, RouterTestingModule.withRoutes([]), TranslateModule.forRoot(), NgbModule],
      declarations: [CollectionItemMapperComponent, ItemSelectComponent, SearchFormComponent, PaginationComponent, EnumKeysPipe, VarDirective, ErrorComponent, LoadingComponent],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: Router, useValue: routerStub },
        { provide: SearchConfigurationService, useValue: searchConfigServiceStub },
        { provide: SearchService, useValue: searchServiceStub },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: ItemDataService, useValue: itemDataServiceStub },
        { provide: CollectionDataService, useValue: collectionDataServiceStub },
        { provide: TranslateService, useValue: translateServiceStub },
        { provide: HostWindowService, useValue: new HostWindowServiceStub(0) },
        { provide: ObjectSelectService, useValue: new ObjectSelectServiceStub() },
        { provide: RouteService, useValue: routeServiceStub },
        { provide: AuthorizationDataService, useValue: authorizationDataService },
        { provide: GroupDataService, useValue: groupDataService },
        { provide: LinkHeadService, useValue: linkHeadService },
        { provide: ConfigurationDataService, useValue: configurationDataService },
      ]
    }).overrideComponent(CollectionItemMapperComponent, {
      set: {
        providers: [
          {
            provide: SEARCH_CONFIG_SERVICE,
            useClass: SearchConfigurationServiceStub
          }
        ] }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CollectionItemMapperComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
    route = (comp as any).route;
    router = (comp as any).router;
    searchConfigService = (comp as any).searchConfigService;
    searchService = (comp as any).searchService;
    notificationsService = (comp as any).notificationsService;
    itemDataService = (comp as any).itemDataService;
  });

  it('should display the correct collection name', () => {
    const name: HTMLElement = fixture.debugElement.query(By.css('#collection-name')).nativeElement;
    expect(name.innerHTML).toContain(mockCollection.name);
  });

  describe('mapItems', () => {
    const ids = ['id1', 'id2', 'id3', 'id4'];

    it('should display a success message if at least one mapping was successful', () => {
      comp.mapItems(ids);
      expect(notificationsService.success).toHaveBeenCalled();
      expect(notificationsService.error).not.toHaveBeenCalled();
    });

    it('should display an error message if at least one mapping was unsuccessful', () => {
      spyOn(itemDataService, 'mapToCollection').and.returnValue(createFailedRemoteDataObject$('Not Found', 404));
      comp.mapItems(ids);
      expect(notificationsService.success).not.toHaveBeenCalled();
      expect(notificationsService.error).toHaveBeenCalled();
    });
  });

  describe('tabChange', () => {
    beforeEach(() => {
      spyOn(routerStub, 'navigateByUrl');
      comp.tabChange({});
    });

    it('should navigate to the same page to remove parameters', () => {
      expect(router.navigateByUrl).toHaveBeenCalledWith(url);
    });
  });

  describe('buildQuery', () => {
    const query = 'query';
    const expected = `-location.coll:\"${mockCollection.id}\" AND ${query}`;

    let result;

    beforeEach(() => {
      result = comp.buildQuery(mockCollection.id, query);
    });

    it('should build a solr query to exclude the provided collection', () => {
      expect(result).toEqual(expected);
    });
  });

  describe('onCancel', () => {
    beforeEach(() => {
      spyOn(routerStub, 'navigate');
      comp.onCancel();
    });

    it('should navigate to the collection page', () => {
      expect(router.navigate).toHaveBeenCalledWith(['/collections/', mockCollection.id]);
    });
  });

});
