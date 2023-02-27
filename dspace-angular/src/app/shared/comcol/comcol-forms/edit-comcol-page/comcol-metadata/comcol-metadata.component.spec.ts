import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { ComColDataService } from '../../../../../core/data/comcol-data.service';
import { Community } from '../../../../../core/shared/community.model';
import { NotificationsService } from '../../../../notifications/notifications.service';
import { SharedModule } from '../../../../shared.module';
import { NotificationsServiceStub } from '../../../../testing/notifications-service.stub';
import {
  createFailedRemoteDataObject$,
  createSuccessfulRemoteDataObject,
  createSuccessfulRemoteDataObject$
} from '../../../../remote-data.utils';
import { ComcolMetadataComponent } from './comcol-metadata.component';

describe('ComColMetadataComponent', () => {
  let comp: ComcolMetadataComponent<any>;
  let fixture: ComponentFixture<ComcolMetadataComponent<any>>;
  let dsoDataService;
  let router: Router;

  let community;
  let newCommunity;
  let communityDataServiceStub;
  let routerStub;
  let routeStub;

  const logoEndpoint = 'rest/api/logo/endpoint';

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

    communityDataServiceStub = {
      update: (com, uuid?) => createSuccessfulRemoteDataObject$(newCommunity),
      patch: () => null,
      getLogoEndpoint: () => observableOf(logoEndpoint)
    };

    routerStub = {
      navigate: (commands) => commands
    };

    routeStub = {
      parent: {
        data: observableOf({
          dso: createSuccessfulRemoteDataObject(community)
        })
      }
    };

  }

  beforeEach(waitForAsync(() => {
    initializeVars();
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), SharedModule, CommonModule, RouterTestingModule],
      providers: [
        { provide: ComColDataService, useValue: communityDataServiceStub },
        { provide: Router, useValue: routerStub },
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ComcolMetadataComponent);
    comp = fixture.componentInstance;
    (comp as any).type = Community.type;
    fixture.detectChanges();
    dsoDataService = (comp as any).dsoDataService;
    router = (comp as any).router;
  });

  describe('onSubmit', () => {
    let data;

    describe('with an empty queue in the uploader', () => {
      beforeEach(() => {
        data = {
          operations: [
            {
              op: 'replace',
              path: '/metadata/dc.title',
              value: {
                value: 'test',
                language: null,
              },
            },
          ],
          dso: new Community(),
          uploader: {
            options: {
              url: ''
            },
            queue: [],
            /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
            uploadAll: () => {
            }
            /* eslint-enable no-empty,@typescript-eslint/no-empty-function */
          },
          deleteLogo: false,
        };
        spyOn(router, 'navigate');
      });

      describe('when successful', () => {

        beforeEach(() => {
          spyOn(dsoDataService, 'patch').and.returnValue(createSuccessfulRemoteDataObject$({}));
        });

        it('should navigate', () => {
          comp.onSubmit(data);
          fixture.detectChanges();
          expect(router.navigate).toHaveBeenCalled();
        });
      });

      describe('on failure', () => {

        beforeEach(() => {
          spyOn(dsoDataService, 'patch').and.returnValue(createFailedRemoteDataObject$('Error', 500));
        });

        it('should not navigate', () => {
          comp.onSubmit(data);
          fixture.detectChanges();
          expect(router.navigate).not.toHaveBeenCalled();
        });
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
        comp.onSubmit(data);
        fixture.detectChanges();
        expect(router.navigate).not.toHaveBeenCalled();
      });

      it('should set the uploader\'s url to the logo\'s endpoint', () => {
        comp.onSubmit(data);
        fixture.detectChanges();
        expect(data.uploader.options.url).toEqual(logoEndpoint);
      });

      it('should call the uploader\'s uploadAll', () => {
        spyOn(data.uploader, 'uploadAll');
        comp.onSubmit(data);
        fixture.detectChanges();
        expect(data.uploader.uploadAll).toHaveBeenCalled();
      });
    });
  });

  describe('navigateToHomePage', () => {
    beforeEach(() => {
      spyOn(router, 'navigate');
      comp.navigateToHomePage();
    });

    it('should navigate', () => {
      expect(router.navigate).toHaveBeenCalled();
    });
  });

});
