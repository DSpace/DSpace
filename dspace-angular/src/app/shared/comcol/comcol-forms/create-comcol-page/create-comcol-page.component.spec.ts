import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CommunityDataService } from '../../../../core/data/community-data.service';
import { RouteService } from '../../../../core/services/route.service';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { Community } from '../../../../core/shared/community.model';
import { SharedModule } from '../../../shared.module';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CreateComColPageComponent } from './create-comcol-page.component';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../../remote-data.utils';
import { ComColDataService } from '../../../../core/data/comcol-data.service';
import { NotificationsService } from '../../../notifications/notifications.service';
import { NotificationsServiceStub } from '../../../testing/notifications-service.stub';
import { RequestService } from '../../../../core/data/request.service';
import { getTestScheduler } from 'jasmine-marbles';

describe('CreateComColPageComponent', () => {
  let comp: CreateComColPageComponent<any>;
  let fixture: ComponentFixture<CreateComColPageComponent<any>>;
  let communityDataService: CommunityDataService;
  let dsoDataService: CommunityDataService;
  let routeService: RouteService;
  let router: Router;

  let community;
  let newCommunity;
  let parentCommunity;
  let communityDataServiceStub;
  let routeServiceStub;
  let routerStub;
  let requestServiceStub;
  let scheduler;

  const logoEndpoint = 'rest/api/logo/endpoint';

  function initializeVars() {
    community = Object.assign(new Community(), {
      uuid: 'a20da287-e174-466a-9926-f66b9300d347',
      metadata: [{
        key: 'dc.title',
        value: 'test community'
      }],
      _links: {}
    });

    parentCommunity = Object.assign(new Community(), {
      uuid: 'a20da287-e174-466a-9926-f66as300d399',
      id: 'a20da287-e174-466a-9926-f66as300d399',
      metadata: [{
        key: 'dc.title',
        value: 'parent community'
      }],
      _links: {}
    });

    newCommunity = Object.assign(new Community(), {
      uuid: '1ff59938-a69a-4e62-b9a4-718569c55d48',
      metadata: [{
        key: 'dc.title',
        value: 'new community'
      }],
      _links: {}
    });

    communityDataServiceStub = {
      findById: (uuid) => createSuccessfulRemoteDataObject$(Object.assign(new Community(), {
        uuid: uuid,
        metadata: [{
          key: 'dc.title',
          value: community.name
        }]
      })),
      create: (com, uuid?) => createSuccessfulRemoteDataObject$(newCommunity),
      getLogoEndpoint: () => observableOf(logoEndpoint),
      findByHref: () => null,
      refreshCache: () => {
        return;
      }
    };

    routeServiceStub = {
      getQueryParameterValue: (param) => observableOf(community.uuid)
    };
    routerStub = {
      navigate: (commands) => commands
    };

    requestServiceStub = jasmine.createSpyObj('RequestService', {
      removeByHrefSubstring: jasmine.createSpy('removeByHrefSubstring'),
    });

  }

  beforeEach(waitForAsync(() => {
    initializeVars();
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), SharedModule, CommonModule, RouterTestingModule],
      providers: [
        { provide: ComColDataService, useValue: communityDataServiceStub },
        { provide: CommunityDataService, useValue: communityDataServiceStub },
        { provide: RouteService, useValue: routeServiceStub },
        { provide: Router, useValue: routerStub },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: RequestService, useValue: requestServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CreateComColPageComponent);
    comp = fixture.componentInstance;
    (comp as any).type = Community.type;
    fixture.detectChanges();
    dsoDataService = (comp as any).dsoDataService;
    communityDataService = (comp as any).communityDataService;
    routeService = (comp as any).routeService;
    router = (comp as any).router;
    scheduler = getTestScheduler();
  });

  describe('onSubmit', () => {
    let data;

    describe('with an empty queue in the uploader', () => {
      beforeEach(() => {
        data = {
          dso: Object.assign(new Community(), {
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
            /* eslint-enable no-empty,@typescript-eslint/no-empty-function */
          }
        };
      });

      it('should navigate and refresh cache when successful', () => {
        spyOn(router, 'navigate');
        spyOn((dsoDataService as any), 'refreshCache');
        scheduler.schedule(() => comp.onSubmit(data));
        scheduler.flush();
        expect(router.navigate).toHaveBeenCalled();
        expect((dsoDataService as any).refreshCache).toHaveBeenCalled();
      });

      it('should neither navigate nor refresh cache on failure', () => {
        spyOn(router, 'navigate');
        spyOn(dsoDataService, 'create').and.returnValue(createFailedRemoteDataObject$('server error', 500));
        spyOn(dsoDataService, 'refreshCache');
        scheduler.schedule(() => comp.onSubmit(data));
        scheduler.flush();
        expect(router.navigate).not.toHaveBeenCalled();
        expect((dsoDataService as any).refreshCache).not.toHaveBeenCalled();
      });
    });

    describe('with at least one item in the uploader\'s queue', () => {
      beforeEach(() => {
        data = {
          dso: Object.assign(new Community(), {
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
            queue: [
              {}
            ],
            /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
            uploadAll: () => {
            }
            /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
          }
        };
      });

      it('should not navigate', () => {
        spyOn(router, 'navigate');
        scheduler.schedule(() => comp.onSubmit(data));
        scheduler.flush();
        expect(router.navigate).not.toHaveBeenCalled();
      });

      it('should set the uploader\'s url to the logo\'s endpoint', () => {
        scheduler.schedule(() => comp.onSubmit(data));
        scheduler.flush();
        expect(data.uploader.options.url).toEqual(logoEndpoint);
      });

      it('should call the uploader\'s uploadAll', () => {
        spyOn(data.uploader, 'uploadAll');
        scheduler.schedule(() => comp.onSubmit(data));
        scheduler.flush();
        expect(data.uploader.uploadAll).toHaveBeenCalled();
      });
    });
  });
});
