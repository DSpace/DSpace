import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectorRef, Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { TranslateModule } from '@ngx-translate/core';
import { cold, getTestScheduler, hot } from 'jasmine-marbles';

import { Bitstream } from '../../core/shared/bitstream.model';
import { Bundle } from '../../core/shared/bundle.model';
import { Item } from '../../core/shared/item.model';
import { LinkService } from '../../core/cache/builders/link.service';
import { getMockLinkService } from '../mocks/link-service.mock';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../remote-data.utils';
import { createPaginatedList, createTestComponent } from '../testing/utils.test';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import { NotificationsService } from '../notifications/notifications.service';
import { NotificationsServiceStub } from '../testing/notifications-service.stub';
import { ResourcePolicyDataService } from '../../core/resource-policy/resource-policy-data.service';
import { getMockResourcePolicyService } from '../mocks/mock-resource-policy-service';
import { GroupDataService } from '../../core/eperson/group-data.service';
import { RequestService } from '../../core/data/request.service';
import { getMockRequestService } from '../mocks/request.service.mock';
import { RouterStub } from '../testing/router.stub';
import { buildPaginatedList } from '../../core/data/paginated-list.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { ResourcePoliciesComponent } from './resource-policies.component';
import { PolicyType } from '../../core/resource-policy/models/policy-type.model';
import { ActionType } from '../../core/resource-policy/models/action-type.model';
import { EPersonMock } from '../testing/eperson.mock';
import { GroupMock } from '../testing/group-mock';
import { ResourcePolicyEntryComponent } from './entry/resource-policy-entry.component';
import { DSONameService } from '../../core/breadcrumbs/dso-name.service';

describe('ResourcePoliciesComponent test suite', () => {
  let comp: ResourcePoliciesComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<ResourcePoliciesComponent>;
  let de;
  let routerStub: any;
  let scheduler: TestScheduler;
  const notificationsServiceStub = new NotificationsServiceStub();
  const resourcePolicyService: any = getMockResourcePolicyService();
  const linkService: any = getMockLinkService();

  const resourcePolicy: any = {
    id: '1',
    name: null,
    description: null,
    policyType: PolicyType.TYPE_SUBMISSION,
    action: ActionType.READ,
    startDate: null,
    endDate: null,
    type: 'resourcepolicy',
    uuid: 'resource-policy-1',
    _links: {
      eperson: {
        href: 'https://rest.api/rest/api/eperson'
      },
      group: {
        href: 'https://rest.api/rest/api/group'
      },
      self: {
        href: 'https://rest.api/rest/api/resourcepolicies/1'
      },
    },
    eperson: observableOf(createSuccessfulRemoteDataObject({})),
    group: observableOf(createSuccessfulRemoteDataObject(GroupMock))
  };

  const anotherResourcePolicy: any = {
    id: '2',
    name: null,
    description: null,
    policyType: PolicyType.TYPE_SUBMISSION,
    action: ActionType.WRITE,
    startDate: null,
    endDate: null,
    type: 'resourcepolicy',
    uuid: 'resource-policy-2',
    _links: {
      eperson: {
        href: 'https://rest.api/rest/api/eperson'
      },
      group: {
        href: 'https://rest.api/rest/api/group'
      },
      self: {
        href: 'https://rest.api/rest/api/resourcepolicies/1'
      },
    },
    eperson: observableOf(createSuccessfulRemoteDataObject(EPersonMock)),
    group: observableOf(createSuccessfulRemoteDataObject({}))
  };

  const bitstream1 = Object.assign(new Bitstream(), {
    id: 'bitstream1',
    uuid: 'bitstream1'
  });
  const bitstream2 = Object.assign(new Bitstream(), {
    id: 'bitstream2',
    uuid: 'bitstream2'
  });
  const bitstream3 = Object.assign(new Bitstream(), {
    id: 'bitstream3',
    uuid: 'bitstream3'
  });
  const bitstream4 = Object.assign(new Bitstream(), {
    id: 'bitstream4',
    uuid: 'bitstream4'
  });
  const bundle1 = Object.assign(new Bundle(), {
    id: 'bundle1',
    uuid: 'bundle1',
    _links: {
      self: { href: 'bundle1-selflink' }
    },
    bitstreams: createSuccessfulRemoteDataObject$(createPaginatedList([bitstream1, bitstream2]))
  });
  const bundle2 = Object.assign(new Bundle(), {
    id: 'bundle2',
    uuid: 'bundle2',
    _links: {
      self: { href: 'bundle2-selflink' }
    },
    bitstreams: createSuccessfulRemoteDataObject$(createPaginatedList([bitstream3, bitstream4]))
  });

  const item = Object.assign(new Item(), {
    uuid: 'itemUUID',
    id: 'itemUUID',
    _links: {
      self: { href: 'item-selflink' }
    },
    bundles: createSuccessfulRemoteDataObject$(createPaginatedList([bundle1, bundle2]))
  });

  const routeStub = {
    data: observableOf({
      item: createSuccessfulRemoteDataObject(item)
    })
  };

  const epersonService = jasmine.createSpyObj('epersonService', {
    findByHref: jasmine.createSpy('findByHref'),
  });

  const groupService = jasmine.createSpyObj('groupService', {
    findByHref: jasmine.createSpy('findByHref'),
  });

  routerStub = Object.assign(new RouterStub(), {
    url: `url/edit`
  });

  const getInitEntries = () => {
    return [
      Object.assign({}, {
        id: resourcePolicy.id,
        policy: resourcePolicy,
        checked: false
      }),
      Object.assign({}, {
        id: anotherResourcePolicy.id,
        policy: anotherResourcePolicy,
        checked: false
      })
    ];
  };

  const resourcePolicySelectedEntries = [
    {
      id: resourcePolicy.id,
      policy: resourcePolicy,
      checked: true
    },
    {
      id: anotherResourcePolicy.id,
      policy: anotherResourcePolicy,
      checked: false
    }
  ];

  const pageInfo = new PageInfo();
  const array = [resourcePolicy, anotherResourcePolicy];
  const paginatedList = buildPaginatedList(pageInfo, array);
  const paginatedListRD = createSuccessfulRemoteDataObject(paginatedList);

  const dsoNameService = jasmine.createSpyObj('dsoNameMock', {
    getName: 'NAME'
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        TranslateModule.forRoot()
      ],
      declarations: [
        ResourcePoliciesComponent,
        ResourcePolicyEntryComponent,
        TestComponent
      ],
      providers: [
        { provide: LinkService, useValue: linkService },
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: EPersonDataService, useValue: epersonService },
        { provide: GroupDataService, useValue: groupService },
        { provide: NotificationsService, useValue: notificationsServiceStub },
        { provide: ResourcePolicyDataService, useValue: resourcePolicyService },
        { provide: RequestService, useValue: getMockRequestService() },
        { provide: Router, useValue: routerStub },
        { provide: DSONameService, useValue: dsoNameService },
        ChangeDetectorRef,
        ResourcePoliciesComponent
      ], schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();
  }));

  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      resourcePolicyService.searchByResource.and.returnValue(hot('a|', {
        a: paginatedListRD
      }));
      const html = `
        <ds-resource-policies [resourceUUID]="resourceUUID" [resourceType]="resourceType"></ds-resource-policies>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create ResourcePoliciesComponent', inject([ResourcePoliciesComponent], (app: ResourcePoliciesComponent) => {

      expect(app).toBeDefined();

    }));
  });

  describe('', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(ResourcePoliciesComponent);
      comp = fixture.componentInstance;
      compAsAny = fixture.componentInstance;
      linkService.resolveLink.and.callFake((object, link) => object);
    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      de = null;
      fixture.destroy();
    });

    it('should init component properly', () => {
      spyOn(comp, 'initResourcePolicyList');
      fixture.detectChanges();
      expect(compAsAny.isActive).toBeTruthy();
      expect(comp.initResourcePolicyList).toHaveBeenCalled();
    });

    it('should init resource policies list properly', () => {
      const expected = getInitEntries();
      compAsAny.isActive = true;
      resourcePolicyService.searchByResource.and.returnValue(hot('a|', {
        a: paginatedListRD
      }));

      scheduler = getTestScheduler();
      scheduler.schedule(() => comp.initResourcePolicyList());
      scheduler.flush();

      expect(compAsAny.resourcePoliciesEntries$.value).toEqual(expected);
    });
  });

  describe('', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(ResourcePoliciesComponent);
      comp = fixture.componentInstance;
      compAsAny = fixture.componentInstance;
      linkService.resolveLink.and.callFake((object, link) => object);
      compAsAny.isActive = true;
      const initResourcePolicyEntries = getInitEntries();
      compAsAny.resourcePoliciesEntries$.next(initResourcePolicyEntries);
      resourcePolicyService.searchByResource.and.returnValue(observableOf({}));
      spyOn(comp, 'initResourcePolicyList').and.callFake(() => ({}));
      fixture.detectChanges();
    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      de = null;
      fixture.destroy();
    });

    describe('canDelete', () => {
      beforeEach(() => {
        const initResourcePolicyEntries = getInitEntries();
        compAsAny.resourcePoliciesEntries$.next(initResourcePolicyEntries);
        fixture.detectChanges();
      });

      afterEach(() => {
        comp = null;
        compAsAny = null;
        de = null;
        fixture.destroy();
      });

      it('should return false when no row is selected', () => {
        expect(comp.canDelete()).toBeObservable(cold('(a|)', {
          a: false
        }));
      });

      it('should return true when at least one row is selected', () => {
        const checkbox = fixture.debugElement.query(By.css('table > tbody > tr:nth-child(1) input'));
        const event = { target: { checked: true } };
        checkbox.triggerEventHandler('change', event);
        expect(comp.canDelete()).toBeObservable(cold('(a|)', {
          a: true
        }));
      });
    });

    it('should render a table with a row for each policy', () => {
      const rows = fixture.debugElement.queryAll(By.css('table > tbody > tr'));
      expect(rows.length).toBe(2);
    });

    describe('deleteSelectedResourcePolicies', () => {
      beforeEach(() => {
        compAsAny.resourcePoliciesEntries$.next(resourcePolicySelectedEntries);
        fixture.detectChanges();
      });

      it('should call ResourcePolicyService.delete for the checked policies', () => {
        resourcePolicyService.delete.and.returnValue(observableOf(true));
        scheduler = getTestScheduler();
        scheduler.schedule(() => comp.deleteSelectedResourcePolicies());
        scheduler.flush();

        // only the first one is checked
        expect(resourcePolicyService.delete).toHaveBeenCalledWith(resourcePolicy.id);
      });

      it('should notify success when delete is successful', () => {

        resourcePolicyService.delete.and.returnValue(observableOf(true));
        scheduler = getTestScheduler();
        scheduler.schedule(() => comp.deleteSelectedResourcePolicies());
        scheduler.flush();

        expect(notificationsServiceStub.success).toHaveBeenCalled();
        expect(comp.initResourcePolicyList).toHaveBeenCalled();
      });

      it('should notify error when delete is not successful', () => {

        resourcePolicyService.delete.and.returnValue(observableOf(false));
        scheduler = getTestScheduler();
        scheduler.schedule(() => comp.deleteSelectedResourcePolicies());
        scheduler.flush();

        expect(notificationsServiceStub.error).toHaveBeenCalled();
        expect(comp.initResourcePolicyList).toHaveBeenCalled();
      });
    });

    it('should get the resource\'s policy list', () => {
      const initResourcePolicyEntries = getInitEntries();
      expect(comp.getResourcePolicies()).toBeObservable(cold('a', {
        a: initResourcePolicyEntries
      }));

    });

    it('should select All Checkbox', () => {
      spyOn(comp, 'selectAllCheckbox').and.callThrough();
      const checkbox = fixture.debugElement.query(By.css('table > thead > tr:nth-child(2) input'));

      const event = { target: { checked: true } };
      checkbox.triggerEventHandler('change', event);
      expect(comp.selectAllCheckbox).toHaveBeenCalled();
    });

    it('should select a Checkbox', () => {
      spyOn(comp, 'selectCheckbox').and.callThrough();
      const checkbox = fixture.debugElement.query(By.css('table > tbody > tr:nth-child(1) input'));

      const event = { target: { checked: true } };
      checkbox.triggerEventHandler('change', event);
      expect(comp.selectCheckbox).toHaveBeenCalled();
    });

    it('should redirect to create resource policy page', () => {

      comp.redirectToResourcePolicyCreatePage();
      expect(compAsAny.router.navigate).toHaveBeenCalled();
    });
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

  resourceUUID = 'itemUUID';
  resourceType = 'item';
}
