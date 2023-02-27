import { StoreModule } from '@ngrx/store';
import { fakeAsync, flush, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpHeaders } from '@angular/common/http';

import { of as observableOf, throwError as observableThrowError } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { cold, getTestScheduler, hot, } from 'jasmine-marbles';

import { RouterMock } from '../shared/mocks/router.mock';
import { SubmissionService } from './submission.service';
import { submissionReducers } from './submission.reducers';
import { SubmissionRestService } from '../core/submission/submission-rest.service';
import { RouteService } from '../core/services/route.service';
import { SubmissionRestServiceStub } from '../shared/testing/submission-rest-service.stub';
import { MockActivatedRoute } from '../shared/mocks/active-router.mock';
import { HttpOptions } from '../core/dspace-rest/dspace-rest.service';
import { SubmissionScopeType } from '../core/submission/submission-scope-type';
import { mockSubmissionDefinition, mockSubmissionRestResponse } from '../shared/mocks/submission.mock';
import { NotificationsService } from '../shared/notifications/notifications.service';
import { TranslateLoaderMock } from '../shared/mocks/translate-loader.mock';
import {
  CancelSubmissionFormAction,
  ChangeSubmissionCollectionAction,
  DiscardSubmissionAction,
  InitSubmissionFormAction,
  ResetSubmissionFormAction,
  SaveAndDepositSubmissionAction,
  SaveForLaterSubmissionFormAction,
  SaveSubmissionFormAction,
  SaveSubmissionSectionFormAction,
  SetActiveSectionAction
} from './objects/submission-objects.actions';
import { createFailedRemoteDataObject, } from '../shared/remote-data.utils';
import { getMockSearchService } from '../shared/mocks/search-service.mock';
import { getMockRequestService } from '../shared/mocks/request.service.mock';
import { RequestService } from '../core/data/request.service';
import { SearchService } from '../core/shared/search/search.service';
import { Item } from '../core/shared/item.model';
import { storeModuleConfig } from '../app.reducer';
import { environment } from '../../environments/environment';
import { SubmissionJsonPatchOperationsService } from '../core/submission/submission-json-patch-operations.service';
import { SubmissionJsonPatchOperationsServiceStub } from '../shared/testing/submission-json-patch-operations-service.stub';

describe('SubmissionService test suite', () => {
  const collectionId = '43fe1f8c-09a6-4fcf-9c78-5d4fed8f2c8f';
  const submissionId = '826';
  const sectionId = 'test';
  const subState = {
    objects: {
      826: {
        collection: '43fe1f8c-09a6-4fcf-9c78-5d4fed8f2c8f',
        definition: 'traditional',
        selfUrl: 'https://rest.api/dspace-spring-rest/api/submission/workspaceitems/826',
        activeSection: 'keyinformation',
        sections: {
          extraction: {
            config: '',
            mandatory: true,
            sectionType: 'utils',
            visibility: {
              main: 'HIDDEN',
              other: 'HIDDEN'
            },
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          collection: {
            config: '',
            mandatory: true,
            sectionType: 'collection',
            visibility: {
              main: 'HIDDEN',
              other: 'HIDDEN'
            },
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          keyinformation: {
            header: 'submit.progressbar.describe.keyinformation',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/keyinformation',
            mandatory: true,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          indexing: {
            header: 'submit.progressbar.describe.indexing',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/indexing',
            mandatory: false,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: false,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          publicationchannel: {
            header: 'submit.progressbar.describe.publicationchannel',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/publicationchannel',
            mandatory: true,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: true
          },
          acknowledgement: {
            header: 'submit.progressbar.describe.acknowledgement',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/acknowledgement',
            mandatory: false,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: false,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          identifiers: {
            header: 'submit.progressbar.describe.identifiers',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/identifiers',
            mandatory: false,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: false,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          references: {
            header: 'submit.progressbar.describe.references',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/references',
            mandatory: false,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: false,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          upload: {
            header: 'submit.progressbar.upload',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionuploads/upload',
            mandatory: true,
            sectionType: 'upload',
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          license: {
            header: 'submit.progressbar.license',
            config: '',
            mandatory: true,
            sectionType: 'license',
            visibility: {
              main: null,
              other: 'READONLY'
            },
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          }
        },
        isLoading: false,
        savePending: false,
        depositPending: false
      }
    }
  };
  const validSubState = {
    objects: {
      826: {
        collection: '43fe1f8c-09a6-4fcf-9c78-5d4fed8f2c8f',
        definition: 'traditional',
        selfUrl: 'https://rest.api/dspace-spring-rest/api/submission/workspaceitems/826',
        activeSection: 'keyinformation',
        sections: {
          extraction: {
            config: '',
            mandatory: true,
            sectionType: 'utils',
            visibility: {
              main: 'HIDDEN',
              other: 'HIDDEN'
            },
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          collection: {
            config: '',
            mandatory: true,
            sectionType: 'collection',
            visibility: {
              main: 'HIDDEN',
              other: 'HIDDEN'
            },
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          keyinformation: {
            header: 'submit.progressbar.describe.keyinformation',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/keyinformation',
            mandatory: true,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: true
          },
          indexing: {
            header: 'submit.progressbar.describe.indexing',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/indexing',
            mandatory: false,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: false,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          publicationchannel: {
            header: 'submit.progressbar.describe.publicationchannel',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/publicationchannel',
            mandatory: true,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: true
          },
          acknowledgement: {
            header: 'submit.progressbar.describe.acknowledgement',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/acknowledgement',
            mandatory: false,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: false,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          identifiers: {
            header: 'submit.progressbar.describe.identifiers',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/identifiers',
            mandatory: false,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: false,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          references: {
            header: 'submit.progressbar.describe.references',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/references',
            mandatory: false,
            sectionType: 'submission-form',
            collapsed: false,
            enabled: false,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: false
          },
          upload: {
            header: 'submit.progressbar.upload',
            config: 'https://rest.api/dspace-spring-rest/api/config/submissionuploads/upload',
            mandatory: true,
            sectionType: 'upload',
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: true
          },
          license: {
            header: 'submit.progressbar.license',
            config: '',
            mandatory: true,
            sectionType: 'license',
            visibility: {
              main: null,
              other: 'READONLY'
            },
            collapsed: false,
            enabled: true,
            data: {},
            errorsToShow: [],
            serverValidationErrors: [],
            isLoading: false,
            isValid: true
          }
        },
        isLoading: false,
        savePending: false,
        depositPending: false
      }
    }
  };
  const restService = new SubmissionRestServiceStub();
  const router = new RouterMock();
  const selfUrl = 'https://rest.api/dspace-spring-rest/api/submission/workspaceitems/826';
  const submissionDefinition: any = mockSubmissionDefinition;
  const submissionJsonPatchOperationsService = new SubmissionJsonPatchOperationsServiceStub();

  let scheduler: TestScheduler;
  let service: SubmissionService;

  const searchService = getMockSearchService();

  const requestServce = getMockRequestService();

  beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot({ submissionReducers } as any, storeModuleConfig),
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      providers: [
        { provide: Router, useValue: router },
        { provide: SubmissionRestService, useValue: restService },
        { provide: ActivatedRoute, useValue: new MockActivatedRoute() },
        { provide: SearchService, useValue: searchService },
        { provide: RequestService, useValue: requestServce },
        { provide: SubmissionJsonPatchOperationsService, useValue: submissionJsonPatchOperationsService },
        NotificationsService,
        RouteService,
        SubmissionService,
        TranslateService
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    service = TestBed.inject(SubmissionService);
    spyOn((service as any).store, 'dispatch').and.callThrough();
  });

  describe('changeSubmissionCollection', () => {
    it('should dispatch a new ChangeSubmissionCollectionAction', () => {
      service.changeSubmissionCollection(submissionId, collectionId);
      const expected = new ChangeSubmissionCollectionAction(submissionId, collectionId);

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });
  });

  describe('createSubmission', () => {
    it('should create a new submission', () => {
      service.createSubmission();

      expect((service as any).restService.postToEndpoint).toHaveBeenCalled();
      expect((service as any).restService.postToEndpoint).toHaveBeenCalledWith('workspaceitems', {}, null, null, undefined);
    });

    it('should create a new submission with collection', () => {
      service.createSubmission(collectionId);

      expect((service as any).restService.postToEndpoint).toHaveBeenCalled();
      expect((service as any).restService.postToEndpoint).toHaveBeenCalledWith('workspaceitems', {}, null, null, collectionId);
    });
  });

  describe('createSubmissionFromExternalSource', () => {
    it('should deposit submission', () => {
      const options: HttpOptions = Object.create({});
      let headers = new HttpHeaders();
      headers = headers.append('Content-Type', 'text/uri-list');
      options.headers = headers;

      service.createSubmissionFromExternalSource(selfUrl, collectionId);

      expect((service as any).restService.postToEndpoint).toHaveBeenCalledWith('workspaceitems', selfUrl, null, options, collectionId);
    });
  });

  describe('depositSubmission', () => {
    it('should deposit submission', () => {
      const options: HttpOptions = Object.create({});
      let headers = new HttpHeaders();
      headers = headers.append('Content-Type', 'text/uri-list');
      options.headers = headers;

      service.depositSubmission(selfUrl);

      expect((service as any).restService.postToEndpoint).toHaveBeenCalledWith('workflowitems', selfUrl, null, options);
    });
  });

  describe('discardSubmission', () => {
    it('should discard submission', () => {
      service.discardSubmission('826');

      expect((service as any).restService.deleteById).toHaveBeenCalledWith('826');
    });
  });

  describe('dispatchInit', () => {
    it('should dispatch a new InitSubmissionFormAction', () => {
      service.dispatchInit(
        collectionId,
        submissionId,
        selfUrl,
        submissionDefinition,
        {},
        new Item(),
        null
      );
      const expected = new InitSubmissionFormAction(
        collectionId,
        submissionId,
        selfUrl,
        submissionDefinition,
        {},
        new Item(),
        null);

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });
  });

  describe('dispatchDeposit', () => {
    it('should dispatch a new SaveAndDepositSubmissionAction', () => {
      service.dispatchDeposit(submissionId,);
      const expected = new SaveAndDepositSubmissionAction(submissionId);

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });
  });

  describe('dispatchDiscard', () => {
    it('should dispatch a new DiscardSubmissionAction', () => {
      service.dispatchDiscard(submissionId,);
      const expected = new DiscardSubmissionAction(submissionId);

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });
  });

  describe('dispatchSave', () => {
    it('should dispatch a new SaveSubmissionFormAction', () => {
      service.dispatchSave(submissionId);
      const expected = new SaveSubmissionFormAction(submissionId);

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });

    it('should dispatch a new SaveSubmissionFormAction with manual flag', () => {
      service.dispatchSave(submissionId, true);
      const expected = new SaveSubmissionFormAction(submissionId, true);

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });
  });

  describe('dispatchSaveForLater', () => {
    it('should dispatch a new SaveForLaterSubmissionFormAction', () => {
      service.dispatchSaveForLater(submissionId,);
      const expected = new SaveForLaterSubmissionFormAction(submissionId);

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });
  });

  describe('dispatchSaveSection', () => {
    it('should dispatch a new SaveSubmissionSectionFormAction', () => {
      service.dispatchSaveSection(submissionId, sectionId);
      const expected = new SaveSubmissionSectionFormAction(submissionId, sectionId);

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });
  });

  describe('getSubmissionObject', () => {
    it('should return submission object state from the store', () => {
      spyOn((service as any).store, 'select').and.returnValue(hot('a', {
        a: subState.objects[826]
      }));

      const result = service.getSubmissionObject('826');
      const expected = cold('b', { b: subState.objects[826] });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getActiveSectionId', () => {
    it('should return current active submission form section', () => {
      spyOn((service as any).store, 'select').and.returnValue(hot('a', {
        a: subState.objects[826]
      }));

      const result = service.getActiveSectionId('826');
      const expected = cold('b', { b: 'keyinformation' });

      expect(result).toBeObservable(expected);

    });
  });

  describe('getSubmissionSections', () => {
    it('should return submission form sections', () => {
      spyOn((service as any).store, 'select').and.returnValue(hot('a|', {
        a: subState.objects[826]
      }));

      const result = service.getSubmissionSections('826');
      const expected = cold('(bc|)', {
        b: [],
        c:
          [
            {
              header: 'submit.progressbar.describe.keyinformation',
              id: 'keyinformation',
              config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/keyinformation',
              mandatory: true,
              sectionType: 'submission-form',
              data: {},
              errorsToShow: [],
              serverValidationErrors: []
            },
            {
              header: 'submit.progressbar.describe.indexing',
              id: 'indexing',
              config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/indexing',
              mandatory: false,
              sectionType: 'submission-form',
              data: {},
              errorsToShow: [],
              serverValidationErrors: []
            },
            {
              header: 'submit.progressbar.describe.publicationchannel',
              id: 'publicationchannel',
              config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/publicationchannel',
              mandatory: true,
              sectionType: 'submission-form',
              data: {},
              errorsToShow: [],
              serverValidationErrors: []
            },
            {
              header: 'submit.progressbar.describe.acknowledgement',
              id: 'acknowledgement',
              config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/acknowledgement',
              mandatory: false,
              sectionType: 'submission-form',
              data: {},
              errorsToShow: [],
              serverValidationErrors: []
            },
            {
              header: 'submit.progressbar.describe.identifiers',
              id: 'identifiers',
              config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/identifiers',
              mandatory: false,
              sectionType: 'submission-form',
              data: {},
              errorsToShow: [],
              serverValidationErrors: []
            },
            {
              header: 'submit.progressbar.describe.references',
              id: 'references',
              config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/references',
              mandatory: false,
              sectionType: 'submission-form',
              data: {},
              errorsToShow: [],
              serverValidationErrors: []
            },
            {
              header: 'submit.progressbar.upload',
              id: 'upload',
              config: 'https://rest.api/dspace-spring-rest/api/config/submissionuploads/upload',
              mandatory: true,
              sectionType: 'upload',
              data: {},
              errorsToShow: [],
              serverValidationErrors: []
            },
            {
              header: 'submit.progressbar.license',
              id: 'license',
              config: '',
              mandatory: true,
              sectionType: 'license',
              data: {},
              errorsToShow: [],
              serverValidationErrors: []
            }
          ]
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getDisabledSectionsList', () => {
    it('should return list of submission disabled sections', () => {
      spyOn((service as any).store, 'select').and.returnValue(hot('-a|', {
        a: subState.objects[826]
      }));

      const result = service.getDisabledSectionsList('826');
      const expected = cold('bc|', {
        b: [],
        c:
          [
            {
              header: 'submit.progressbar.describe.indexing',
              id: 'indexing',
            },
            {
              header: 'submit.progressbar.describe.acknowledgement',
              id: 'acknowledgement',
            },
            {
              header: 'submit.progressbar.describe.identifiers',
              id: 'identifiers',
            },
            {
              header: 'submit.progressbar.describe.references',
              id: 'references',
            }
          ]
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getSubmissionObjectLinkName', () => {
    it('should return properly submission link name', () => {
      let expected = 'workspaceitems';
      router.setRoute('/workspaceitems/826/edit');
      expect(service.getSubmissionObjectLinkName()).toBe(expected);

      expected = 'workspaceitems';
      router.setRoute('/submit');
      expect(service.getSubmissionObjectLinkName()).toBe(expected);

      expected = 'workflowitems';
      router.setRoute('/workflowitems/826/edit');
      expect(service.getSubmissionObjectLinkName()).toBe(expected);

      expected = 'edititems';
      router.setRoute('/items/9e79b1f2-ae0f-4737-9a4b-990952a8857c/edit');
      expect(service.getSubmissionObjectLinkName()).toBe(expected);
    });
  });

  describe('getSubmissionScope', () => {
    it('should return properly submission scope', () => {
      let expected = SubmissionScopeType.WorkspaceItem;

      router.setRoute('/workspaceitems/826/edit');
      expect(service.getSubmissionScope()).toBe(expected);

      router.setRoute('/submit');
      expect(service.getSubmissionScope()).toBe(expected);

      expected = SubmissionScopeType.WorkflowItem;
      router.setRoute('/workflowitems/826/edit');
      expect(service.getSubmissionScope()).toBe(expected);

    });
  });

  describe('getSubmissionStatus', () => {
    it('should return properly submission status', () => {
      spyOn((service as any).store, 'select').and.returnValue(hot('-a-b', {
        a: subState,
        b: validSubState
      }));
      const result = service.getSubmissionStatus('826');
      const expected = cold('cc-d', {
        c: false,
        d: true
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getSubmissionSaveProcessingStatus', () => {
    it('should return submission save processing status', () => {
      spyOn((service as any).store, 'select').and.returnValue(hot('-a', {
        a: subState.objects[826]
      }));

      const result = service.getSubmissionSaveProcessingStatus('826');
      const expected = cold('bb', {
        b: false
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getSubmissionDepositProcessingStatus', () => {
    it('should return submission deposit processing status', () => {
      spyOn((service as any).store, 'select').and.returnValue(hot('-a', {
        a: subState.objects[826]
      }));

      const result = service.getSubmissionDepositProcessingStatus('826');
      const expected = cold('bb', {
        b: false
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('hasUnsavedModification', () => {
    it('should call jsonPatchOperationService hasPendingOperation observable', () => {
      (service as any).jsonPatchOperationService.hasPendingOperations = jasmine.createSpy('hasPendingOperations')
        .and.returnValue(observableOf(true));

      scheduler = getTestScheduler();
      scheduler.schedule(() => service.hasUnsavedModification());
      scheduler.flush();

      expect((service as any).jsonPatchOperationService.hasPendingOperations).toHaveBeenCalledWith('sections');

    });
  });

  describe('isSectionHidden', () => {
    it('should return true/false when section is hidden/visible', () => {
      let section: any = {
        config: '',
        header: '',
        mandatory: true,
        sectionType: 'collection' as any,
        visibility: {
          main: 'HIDDEN',
          other: 'HIDDEN'
        },
        collapsed: false,
        enabled: true,
        data: {},
        errorsToShow: [],
        serverValidationErrors: [],
        isLoading: false,
        isValid: false
      };
      expect(service.isSectionHidden(section)).toBeTruthy();

      section = {
        header: 'submit.progressbar.describe.keyinformation',
        config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/keyinformation',
        mandatory: true,
        sectionType: 'submission-form',
        collapsed: false,
        enabled: true,
        data: {},
        errorsToShow: [],
        serverValidationErrors: [],
        isLoading: false,
        isValid: false
      };
      expect(service.isSectionHidden(section)).toBeFalsy();
    });
  });

  describe('isSubmissionLoading', () => {
    it('should return true/false when section is loading/not loading', () => {
      const spy = spyOn(service, 'getSubmissionObject').and.returnValue(observableOf({ isLoading: true }));

      let expected = cold('(b|)', {
        b: true
      });

      expect(service.isSubmissionLoading(submissionId)).toBeObservable(expected);

      spy.and.returnValue(observableOf({ isLoading: false }));

      expected = cold('(b|)', {
        b: false
      });

      expect(service.isSubmissionLoading(submissionId)).toBeObservable(expected);
    });
  });

  describe('notifyNewSection', () => {
    it('should return true/false when section is loading/not loading', fakeAsync(() => {
      spyOn((service as any).translate, 'get').and.returnValue(observableOf('test'));

      spyOn((service as any).notificationsService, 'info');

      service.notifyNewSection(submissionId, sectionId);
      flush();

      expect((service as any).notificationsService.info).toHaveBeenCalledWith(null, 'submission.sections.general.metadata-extracted-new-section', null, true);
    }));
  });

  describe('redirectToMyDSpace', () => {
    it('should redirect to MyDspace page', () => {
      scheduler = getTestScheduler();
      const spy = spyOn((service as any).routeService, 'getPreviousUrl');

      spy.and.returnValue(observableOf('/mydspace?configuration=workflow'));
      scheduler.schedule(() => service.redirectToMyDSpace());
      scheduler.flush();

      expect((service as any).router.navigateByUrl).toHaveBeenCalledWith('/mydspace?configuration=workflow');

      spy.and.returnValue(observableOf(''));
      scheduler.schedule(() => service.redirectToMyDSpace());
      scheduler.flush();

      expect((service as any).router.navigate).toHaveBeenCalledWith(['/mydspace']);

      spy.and.returnValue(observableOf('/home'));
      scheduler.schedule(() => service.redirectToMyDSpace());
      scheduler.flush();

      expect((service as any).router.navigate).toHaveBeenCalledWith(['/mydspace']);
    });
  });

  describe('resetAllSubmissionObjects', () => {
    it('should dispatch a new CancelSubmissionFormAction', () => {
      service.resetAllSubmissionObjects();
      const expected = new CancelSubmissionFormAction();

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });
  });

  describe('resetSubmissionObject', () => {
    it('should dispatch a new ResetSubmissionFormAction', () => {
      service.resetSubmissionObject(
        collectionId,
        submissionId,
        selfUrl,
        submissionDefinition,
        {},
        new Item()
      )
      ;
      const expected = new ResetSubmissionFormAction(
        collectionId,
        submissionId,
        selfUrl,
        {},
        submissionDefinition,
        new Item()
      );

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });
  });

  describe('retrieveSubmission', () => {
    it('should retrieve submission from REST endpoint', () => {
      (service as any).restService.getDataById.and.returnValue(hot('a|', {
        a: mockSubmissionRestResponse
      }));

      const result = service.retrieveSubmission('826');
      const expected = cold('(b|)', {
        b: jasmine.objectContaining({ payload: mockSubmissionRestResponse[0] })
      });

      expect(result).toBeObservable(expected);
    });

    it('should catch error from REST endpoint', () => {
      (service as any).restService.getDataById.and.callFake(
        () => observableThrowError({
          statusCode: 500,
          errorMessage: 'Internal Server Error',
        })
      );

      service.retrieveSubmission('826').subscribe((r) => {
        const expectedRD = createFailedRemoteDataObject('Internal Server Error',500) as any;
        expect(r.payload).toEqual(expectedRD.payload);
        expect(r.statusCode).toEqual(expectedRD.statusCode);
        expect(r.errorMessage).toEqual(expectedRD.errorMessage);
        expect(r.hasSucceeded).toEqual(expectedRD.hasSucceeded);
      });
    });
  });

  describe('setActiveSection', () => {
    it('should dispatch a new SetActiveSectionAction', () => {
      service.setActiveSection(submissionId, sectionId);
      const expected = new SetActiveSectionAction(submissionId, sectionId);

      expect((service as any).store.dispatch).toHaveBeenCalledWith(expected);
    });
  });

  describe('startAutoSave', () => {

    let environmentAutoSaveTimerOriginalValue;

    beforeEach(() => {
      environmentAutoSaveTimerOriginalValue = environment.submission.autosave.timer;
    });

    it('should start Auto Save', fakeAsync(() => {
      const duration = environment.submission.autosave.timer;

      service.startAutoSave('826');
      const sub = (service as any).timer$.subscribe();

      tick(duration / 2);
      expect((service as any).store.dispatch).not.toHaveBeenCalled();

      tick(duration / 2);
      expect((service as any).store.dispatch).toHaveBeenCalled();

      sub.unsubscribe();
      (service as any).autoSaveSub.unsubscribe();
    }));

    it('should not start Auto Save if timer is 0', fakeAsync(() => {
      environment.submission.autosave.timer = 0;

      service.startAutoSave('826');

      expect((service as any).autoSaveSub).toBeUndefined();
    }));

    afterEach(() => {
      environment.submission.autosave.timer = environmentAutoSaveTimerOriginalValue;
    });

  });

  describe('stopAutoSave', () => {
    it('should stop Auto Save', () => {
      service.startAutoSave('826');
      service.stopAutoSave();

      expect((service as any).autoSaveSub).toBeNull();
    });
  });
});
