import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CommunityDataService } from '../../../../core/data/community-data.service';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { Community } from '../../../../core/shared/community.model';
import { SharedModule } from '../../../shared.module';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { DeleteComColPageComponent } from './delete-comcol-page.component';
import { NotificationsService } from '../../../notifications/notifications.service';
import { NotificationsServiceStub } from '../../../testing/notifications-service.stub';
import { getTestScheduler } from 'jasmine-marbles';
import { ComColDataService } from '../../../../core/data/comcol-data.service';
import { createFailedRemoteDataObject$, createNoContentRemoteDataObject$ } from '../../../remote-data.utils';

describe('DeleteComColPageComponent', () => {
  let comp: DeleteComColPageComponent<any>;
  let fixture: ComponentFixture<DeleteComColPageComponent<any>>;
  let dsoDataService: CommunityDataService;
  let router: Router;

  let community;
  let newCommunity;
  let parentCommunity;
  let routerStub;
  let routeStub;
  let notificationsService;
  let translateServiceStub;
  let requestServiceStub;

  let scheduler;

  const validUUID = 'valid-uuid';
  const invalidUUID = 'invalid-uuid';
  const frontendURL = '/testType';

  function initializeVars() {
    community = Object.assign(new Community(), {
      uuid: 'a20da287-e174-466a-9926-f66b9300d347',
      metadata: [{
        key: 'dc.title',
        value: 'test community'
      }]
    });

    newCommunity = Object.assign(new Community(), {
      uuid: '1ff59938-a69a-4e62-b9a4-718569c55d48',
      metadata: [{
        key: 'dc.title',
        value: 'new community'
      }]
    });

    parentCommunity = Object.assign(new Community(), {
      uuid: 'a20da287-e174-466a-9926-f66as300d399',
      id: 'a20da287-e174-466a-9926-f66as300d399',
      metadata: [{
        key: 'dc.title',
        value: 'parent community'
      }]
    });

    dsoDataService = jasmine.createSpyObj(
      'dsoDataService',
      {
        delete: createNoContentRemoteDataObject$(),
        findByHref: jasmine.createSpy('findByHref'),
      });

    routerStub = {
      navigate: (commands) => commands
    };

    routeStub = {
      data: observableOf(community)
    };

    translateServiceStub = jasmine.createSpyObj('TranslateService', {
      instant: jasmine.createSpy('instant')
    });

  }

  beforeEach(waitForAsync(() => {
    initializeVars();
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), SharedModule, CommonModule, RouterTestingModule],
      providers: [
        { provide: ComColDataService, useValue: dsoDataService },
        { provide: Router, useValue: routerStub },
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: TranslateService, useValue: translateServiceStub },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DeleteComColPageComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
    notificationsService = (comp as any).notifications;
    (comp as any).frontendURL = frontendURL;
    router = (comp as any).router;
    scheduler = getTestScheduler();
  });

  describe('onConfirm', () => {
    let data1;
    let data2;
    beforeEach(() => {
      data1 = {
        dso: Object.assign(new Community(), {
          uuid: validUUID,
          metadata: [{
            key: 'dc.title',
            value: 'test'
          }]
        }),
        _links: {}
      };

      data2 = {
        dso: Object.assign(new Community(), {
          uuid: invalidUUID,
          metadata: [{
            key: 'dc.title',
            value: 'test'
          }]
        }),
        _links: {},
        uploader: {
          options: {
            url: ''
          },
          queue: [],
          /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
          uploadAll: () => {
          }
          /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
        }
      };
    });

    it('should show an error notification on failure', () => {
      (dsoDataService.delete as any).and.returnValue(createFailedRemoteDataObject$('Error', 500));
      spyOn(router, 'navigate');
      scheduler.schedule(() => comp.onConfirm(data2));
      scheduler.flush();
      fixture.detectChanges();
      expect(notificationsService.error).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalled();
    });

    it('should show a success notification on success and navigate', () => {
      spyOn(router, 'navigate');
      scheduler.schedule(() => comp.onConfirm(data1));
      scheduler.flush();
      fixture.detectChanges();
      expect(notificationsService.success).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalled();
    });

    it('should call delete on the data service', () => {
      comp.onConfirm(data1);
      fixture.detectChanges();
      expect(dsoDataService.delete).toHaveBeenCalledWith(data1.id);
    });
  });

  describe('onCancel', () => {
    let data1;
    beforeEach(() => {
      data1 = Object.assign(new Community(), {
        uuid: validUUID,
        metadata: [{
          key: 'dc.title',
          value: 'test'
        }]
      });
    });

    it('should redirect to the edit page', () => {
      const redirectURL = frontendURL + '/' + validUUID + '/edit';
      spyOn(router, 'navigate');
      comp.onCancel(data1);
      fixture.detectChanges();
      expect(router.navigate).toHaveBeenCalledWith([redirectURL]);
    });
  });
});
