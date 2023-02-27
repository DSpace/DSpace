/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { of as observableOf } from 'rxjs';
import { createSuccessfulRemoteDataObject } from '../../remote-data.utils';
import { GroupMock } from '../../testing/group-mock';
import { PolicyType } from '../../../core/resource-policy/models/policy-type.model';
import { ActionType } from '../../../core/resource-policy/models/action-type.model';
import { EPersonMock } from '../../testing/eperson.mock';
import { ResourcePolicyEntryComponent } from './resource-policy-entry.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GroupDataService } from '../../../core/eperson/group-data.service';
import { DSONameService } from '../../../core/breadcrumbs/dso-name.service';
import { RouterStub } from '../../testing/router.stub';
import { Item } from '../../../core/shared/item.model';
import { cold } from 'jasmine-marbles';
import { By } from '@angular/platform-browser';
import createSpyObj = jasmine.createSpyObj;

const groupRP: any = {
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
      href: 'https://rest.api/rest/api/resourcepolicies/1/eperson'
    },
    group: {
      href: 'https://rest.api/rest/api/resourcepolicies/1/group'
    },
    self: {
      href: 'https://rest.api/rest/api/resourcepolicies/1'
    },
  },
  eperson: observableOf(createSuccessfulRemoteDataObject(undefined)),
  group: observableOf(createSuccessfulRemoteDataObject(GroupMock))
};

const epersonRP: any = {
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
      href: 'https://rest.api/rest/api/resourcepolicies/1/eperson'
    },
    group: {
      href: 'https://rest.api/rest/api/resourcepolicies/1/group'
    },
    self: {
      href: 'https://rest.api/rest/api/resourcepolicies/1'
    },
  },
  eperson: observableOf(createSuccessfulRemoteDataObject(EPersonMock)),
  group: observableOf(createSuccessfulRemoteDataObject(undefined))
};

const item = Object.assign(new Item(), {
  uuid: 'itemUUID',
  id: 'itemUUID',
  _links: {
    self: { href: 'item-selflink' }
  },
});

describe('ResourcePolicyEntryComponent', () => {
  let fixture: ComponentFixture<ResourcePolicyEntryComponent>;
  let comp: ResourcePolicyEntryComponent;
  let compAsAny: any;

  let dsoNameService;
  let groupService;
  let routeStub;
  let routerStub;

  it('should pass', () => {
    expect(true).toBe(true);
  });

  beforeEach(() => {
    dsoNameService = createSpyObj('dsoNameMock', {
      getName: 'NAME'
    });
    groupService = jasmine.createSpyObj('groupService', {
      findByHref: jasmine.createSpy('findByHref'),
    });
    routeStub = {
      data: observableOf({
        item: createSuccessfulRemoteDataObject(item)
      })
    };
    routerStub = Object.assign(new RouterStub(), {
      url: `url/edit`
    });


    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        TranslateModule.forRoot()
      ],
      declarations: [
        ResourcePolicyEntryComponent,
      ],
      providers: [
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: Router, useValue: routerStub },
        { provide: GroupDataService, useValue: groupService },
        { provide: DSONameService, useValue: dsoNameService }
      ],
      // schemas: [
      //   NO_ERRORS_SCHEMA
      // ]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ResourcePolicyEntryComponent);
    comp = fixture.componentInstance;
    compAsAny = comp;
  });

  describe('target DSO is an EPerson', () => {
    beforeEach(() => {
      comp.entry = {
        id: 'test',
        policy: epersonRP,
        checked: false,
      };
      comp.ngOnInit();
    });

    it('should have a valid epersonName$', () => {
      expect(compAsAny.epersonName$).toBeObservable(cold('(n|)', { u: undefined, n: 'NAME' }));
    });

    it('should have an undefined groupName$', () => {
      expect(compAsAny.groupName$).toBeObservable(cold('(u|)', { u: undefined, n: 'NAME' }));
    });
  });

  describe('target DSO is a Group ', () => {
    beforeEach(() => {
      comp.entry = {
        id: 'test',
        policy: groupRP,
        checked: false,
      };
      comp.ngOnInit();
    });

    it('should have a valid groupName$', () => {
      expect(compAsAny.groupName$).toBeObservable(cold('(n|)', { u: undefined, n: 'NAME' }));
    });

    it('should have an undefined epersonName$', () => {
      expect(compAsAny.epersonName$).toBeObservable(cold('(u|)', { u: undefined, n: 'NAME' }));
    });
  });

  describe('', () => {
    beforeEach(() => {
      comp.entry = {
        id: 'test',
        policy: groupRP,
        checked: false,
      };
      comp.ngOnInit();
    });

    it('should format date properly', () => {
      expect(comp.formatDate('2020-04-14T12:00:00Z')).toBe('2020-04-14');
    });

    it('should redirect to ResourcePolicy edit page', () => {

      comp.redirectToResourcePolicyEditPage();
      expect(compAsAny.router.navigate).toHaveBeenCalled();
    });

    it('should redirect to Group edit page', () => {
      compAsAny.groupService.findByHref.and.returnValue(observableOf(createSuccessfulRemoteDataObject(GroupMock)));

      comp.redirectToGroupEditPage();
      expect(compAsAny.router.navigate).toHaveBeenCalled();
    });

    it('should emit new state when checkbox is toggled', () => {
      spyOn(comp.toggleCheckbox, 'emit');

      const checkbox = fixture.debugElement.query(By.css('input[type="checkbox"]'));

      comp.entry.checked = false;
      checkbox.triggerEventHandler('ngModelChange', true);
      expect(comp.toggleCheckbox.emit).toHaveBeenCalledWith(true);

      comp.entry.checked = true;
      checkbox.triggerEventHandler('ngModelChange', false);
      expect(comp.toggleCheckbox.emit).toHaveBeenCalledWith(false);
    });
  });
});
