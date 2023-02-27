import { Router } from '@angular/router';
import { Observable, of as observableOf } from 'rxjs';
import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule, By } from '@angular/platform-browser';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { buildPaginatedList, PaginatedList } from '../../core/data/paginated-list.model';
import { RemoteData } from '../../core/data/remote-data';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { FormBuilderService } from '../../shared/form/builder/form-builder.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { EPeopleRegistryComponent } from './epeople-registry.component';
import { EPersonMock, EPersonMock2 } from '../../shared/testing/eperson.mock';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { getMockFormBuilderService } from '../../shared/mocks/form-builder-service.mock';
import { getMockTranslateService } from '../../shared/mocks/translate.service.mock';
import { TranslateLoaderMock } from '../../shared/mocks/translate-loader.mock';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { RouterStub } from '../../shared/testing/router.stub';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { RequestService } from '../../core/data/request.service';
import { PaginationService } from '../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../shared/testing/pagination-service.stub';
import { FindListOptions } from '../../core/data/find-list-options.model';

describe('EPeopleRegistryComponent', () => {
  let component: EPeopleRegistryComponent;
  let fixture: ComponentFixture<EPeopleRegistryComponent>;
  let translateService: TranslateService;
  let builderService: FormBuilderService;

  let mockEPeople;
  let ePersonDataServiceStub: any;
  let authorizationService: AuthorizationDataService;
  let modalService;

  let paginationService;

  beforeEach(waitForAsync(() => {
    mockEPeople = [EPersonMock, EPersonMock2];
    ePersonDataServiceStub = {
      activeEPerson: null,
      allEpeople: mockEPeople,
      getEPeople(): Observable<RemoteData<PaginatedList<EPerson>>> {
        return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
          elementsPerPage: this.allEpeople.length,
          totalElements: this.allEpeople.length,
          totalPages: 1,
          currentPage: 1
        }), this.allEpeople));
      },
      getActiveEPerson(): Observable<EPerson> {
        return observableOf(this.activeEPerson);
      },
      searchByScope(scope: string, query: string, options: FindListOptions = {}): Observable<RemoteData<PaginatedList<EPerson>>> {
        if (scope === 'email') {
          const result = this.allEpeople.find((ePerson: EPerson) => {
            return ePerson.email === query;
          });
          return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
            elementsPerPage: [result].length,
            totalElements: [result].length,
            totalPages: 1,
            currentPage: 1
          }), [result]));
        }
        if (scope === 'metadata') {
          if (query === '') {
            return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
              elementsPerPage: this.allEpeople.length,
              totalElements: this.allEpeople.length,
              totalPages: 1,
              currentPage: 1
            }), this.allEpeople));
          }
          const result = this.allEpeople.find((ePerson: EPerson) => {
            return (ePerson.name.includes(query) || ePerson.email.includes(query));
          });
          return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
            elementsPerPage: [result].length,
            totalElements: [result].length,
            totalPages: 1,
            currentPage: 1
          }), [result]));
        }
        return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo({
          elementsPerPage: this.allEpeople.length,
          totalElements: this.allEpeople.length,
          totalPages: 1,
          currentPage: 1
        }), this.allEpeople));
      },
      deleteEPerson(ePerson: EPerson): Observable<boolean> {
        this.allEpeople = this.allEpeople.filter((ePerson2: EPerson) => {
          return (ePerson2.uuid !== ePerson.uuid);
        });
        return observableOf(true);
      },
      editEPerson(ePerson: EPerson) {
        this.activeEPerson = ePerson;
      },
      cancelEditEPerson() {
        this.activeEPerson = null;
      },
      clearEPersonRequests(): void {
        // empty
      },
      getEPeoplePageRouterLink(): string {
        return '/access-control/epeople';
      }
    };
    authorizationService = jasmine.createSpyObj('authorizationService', {
      isAuthorized: observableOf(true)
    });
    builderService = getMockFormBuilderService();
    translateService = getMockTranslateService();

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
      declarations: [EPeopleRegistryComponent],
      providers: [
        { provide: EPersonDataService, useValue: ePersonDataServiceStub },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: AuthorizationDataService, useValue: authorizationService },
        { provide: FormBuilderService, useValue: builderService },
        { provide: Router, useValue: new RouterStub() },
        { provide: RequestService, useValue: jasmine.createSpyObj('requestService', ['removeByHrefSubstring']) },
        { provide: PaginationService, useValue: paginationService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EPeopleRegistryComponent);
    component = fixture.componentInstance;
    modalService = (component as any).modalService;
    spyOn(modalService, 'open').and.returnValue(Object.assign({ componentInstance: Object.assign({ response: observableOf(true) }) }));
    fixture.detectChanges();
  });

  it('should create EPeopleRegistryComponent', () => {
    expect(component).toBeDefined();
  });

  it('should display list of ePeople', () => {
    const ePeopleIdsFound = fixture.debugElement.queryAll(By.css('#epeople tr td:first-child'));
    expect(ePeopleIdsFound.length).toEqual(2);
    mockEPeople.map((ePerson: EPerson) => {
      expect(ePeopleIdsFound.find((foundEl) => {
        return (foundEl.nativeElement.textContent.trim() === ePerson.uuid);
      })).toBeTruthy();
    });
  });

  describe('search', () => {
    describe('when searching with scope/query (scope metadata)', () => {
      let ePeopleIdsFound;
      beforeEach(fakeAsync(() => {
        component.search({ scope: 'metadata', query: EPersonMock2.name });
        tick();
        fixture.detectChanges();
        ePeopleIdsFound = fixture.debugElement.queryAll(By.css('#epeople tr td:first-child'));
      }));

      it('should display search result', () => {
        expect(ePeopleIdsFound.length).toEqual(1);
        expect(ePeopleIdsFound.find((foundEl) => {
          return (foundEl.nativeElement.textContent.trim() === EPersonMock2.uuid);
        })).toBeTruthy();
      });
    });

    describe('when searching with scope/query (scope email)', () => {
      let ePeopleIdsFound;
      beforeEach(fakeAsync(() => {
        component.search({ scope: 'email', query: EPersonMock.email });
        tick();
        fixture.detectChanges();
        ePeopleIdsFound = fixture.debugElement.queryAll(By.css('#epeople tr td:first-child'));
      }));

      it('should display search result', () => {
        expect(ePeopleIdsFound.length).toEqual(1);
        expect(ePeopleIdsFound.find((foundEl) => {
          return (foundEl.nativeElement.textContent.trim() === EPersonMock.uuid);
        })).toBeTruthy();
      });
    });
  });

  describe('toggleEditEPerson', () => {
    describe('when you click on first edit eperson button', () => {
      beforeEach(fakeAsync(() => {
        const editButtons = fixture.debugElement.queryAll(By.css('.access-control-editEPersonButton'));
        editButtons[0].triggerEventHandler('click', {
          preventDefault: () => {/**/
          }
        });
        tick();
        fixture.detectChanges();
      }));

      it('editEPerson form is toggled', () => {
        const ePeopleIds = fixture.debugElement.queryAll(By.css('#epeople tr td:first-child'));
        ePersonDataServiceStub.getActiveEPerson().subscribe((activeEPerson: EPerson) => {
          if (ePeopleIds[0] && activeEPerson === ePeopleIds[0].nativeElement.textContent) {
            expect(component.isEPersonFormShown).toEqual(false);
          } else {
            expect(component.isEPersonFormShown).toEqual(true);
          }

        });
      });

      it('EPerson search section is hidden', () => {
        expect(fixture.debugElement.query(By.css('#search'))).toBeNull();
      });
    });
  });

  describe('deleteEPerson', () => {
    describe('when you click on first delete eperson button', () => {
      let ePeopleIdsFoundBeforeDelete;
      let ePeopleIdsFoundAfterDelete;
      beforeEach(fakeAsync(() => {
        ePeopleIdsFoundBeforeDelete = fixture.debugElement.queryAll(By.css('#epeople tr td:first-child'));
        const deleteButtons = fixture.debugElement.queryAll(By.css('.access-control-deleteEPersonButton'));
        deleteButtons[0].triggerEventHandler('click', {
          preventDefault: () => {/**/
          }
        });
        tick();
        fixture.detectChanges();
        ePeopleIdsFoundAfterDelete = fixture.debugElement.queryAll(By.css('#epeople tr td:first-child'));
      }));

      it('first ePerson is deleted', () => {
        expect(ePeopleIdsFoundBeforeDelete.length === ePeopleIdsFoundAfterDelete + 1);
        ePeopleIdsFoundAfterDelete.forEach((epersonElement) => {
          expect(epersonElement !== ePeopleIdsFoundBeforeDelete[0].nativeElement.textContent).toBeTrue();
        });
      });
    });
  });

  describe('delete EPerson button when the isAuthorized returns false', () => {
    let ePeopleDeleteButton;
    beforeEach(() => {
      authorizationService = jasmine.createSpyObj('authorizationService', {
        isAuthorized: observableOf(false)
      });
    });

    it('should be disabled', () => {
      ePeopleDeleteButton = fixture.debugElement.queryAll(By.css('#epeople tr td div button.delete-button'));
      ePeopleDeleteButton.forEach((deleteButton) => {
        expect(deleteButton.nativeElement.disabled).toBe(true);
      });

    });
  });
});
