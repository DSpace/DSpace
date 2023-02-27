import { CommonModule } from '@angular/common';
import { EventEmitter } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { SortDirection, SortOptions } from '../../../core/cache/models/sort-options.model';
import { CollectionDataService } from '../../../core/data/collection-data.service';
import { ItemDataService } from '../../../core/data/item-data.service';
import { RemoteData } from '../../../core/data/remote-data';
import { Collection } from '../../../core/shared/collection.model';
import { Item } from '../../../core/shared/item.model';
import { SearchConfigurationService } from '../../../core/shared/search/search-configuration.service';
import { SearchService } from '../../../core/shared/search/search.service';
import { ErrorComponent } from '../../../shared/error/error.component';
import { HostWindowService } from '../../../shared/host-window.service';
import { LoadingComponent } from '../../../shared/loading/loading.component';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { CollectionSelectComponent } from '../../../shared/object-select/collection-select/collection-select.component';
import { ObjectSelectService } from '../../../shared/object-select/object-select.service';
import { PaginationComponentOptions } from '../../../shared/pagination/pagination-component-options.model';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { SearchFormComponent } from '../../../shared/search-form/search-form.component';
import { PaginatedSearchOptions } from '../../../shared/search/models/paginated-search-options.model';
import { HostWindowServiceStub } from '../../../shared/testing/host-window-service.stub';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { ObjectSelectServiceStub } from '../../../shared/testing/object-select-service.stub';
import { RouterStub } from '../../../shared/testing/router.stub';
import { SearchServiceStub } from '../../../shared/testing/search-service.stub';
import { EnumKeysPipe } from '../../../shared/utils/enum-keys-pipe';
import { VarDirective } from '../../../shared/utils/var.directive';
import { ItemCollectionMapperComponent } from './item-collection-mapper.component';
import {
  createFailedRemoteDataObject$,
  createSuccessfulRemoteDataObject,
  createSuccessfulRemoteDataObject$
} from '../../../shared/remote-data.utils';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';

describe('ItemCollectionMapperComponent', () => {
  let comp: ItemCollectionMapperComponent;
  let fixture: ComponentFixture<ItemCollectionMapperComponent>;

  let route: ActivatedRoute;
  let router: Router;
  let searchConfigService: SearchConfigurationService;
  let searchService: SearchService;
  let notificationsService: NotificationsService;
  let itemDataService: ItemDataService;

  const mockCollection = Object.assign(new Collection(), { id: 'collection1' });
  const mockItem: Item = Object.assign(new Item(), {
    id: '932c7d50-d85a-44cb-b9dc-b427b12877bd',
    uuid: '932c7d50-d85a-44cb-b9dc-b427b12877bd',
    name: 'test-item'
  });
  const mockItemRD: RemoteData<Item> = createSuccessfulRemoteDataObject(mockItem);
  const mockSearchOptions = observableOf(new PaginatedSearchOptions({
    pagination: Object.assign(new PaginationComponentOptions(), {
      id: 'search-page-configuration',
      pageSize: 10,
      currentPage: 1
    }),
    sort: new SortOptions('dc.title', SortDirection.ASC)
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
  const mockCollectionsRD = createSuccessfulRemoteDataObject(createPaginatedList([]));
  const itemDataServiceStub = {
    mapToCollection: () => createSuccessfulRemoteDataObject$({}),
    removeMappingFromCollection: () => createSuccessfulRemoteDataObject$({}),
    getMappedCollectionsEndpoint: () => observableOf('rest/api/mappedCollectionsEndpoint'),
    getMappedCollections: () => observableOf(mockCollectionsRD),
    /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
    clearMappedCollectionsRequests: () => {}
    /* eslint-enable no-empty,@typescript-eslint/no-empty-function */
  };
  const collectionDataServiceStub = {
    findListByHref: () => observableOf(mockCollectionsRD),
  };
  const searchServiceStub = Object.assign(new SearchServiceStub(), {
    search: () => observableOf(mockCollectionsRD),
    /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
    clearDiscoveryRequests: () => {}
    /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
  });
  const activatedRouteStub = {
    parent: {
      data: observableOf({
        dso: mockItemRD
      })
    }
  };
  const translateServiceStub = {
    get: () => observableOf('test-message of item ' + mockItem.name),
    onLangChange: new EventEmitter(),
    onTranslationChange: new EventEmitter(),
    onDefaultLangChange: new EventEmitter()
  };

  const authorizationDataService = jasmine.createSpyObj('authorizationDataService', {
    isAuthorized: observableOf(true)
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule, RouterTestingModule.withRoutes([]), TranslateModule.forRoot(), NgbModule],
      declarations: [ItemCollectionMapperComponent, CollectionSelectComponent, SearchFormComponent, PaginationComponent, EnumKeysPipe, VarDirective, ErrorComponent, LoadingComponent],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: Router, useValue: routerStub },
        { provide: SearchConfigurationService, useValue: searchConfigServiceStub },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: ItemDataService, useValue: itemDataServiceStub },
        { provide: SearchService, useValue: searchServiceStub },
        { provide: ObjectSelectService, useValue: new ObjectSelectServiceStub() },
        { provide: TranslateService, useValue: translateServiceStub },
        { provide: HostWindowService, useValue: new HostWindowServiceStub(0) },
        { provide: CollectionDataService, useValue: collectionDataServiceStub },
        { provide: AuthorizationDataService, useValue: authorizationDataService }
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemCollectionMapperComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
    route = (comp as any).route;
    router = (comp as any).router;
    searchConfigService = (comp as any).searchConfigService;
    notificationsService = (comp as any).notificationsService;
    itemDataService = (comp as any).itemDataService;
    searchService = (comp as any).searchService;
  });

  it('should display the correct collection name', () => {
    const name: HTMLElement = fixture.debugElement.query(By.css('#item-name')).nativeElement;
    expect(name.innerHTML).toContain(mockItem.name);
  });

  describe('mapCollections', () => {
    const ids = ['id1', 'id2', 'id3', 'id4'];

    it('should display a success message if at least one mapping was successful', () => {
      comp.mapCollections(ids);
      expect(notificationsService.success).toHaveBeenCalled();
      expect(notificationsService.error).not.toHaveBeenCalled();
    });

    it('should display an error message if at least one mapping was unsuccessful', () => {
      spyOn(itemDataService, 'mapToCollection').and.returnValue(createFailedRemoteDataObject$('Not Found', 404));
      comp.mapCollections(ids);
      expect(notificationsService.success).not.toHaveBeenCalled();
      expect(notificationsService.error).toHaveBeenCalled();
    });
  });

  describe('removeMappings', () => {
    const ids = ['id1', 'id2', 'id3', 'id4'];

    it('should display a success message if the removal of at least one mapping was successful', () => {
      comp.removeMappings(ids);
      expect(notificationsService.success).toHaveBeenCalled();
      expect(notificationsService.error).not.toHaveBeenCalled();
    });

    it('should display an error message if the removal of at least one mapping was unsuccessful', () => {
      spyOn(itemDataService, 'removeMappingFromCollection').and.returnValue(createFailedRemoteDataObject$('Not Found', 404));
      comp.removeMappings(ids);
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
    const expected = `${query} AND -search.resourceid:${mockCollection.id}`;

    let result;

    beforeEach(() => {
      result = comp.buildQuery([mockCollection], query);
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

    it('should navigate to the item page', () => {
      expect(router.navigate).toHaveBeenCalledWith(['/items/' + mockItem.uuid]);
    });
  });

});
