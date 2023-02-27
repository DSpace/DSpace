import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ConfigurationDataService } from '../../core/data/configuration-data.service';
import { RemoteData } from '../../core/data/remote-data';
import { GroupDataService } from '../../core/eperson/group-data.service';
import { PaginationService } from '../../core/pagination/pagination.service';
import { LinkHeadService } from '../../core/services/link-head.service';
import { Collection } from '../../core/shared/collection.model';
import { ConfigurationProperty } from '../../core/shared/configuration-property.model';
import { SearchConfigurationService } from '../../core/shared/search/search-configuration.service';
import { PaginationComponentOptions } from '../pagination/pagination-component-options.model';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../remote-data.utils';
import { PaginationServiceStub } from '../testing/pagination-service.stub';
import { createPaginatedList } from '../testing/utils.test';
import { RSSComponent } from './rss.component';
import { of as observableOf } from 'rxjs';
import { SearchConfigurationServiceStub } from '../testing/search-configuration-service.stub';
import { PaginatedSearchOptions } from '../search/models/paginated-search-options.model';
import { Router } from '@angular/router';
import { RouterMock } from '../mocks/router.mock';



describe('RssComponent', () => {
    let comp: RSSComponent;
    let fixture: ComponentFixture<RSSComponent>;
    let uuid: string;
    let query: string;
    let groupDataService: GroupDataService;
    let linkHeadService: LinkHeadService;
    let configurationDataService: ConfigurationDataService;
    let paginationService;

    beforeEach(waitForAsync(() => {
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
        configurationDataService = jasmine.createSpyObj('configurationDataService', {
            findByPropertyName: createSuccessfulRemoteDataObject$(Object.assign(new ConfigurationProperty(), {
              name: 'test',
              values: [
                'org.dspace.ctask.general.ProfileFormats = test'
              ]
            }))
          });
        linkHeadService = jasmine.createSpyObj('linkHeadService', {
            addTag: ''
        });
        const mockCollectionRD: RemoteData<Collection> = createSuccessfulRemoteDataObject(mockCollection);
        const mockSearchOptions = observableOf(new PaginatedSearchOptions({
            pagination: Object.assign(new PaginationComponentOptions(), {
              id: 'search-page-configuration',
              pageSize: 10,
              currentPage: 1
            }),
          }));
        groupDataService = jasmine.createSpyObj('groupsDataService', {
            findListByHref: createSuccessfulRemoteDataObject$(createPaginatedList([])),
            getGroupRegistryRouterLink: '',
            getUUIDFromString: '',
          });
        paginationService = new PaginationServiceStub();
        const searchConfigService = {
            paginatedSearchOptions: mockSearchOptions
          };
        TestBed.configureTestingModule({
          providers: [
            { provide: GroupDataService, useValue: groupDataService },
            { provide: LinkHeadService, useValue: linkHeadService },
            { provide: ConfigurationDataService, useValue: configurationDataService },
            { provide: SearchConfigurationService, useValue: new SearchConfigurationServiceStub() },
            { provide: PaginationService, useValue: paginationService },
            { provide: Router, useValue: new RouterMock() }
          ],
          declarations: [RSSComponent]
        }).compileComponents();
      }));

    beforeEach(() => {
        uuid = '2cfcf65e-0a51-4bcb-8592-b8db7b064790';
        query = 'test';
        fixture = TestBed.createComponent(RSSComponent);
        comp = fixture.componentInstance;
    });

    it('should formulate the correct url given params in url', () => {
        const route = comp.formulateRoute(uuid, 'opensearch/search', query);
        expect(route).toBe('/opensearch/search?format=atom&scope=2cfcf65e-0a51-4bcb-8592-b8db7b064790&query=test');
    });

    it('should skip uuid if its null', () => {
        const route = comp.formulateRoute(null, 'opensearch/search', query);
        expect(route).toBe('/opensearch/search?format=atom&query=test');
    });

    it('should default to query * if none provided', () => {
        const route = comp.formulateRoute(null, 'opensearch/search', null);
        expect(route).toBe('/opensearch/search?format=atom&query=*');
    });
});

