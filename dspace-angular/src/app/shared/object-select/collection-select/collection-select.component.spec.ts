import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { PaginationComponentOptions } from '../../pagination/pagination-component-options.model';
import { TranslateModule } from '@ngx-translate/core';
import { SharedModule } from '../../shared.module';
import { ObjectSelectServiceStub } from '../../testing/object-select-service.stub';
import { ObjectSelectService } from '../object-select.service';
import { HostWindowService } from '../../host-window.service';
import { HostWindowServiceStub } from '../../testing/host-window-service.stub';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { CollectionSelectComponent } from './collection-select.component';
import { Collection } from '../../../core/shared/collection.model';
import { createSuccessfulRemoteDataObject$ } from '../../remote-data.utils';
import { createPaginatedList } from '../../testing/utils.test';
import { PaginationService } from '../../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../testing/pagination-service.stub';
import { of as observableOf } from 'rxjs';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';
import { LinkHeadService } from '../../../core/services/link-head.service';
import { GroupDataService } from '../../../core/eperson/group-data.service';
import { ConfigurationDataService } from '../../../core/data/configuration-data.service';
import { SearchConfigurationService } from '../../../core/shared/search/search-configuration.service';
import { SearchConfigurationServiceStub } from '../../testing/search-configuration-service.stub';
import { ConfigurationProperty } from '../../../core/shared/configuration-property.model';

describe('CollectionSelectComponent', () => {
  let comp: CollectionSelectComponent;
  let fixture: ComponentFixture<CollectionSelectComponent>;
  let objectSelectService: ObjectSelectService;

  const mockCollectionList = [
    Object.assign(new Collection(), {
      id: 'id1',
      name: 'name1'
    }),
    Object.assign(new Collection(), {
      id: 'id2',
      name: 'name2'
    })
  ];
  const mockCollections = createSuccessfulRemoteDataObject$(createPaginatedList(mockCollectionList));
  const mockPaginationOptions = Object.assign(new PaginationComponentOptions(), {
    id: 'search-page-configuration',
    pageSize: 10,
    currentPage: 1
  });

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

  const paginationService = new PaginationServiceStub();
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), SharedModule, RouterTestingModule.withRoutes([])],
      declarations: [],
      providers: [
        { provide: ObjectSelectService, useValue: new ObjectSelectServiceStub([mockCollectionList[1].id]) },
        { provide: HostWindowService, useValue: new HostWindowServiceStub(0) },
        { provide: PaginationService, useValue: paginationService },
        { provide: AuthorizationDataService, useValue: authorizationDataService },
        { provide: GroupDataService, useValue: groupDataService },
        { provide: LinkHeadService, useValue: linkHeadService },
        { provide: ConfigurationDataService, useValue: configurationDataService },
        { provide: SearchConfigurationService, useValue: new SearchConfigurationServiceStub() },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CollectionSelectComponent);
    comp = fixture.componentInstance;
    comp.dsoRD$ = mockCollections;
    comp.paginationOptions = mockPaginationOptions;
    fixture.detectChanges();
    objectSelectService = (comp as any).objectSelectService;
  });

  it(`should show a list of ${mockCollectionList.length} collections`, () => {
    const tbody: HTMLElement = fixture.debugElement.query(By.css('table#collection-select tbody')).nativeElement;
    expect(tbody.children.length).toBe(mockCollectionList.length);
  });

  describe('checkboxes', () => {
    let checkbox: HTMLInputElement;

    beforeEach(() => {
      checkbox = fixture.debugElement.query(By.css('input.collection-checkbox')).nativeElement;
    });

    it('should initially be unchecked', () => {
      expect(checkbox.checked).toBeFalsy();
    });

    it('should be checked when clicked', () => {
      checkbox.click();
      fixture.detectChanges();
      expect(checkbox.checked).toBeTruthy();
    });

    it('should switch the value through object-select-service', () => {
      spyOn((comp as any).objectSelectService, 'switch').and.callThrough();
      checkbox.click();
      expect((comp as any).objectSelectService.switch).toHaveBeenCalled();
    });
  });

  describe('when confirm is clicked', () => {
    let confirmButton: HTMLButtonElement;

    beforeEach(() => {
      confirmButton = fixture.debugElement.query(By.css('button.collection-confirm')).nativeElement;
      spyOn(comp.confirm, 'emit').and.callThrough();
    });

    it('should emit the selected collections', () => {
      confirmButton.click();
      expect(comp.confirm.emit).toHaveBeenCalled();
    });
  });

  describe('when cancel is clicked', () => {
    let cancelButton: HTMLButtonElement;

    beforeEach(() => {
      cancelButton = fixture.debugElement.query(By.css('button.collection-cancel')).nativeElement;
      spyOn(comp.cancel, 'emit').and.callThrough();
    });

    it('should emit a cancel event', () => {
      cancelButton.click();
      expect(comp.cancel.emit).toHaveBeenCalled();
    });
  });
});
