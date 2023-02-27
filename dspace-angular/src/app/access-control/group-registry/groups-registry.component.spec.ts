import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, inject, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule, By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { Observable, of as observableOf } from 'rxjs';
import { DSpaceObjectDataService } from '../../core/data/dspace-object-data.service';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { buildPaginatedList, PaginatedList } from '../../core/data/paginated-list.model';
import { RemoteData } from '../../core/data/remote-data';
import { RequestService } from '../../core/data/request.service';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import { GroupDataService } from '../../core/eperson/group-data.service';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { Group } from '../../core/eperson/models/group.model';
import { RouteService } from '../../core/services/route.service';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { GroupMock, GroupMock2 } from '../../shared/testing/group-mock';
import { GroupsRegistryComponent } from './groups-registry.component';
import { EPersonMock, EPersonMock2 } from '../../shared/testing/eperson.mock';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { TranslateLoaderMock } from '../../shared/testing/translate-loader.mock';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { routeServiceStub } from '../../shared/testing/route-service.stub';
import { RouterMock } from '../../shared/mocks/router.mock';
import { PaginationService } from '../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../shared/testing/pagination-service.stub';
import { FeatureID } from '../../core/data/feature-authorization/feature-id';
import { NoContent } from '../../core/shared/NoContent.model';

describe('GroupRegistryComponent', () => {
  let component: GroupsRegistryComponent;
  let fixture: ComponentFixture<GroupsRegistryComponent>;
  let ePersonDataServiceStub: any;
  let groupsDataServiceStub: any;
  let dsoDataServiceStub: any;
  let authorizationService: AuthorizationDataService;

  let mockGroups;
  let mockEPeople;
  let paginationService;

  /**
   * Set authorizationService.isAuthorized to return the following values.
   * @param isAdmin whether or not the current user is an admin.
   * @param canManageGroup whether or not the current user can manage all groups.
   */
  const setIsAuthorized = (isAdmin: boolean, canManageGroup: boolean) => {
    (authorizationService as any).isAuthorized.and.callFake((featureId?: FeatureID) => {
      switch (featureId) {
        case FeatureID.AdministratorOf:
          return observableOf(isAdmin);
        case FeatureID.CanManageGroup:
          return observableOf(canManageGroup);
        case FeatureID.CanDelete:
          return observableOf(true);
        default:
          throw new Error(`setIsAuthorized: this fake implementation does not support ${featureId}.`);
      }
    });
  };

  beforeEach(waitForAsync(() => {
    mockGroups = [GroupMock, GroupMock2];
    mockEPeople = [EPersonMock, EPersonMock2];
    ePersonDataServiceStub = {
      findListByHref(href: string): Observable<RemoteData<PaginatedList<EPerson>>> {
        switch (href) {
          case 'https://dspace.4science.it/dspace-spring-rest/api/eperson/groups/testgroupid2/epersons':
            return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
              elementsPerPage: 1,
              totalElements: 0,
              totalPages: 0,
              currentPage: 1
            }), []));
          case 'https://dspace.4science.it/dspace-spring-rest/api/eperson/groups/testgroupid/epersons':
            return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
              elementsPerPage: 1,
              totalElements: 1,
              totalPages: 1,
              currentPage: 1
            }), [EPersonMock]));
          default:
            return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
              elementsPerPage: 1,
              totalElements: 0,
              totalPages: 0,
              currentPage: 1
            }), []));
        }
      }
    };
    groupsDataServiceStub = {
      allGroups: mockGroups,
      findListByHref(href: string): Observable<RemoteData<PaginatedList<Group>>> {
        switch (href) {
          case 'https://dspace.4science.it/dspace-spring-rest/api/eperson/groups/testgroupid2/groups':
            return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
              elementsPerPage: 1,
              totalElements: 0,
              totalPages: 0,
              currentPage: 1
            }), []));
          case 'https://dspace.4science.it/dspace-spring-rest/api/eperson/groups/testgroupid/groups':
            return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
              elementsPerPage: 1,
              totalElements: 1,
              totalPages: 1,
              currentPage: 1
            }), [GroupMock2]));
          default:
            return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
              elementsPerPage: 1,
              totalElements: 0,
              totalPages: 0,
              currentPage: 1
            }), []));
        }
      },
      getGroupEditPageRouterLink(group: Group): string {
        return '/access-control/groups/' + group.id;
      },
      getGroupRegistryRouterLink(): string {
        return '/access-control/groups';
      },
      searchGroups(query: string): Observable<RemoteData<PaginatedList<Group>>> {
        if (query === '') {
          return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
            elementsPerPage: this.allGroups.length,
            totalElements: this.allGroups.length,
            totalPages: 1,
            currentPage: 1
          }), this.allGroups));
        }
        const result = this.allGroups.find((group: Group) => {
          return (group.id.includes(query));
        });
        return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
          elementsPerPage: [result].length,
          totalElements: [result].length,
          totalPages: 1,
          currentPage: 1
        }), [result]));
      },
      delete(objectId: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
        return createSuccessfulRemoteDataObject$({});
      },
    };
    dsoDataServiceStub = {
      findByHref(href: string): Observable<RemoteData<DSpaceObject>> {
        return createSuccessfulRemoteDataObject$(undefined);
      }
    };

    authorizationService = jasmine.createSpyObj('authorizationService', ['isAuthorized']);
    setIsAuthorized(true, true);
    paginationService = new PaginationServiceStub();
    TestBed.configureTestingModule({
      imports: [CommonModule, NgbModule, FormsModule, ReactiveFormsModule, BrowserModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
      ],
      declarations: [GroupsRegistryComponent],
      providers: [GroupsRegistryComponent,
        { provide: EPersonDataService, useValue: ePersonDataServiceStub },
        { provide: GroupDataService, useValue: groupsDataServiceStub },
        { provide: DSpaceObjectDataService, useValue: dsoDataServiceStub },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: RouteService, useValue: routeServiceStub },
        { provide: Router, useValue: new RouterMock() },
        { provide: AuthorizationDataService, useValue: authorizationService },
        { provide: PaginationService, useValue: paginationService },
        { provide: RequestService, useValue: jasmine.createSpyObj('requestService', ['removeByHrefSubstring']) }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupsRegistryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create GroupRegistryComponent', inject([GroupsRegistryComponent], (comp: GroupsRegistryComponent) => {
    expect(comp).toBeDefined();
  }));

  it('should display list of groups', () => {
    const groupIdsFound = fixture.debugElement.queryAll(By.css('#groups tr td:first-child'));
    expect(groupIdsFound.length).toEqual(2);
    mockGroups.map((group: Group) => {
      expect(groupIdsFound.find((foundEl) => {
        return (foundEl.nativeElement.textContent.trim() === group.uuid);
      })).toBeTruthy();
    });
  });

  it('should display community/collection name if present', () => {
    const collectionNamesFound = fixture.debugElement.queryAll(By.css('#groups tr td:nth-child(3)'));
    expect(collectionNamesFound.length).toEqual(2);
    expect(collectionNamesFound[0].nativeElement.textContent).toEqual('');
    expect(collectionNamesFound[1].nativeElement.textContent).toEqual('testgroupid2objectName');
  });

  describe('edit buttons', () => {
    describe('when the user is a general admin', () => {
      beforeEach(fakeAsync(() => {
        // NOTE: setting canManageGroup to false should not matter, since isAdmin takes priority
        setIsAuthorized(true, false);

        // force rerender after setup changes
        component.search({ query: '' });
        tick();
        fixture.detectChanges();
      }));

      it('should be active', () => {
        const editButtonsFound = fixture.debugElement.queryAll(By.css('#groups tr td:nth-child(5) button.btn-edit'));
        expect(editButtonsFound.length).toEqual(2);
        editButtonsFound.forEach((editButtonFound) => {
          expect(editButtonFound.nativeElement.disabled).toBeFalse();
        });
      });

      it('should not check the canManageGroup permissions', () => {
        expect(authorizationService.isAuthorized).not.toHaveBeenCalledWith(
          FeatureID.CanManageGroup, mockGroups[0].self
        );
        expect(authorizationService.isAuthorized).not.toHaveBeenCalledWith(
          FeatureID.CanManageGroup, mockGroups[0].self, undefined // treated differently
        );
        expect(authorizationService.isAuthorized).not.toHaveBeenCalledWith(
          FeatureID.CanManageGroup, mockGroups[1].self
        );
        expect(authorizationService.isAuthorized).not.toHaveBeenCalledWith(
          FeatureID.CanManageGroup, mockGroups[1].self, undefined // treated differently
        );
      });
    });

    describe('when the user can edit the groups', () => {
      beforeEach(fakeAsync(() => {
        setIsAuthorized(false, true);

        // force rerender after setup changes
        component.search({ query: '' });
        tick();
        fixture.detectChanges();
      }));

      it('should be active', () => {
        const editButtonsFound = fixture.debugElement.queryAll(By.css('#groups tr td:nth-child(5) button.btn-edit'));
        expect(editButtonsFound.length).toEqual(2);
        editButtonsFound.forEach((editButtonFound) => {
          expect(editButtonFound.nativeElement.disabled).toBeFalse();
        });
      });
    });

    describe('when the user can not edit the groups', () => {
      beforeEach(fakeAsync(() => {
        setIsAuthorized(false, false);

        // force rerender after setup changes
        component.search({ query: '' });
        tick();
        fixture.detectChanges();
      }));

      it('should not be active', () => {
        const editButtonsFound = fixture.debugElement.queryAll(By.css('#groups tr td:nth-child(5) button.btn-edit'));
        expect(editButtonsFound.length).toEqual(2);
        editButtonsFound.forEach((editButtonFound) => {
          expect(editButtonFound.nativeElement.disabled).toBeTrue();
        });
      });
    });
  });

  describe('search', () => {
    describe('when searching with query', () => {
      let groupIdsFound;
      beforeEach(fakeAsync(() => {
        component.search({ query: GroupMock2.id });
        tick();
        fixture.detectChanges();
        groupIdsFound = fixture.debugElement.queryAll(By.css('#groups tr td:first-child'));
      }));

      it('should display search result', () => {
        expect(groupIdsFound.length).toEqual(1);
        expect(groupIdsFound.find((foundEl) => {
          return (foundEl.nativeElement.textContent.trim() === GroupMock2.uuid);
        })).toBeTruthy();
      });
    });
  });

  describe('delete', () => {
    let deleteButton;

    beforeEach(fakeAsync(() => {
      spyOn(groupsDataServiceStub, 'delete').and.callThrough();

      setIsAuthorized(true, true);

      // force rerender after setup changes
      component.search({ query: '' });
      tick();
      fixture.detectChanges();

      // only mockGroup[0] is deletable, so we should only get one button
      deleteButton = fixture.debugElement.query(By.css('.btn-delete')).nativeElement;
    }));

    it('should call GroupDataService.delete', () => {
      deleteButton.click();
      fixture.detectChanges();

      expect(groupsDataServiceStub.delete).toHaveBeenCalledWith(mockGroups[0].id);
    });
  });
});
