import { TestBed } from '@angular/core/testing';

import { cold, hot } from 'jasmine-marbles';
import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { Observable, of as observableOf, throwError as observableThrowError } from 'rxjs';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';

import { SubmissionObjectEffects } from './submission-objects.effects';
import {
  CompleteInitSubmissionFormAction,
  DepositSubmissionAction,
  DepositSubmissionErrorAction,
  DepositSubmissionSuccessAction,
  DiscardSubmissionErrorAction,
  DiscardSubmissionSuccessAction,
  InitSectionAction,
  InitSubmissionFormAction,
  SaveForLaterSubmissionFormSuccessAction,
  SaveSubmissionFormErrorAction,
  SaveSubmissionFormSuccessAction,
  SaveSubmissionSectionFormErrorAction,
  SaveSubmissionSectionFormSuccessAction,
  SubmissionObjectActionTypes,
  UpdateSectionDataAction
} from './submission-objects.actions';
import {
  mockSectionsData,
  mockSectionsDataTwo,
  mockSectionsErrors,
  mockSectionsErrorsTouchedField,
  mockSubmissionCollectionId,
  mockSubmissionDefinition,
  mockSubmissionDefinitionResponse,
  mockSubmissionId,
  mockSubmissionRestResponse,
  mockSubmissionSelfUrl,
  mockSubmissionState
} from '../../shared/mocks/submission.mock';
import { SubmissionSectionModel } from '../../core/config/models/config-submission-section.model';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { SubmissionJsonPatchOperationsServiceStub } from '../../shared/testing/submission-json-patch-operations-service.stub';
import { SubmissionJsonPatchOperationsService } from '../../core/submission/submission-json-patch-operations.service';
import { SectionsService } from '../sections/sections.service';
import { SectionsServiceStub } from '../../shared/testing/sections-service.stub';
import { SubmissionService } from '../submission.service';
import { SubmissionServiceStub } from '../../shared/testing/submission-service.stub';
import { TranslateLoaderMock } from '../../shared/mocks/translate-loader.mock';
import { StoreMock } from '../../shared/testing/store.mock';
import { AppState, storeModuleConfig } from '../../app.reducer';
import parseSectionErrors from '../utils/parseSectionErrors';
import { Item } from '../../core/shared/item.model';
import { WorkspaceitemDataService } from '../../core/submission/workspaceitem-data.service';
import { WorkflowItemDataService } from '../../core/submission/workflowitem-data.service';
import { HALEndpointService } from '../../core/shared/hal-endpoint.service';
import { SubmissionObjectDataService } from '../../core/submission/submission-object-data.service';
import { mockSubmissionObjectDataService } from '../../shared/testing/submission-oject-data-service.mock';

describe('SubmissionObjectEffects test suite', () => {
  let submissionObjectEffects: SubmissionObjectEffects;
  let actions: Observable<any>;
  let store: StoreMock<AppState>;

  let notificationsServiceStub;
  let submissionServiceStub;
  let submissionJsonPatchOperationsServiceStub;
  let submissionObjectDataServiceStub;
  const collectionId: string = mockSubmissionCollectionId;
  const submissionId: string = mockSubmissionId;
  const submissionDefinitionResponse: any = mockSubmissionDefinitionResponse;
  const submissionDefinition: any = mockSubmissionDefinition;
  const selfUrl: string = mockSubmissionSelfUrl;
  const submissionState: any = Object.assign({}, mockSubmissionState);

  beforeEach(() => {

    notificationsServiceStub = new NotificationsServiceStub();
    submissionServiceStub =  new SubmissionServiceStub();
    submissionJsonPatchOperationsServiceStub = new SubmissionJsonPatchOperationsServiceStub();
    submissionObjectDataServiceStub = mockSubmissionObjectDataService;

    submissionServiceStub.hasUnsavedModification.and.returnValue(observableOf(true));

    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot({}, storeModuleConfig),
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
      ],
      providers: [
        SubmissionObjectEffects,
        TranslateService,
        { provide: Store, useClass: StoreMock },
        provideMockActions(() => actions),
        { provide: NotificationsService, useValue: notificationsServiceStub },
        { provide: SectionsService, useClass: SectionsServiceStub },
        { provide: SubmissionService, useValue: submissionServiceStub },
        { provide: SubmissionJsonPatchOperationsService, useValue: submissionJsonPatchOperationsServiceStub },
        { provide: WorkspaceitemDataService, useValue: {} },
        { provide: WorkflowItemDataService, useValue: {} },
        { provide: WorkflowItemDataService, useValue: {} },
        { provide: HALEndpointService, useValue: {} },
        { provide: SubmissionObjectDataService, useValue: submissionObjectDataServiceStub },
      ],
    });

    submissionObjectEffects = TestBed.inject(SubmissionObjectEffects);
    store = TestBed.inject(Store as any);
  });

  describe('loadForm$', () => {
    it('should return a INIT_SECTION action for each defined section and a COMPLETE_INIT_SUBMISSION_FORM action', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.INIT_SUBMISSION_FORM,
          payload: {
            collectionId: collectionId,
            submissionId: submissionId,
            selfUrl: selfUrl,
            submissionDefinition: submissionDefinition,
            sections: {},
            item: {metadata: {}},
            errors: [],
          }
        }
      });

      const mappedActions = [];
      (submissionDefinitionResponse.sections as SubmissionSectionModel[])
        .forEach((sectionDefinition: SubmissionSectionModel) => {
          const sectionId = sectionDefinition._links.self.href.substr(sectionDefinition._links.self.href.lastIndexOf('/') + 1);
          const config = sectionDefinition._links.config.href || '';
          const enabled = (sectionDefinition.mandatory);
          const sectionData = {};
          const sectionErrors = null;
          mappedActions.push(new InitSectionAction(
            submissionId,
            sectionId,
            sectionDefinition.header,
            config,
            sectionDefinition.mandatory,
            sectionDefinition.sectionType,
            sectionDefinition.visibility,
            enabled,
            sectionData,
            sectionErrors));
        });
      mappedActions.push(new CompleteInitSubmissionFormAction(submissionId));

      const expected = cold('--(bcdefgh)', {
        b: mappedActions[0],
        c: mappedActions[1],
        d: mappedActions[2],
        e: mappedActions[3],
        f: mappedActions[4],
        g: mappedActions[5],
        h: mappedActions[6]
      });

      expect(submissionObjectEffects.loadForm$).toBeObservable(expected);
    });
  });

  describe('resetForm$', () => {
    it('should return a INIT_SUBMISSION_FORM action', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.RESET_SUBMISSION_FORM,
          payload: {
            collectionId: collectionId,
            submissionId: submissionId,
            selfUrl: selfUrl,
            submissionDefinition: submissionDefinition,
            sections: {},
            item: new Item(),
            errors: [],
          }
        }
      });

      const expected = cold('--b-', {
        b: new InitSubmissionFormAction(
          collectionId,
          submissionId,
          selfUrl,
          submissionDefinition,
          {},
          new Item(),
          null
        )
      });

      expect(submissionObjectEffects.resetForm$).toBeObservable(expected);
    });
  });

  describe('saveSubmission$', () => {
    it('should return a SAVE_SUBMISSION_FORM_SUCCESS action on success', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceType.and.returnValue(observableOf(mockSubmissionRestResponse));
      const expected = cold('--b-', {
        b: new SaveSubmissionFormSuccessAction(
          submissionId,
          mockSubmissionRestResponse as any
        )
      });

      expect(submissionObjectEffects.saveSubmission$).toBeObservable(expected);
    });

    it('should enable notifications if is manual', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM,
          payload: {
            submissionId: submissionId,
            isManual: true
          }
        }
      });

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceType.and.returnValue(observableOf(mockSubmissionRestResponse));
      const expected = cold('--b-', {
        b: new SaveSubmissionFormSuccessAction(
          submissionId,
          mockSubmissionRestResponse as any,
          true,
          true
        )
      });

      expect(submissionObjectEffects.saveSubmission$).toBeObservable(expected);
    });

    it('should disable notifications if is not manual', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM,
          payload: {
            submissionId: submissionId,
            isManual: false
          }
        }
      });

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceType.and.returnValue(observableOf(mockSubmissionRestResponse));
      const expected = cold('--b-', {
        b: new SaveSubmissionFormSuccessAction(
          submissionId,
          mockSubmissionRestResponse as any,
          false,
          false
        )
      });

      expect(submissionObjectEffects.saveSubmission$).toBeObservable(expected);
    });

    it('should return a SAVE_SUBMISSION_FORM_ERROR action on error', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceType.and.callFake(
        () => observableThrowError('Error')
      );
      const expected = cold('--b-', {
        b: new SaveSubmissionFormErrorAction(
          submissionId
        )
      });

      expect(submissionObjectEffects.saveSubmission$).toBeObservable(expected);
    });
  });

  describe('saveForLaterSubmission$', () => {
    it('should return a SAVE_FOR_LATER_SUBMISSION_FORM_SUCCESS action on success', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_FOR_LATER_SUBMISSION_FORM,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceType.and.returnValue(observableOf(mockSubmissionRestResponse));
      const expected = cold('--b-', {
        b: new SaveForLaterSubmissionFormSuccessAction(
          submissionId,
          mockSubmissionRestResponse as any
        )
      });

      expect(submissionObjectEffects.saveForLaterSubmission$).toBeObservable(expected);
    });

    it('should return a SAVE_SUBMISSION_FORM_ERROR action on error', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_FOR_LATER_SUBMISSION_FORM,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceType.and.callFake(
        () => observableThrowError('Error')
      );
      const expected = cold('--b-', {
        b: new SaveSubmissionFormErrorAction(
          submissionId
        )
      });

      expect(submissionObjectEffects.saveForLaterSubmission$).toBeObservable(expected);
    });
  });

  describe('saveSubmissionSuccess$', () => {

    it('should return a UPDATE_SECTION_DATA action for each updated section', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsData,
        errors: mockSectionsErrors
      })];
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM_SUCCESS,
          payload: {
            submissionId: submissionId,
            submissionObject: response,
            notify: true
          }
        }
      });

      const errorsList = parseSectionErrors(mockSectionsErrors);
      const expected = cold('--(bcd)-', {
        b: new UpdateSectionDataAction(
          submissionId,
          'traditionalpageone',
          mockSectionsData.traditionalpageone as any,
          errorsList.traditionalpageone || [],
          errorsList.traditionalpageone || []
        ),
        c: new UpdateSectionDataAction(
          submissionId,
          'license',
          mockSectionsData.license as any,
          errorsList.license || [],
          errorsList.license || []
        ),
        d: new UpdateSectionDataAction(
          submissionId,
          'upload',
          mockSectionsData.upload as any,
          errorsList.upload || [],
          errorsList.upload || []
        ),
      });

      expect(submissionObjectEffects.saveSubmissionSuccess$).toBeObservable(expected);
      expect(notificationsServiceStub.success).toHaveBeenCalled();

    });

    it('should not display errors when notification are disabled and field are not touched', () => {
      store.nextState({
        submission: {
          objects: submissionState
        },
        forms: {
          '2_traditionalpageone': {
            touched: {
              'dc.title': true
            }
          }
        }
      } as any);

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsData,
        errors: mockSectionsErrors
      })];
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM_SUCCESS,
          payload: {
            submissionId: submissionId,
            submissionObject: response,
            notify: false
          }
        }
      });

      const errorsToShowList = parseSectionErrors(mockSectionsErrorsTouchedField);
      const serverValidationErrorsList = parseSectionErrors(mockSectionsErrors);
      const expected = cold('--(bcd)-', {
        b: new UpdateSectionDataAction(
          submissionId,
          'traditionalpageone',
          mockSectionsData.traditionalpageone as any,
          errorsToShowList.traditionalpageone,
          serverValidationErrorsList.traditionalpageone
        ),
        c: new UpdateSectionDataAction(
          submissionId,
          'license',
          mockSectionsData.license as any,
          errorsToShowList.license || [],
          serverValidationErrorsList.license || []
        ),
        d: new UpdateSectionDataAction(
          submissionId,
          'upload',
          mockSectionsData.upload as any,
          errorsToShowList.upload || [],
          serverValidationErrorsList.upload || []
        ),
      });

      expect(submissionObjectEffects.saveSubmissionSectionSuccess$).toBeObservable(expected);
      expect(notificationsServiceStub.warning).not.toHaveBeenCalled();
    });

    it('should display a success notification', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsData
      })];
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM_SUCCESS,
          payload: {
            submissionId: submissionId,
            submissionObject: response
          }
        }
      });

      const expected = cold('--(bcd)-', {
        b: new UpdateSectionDataAction(
          submissionId,
          'traditionalpageone',
          mockSectionsData.traditionalpageone as any,
          [],
          []
        ),
        c: new UpdateSectionDataAction(
          submissionId,
          'license',
          mockSectionsData.license as any,
          [],
          []
        ),
        d: new UpdateSectionDataAction(
          submissionId,
          'upload',
          mockSectionsData.upload as any,
          [],
          []
        ),
      });

      expect(submissionObjectEffects.saveSubmissionSuccess$).toBeObservable(expected);
      expect(notificationsServiceStub.success).toHaveBeenCalled();
    });

    it('should display a warning notification when there are errors', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsData,
        errors: mockSectionsErrors
      })];
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM_SUCCESS,
          payload: {
            submissionId: submissionId,
            submissionObject: response
          }
        }
      });

      const errorsList = parseSectionErrors(mockSectionsErrors);
      const expected = cold('--(bcd)-', {
        b: new UpdateSectionDataAction(
          submissionId,
          'traditionalpageone',
          mockSectionsData.traditionalpageone as any,
          errorsList.traditionalpageone || [],
          errorsList.traditionalpageone || []
        ),
        c: new UpdateSectionDataAction(
          submissionId,
          'license',
          mockSectionsData.license as any,
          errorsList.license || [],
          errorsList.license || []
        ),
        d: new UpdateSectionDataAction(
          submissionId,
          'upload',
          mockSectionsData.upload as any,
          errorsList.upload || [],
          errorsList.upload || []
        ),
      });

      expect(submissionObjectEffects.saveSubmissionSuccess$).toBeObservable(expected);
      expect(notificationsServiceStub.warning).toHaveBeenCalled();
    });

    it('should detect and notify a new section', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsDataTwo,
        errors: mockSectionsErrors
      })];
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM_SUCCESS,
          payload: {
            submissionId: submissionId,
            submissionObject: response
          }
        }
      });

      const errorsList = parseSectionErrors(mockSectionsErrors);
      const expected = cold('--(bcde)-', {
        b: new UpdateSectionDataAction(
          submissionId,
          'traditionalpageone',
          mockSectionsDataTwo.traditionalpageone as any,
          errorsList.traditionalpageone || [],
          errorsList.traditionalpageone || []
        ),
        c: new UpdateSectionDataAction(
          submissionId,
          'traditionalpagetwo',
          mockSectionsDataTwo.traditionalpagetwo as any,
          errorsList.traditionalpagetwo || [],
          errorsList.traditionalpagetwo || []
        ),
        d: new UpdateSectionDataAction(
          submissionId,
          'license',
          mockSectionsDataTwo.license as any,
          errorsList.license || [],
          errorsList.license || []
        ),
        e: new UpdateSectionDataAction(
          submissionId,
          'upload',
          mockSectionsDataTwo.upload as any,
          errorsList.upload || [],
          errorsList.upload || []
        ),
      });

      expect(submissionObjectEffects.saveSubmissionSuccess$).toBeObservable(expected);
      expect(submissionServiceStub.notifyNewSection).toHaveBeenCalled();
    });

  });

  describe('saveSubmissionSectionSuccess$', () => {

    it('should return a UPDATE_SECTION_DATA action for each updated section', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsData,
        errors: mockSectionsErrors
      })];
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM_SUCCESS,
          payload: {
            submissionId: submissionId,
            submissionObject: response
          }
        }
      });

      const errorsList = parseSectionErrors(mockSectionsErrors);
      const expected = cold('--(bcd)-', {
        b: new UpdateSectionDataAction(
          submissionId,
          'traditionalpageone',
          mockSectionsData.traditionalpageone as any,
          [],
          errorsList.traditionalpageone
        ),
        c: new UpdateSectionDataAction(
          submissionId,
          'license',
          mockSectionsData.license as any,
          errorsList.license || [],
          errorsList.license || []
        ),
        d: new UpdateSectionDataAction(
          submissionId,
          'upload',
          mockSectionsData.upload as any,
          errorsList.upload || [],
          errorsList.upload || []
        ),
      });

      expect(submissionObjectEffects.saveSubmissionSectionSuccess$).toBeObservable(expected);

    });

    it('should not display a success notification', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsData
      })];
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM_SUCCESS,
          payload: {
            submissionId: submissionId,
            submissionObject: response
          }
        }
      });

      const expected = cold('--(bcd)-', {
        b: new UpdateSectionDataAction(
          submissionId,
          'traditionalpageone',
          mockSectionsData.traditionalpageone as any,
          [],
          []
        ),
        c: new UpdateSectionDataAction(
          submissionId,
          'license',
          mockSectionsData.license as any,
          [],
          []
        ),
        d: new UpdateSectionDataAction(
          submissionId,
          'upload',
          mockSectionsData.upload as any,
          [],
          []
        ),
      });

      expect(submissionObjectEffects.saveSubmissionSectionSuccess$).toBeObservable(expected);
      expect(notificationsServiceStub.success).not.toHaveBeenCalled();
    });

    it('should not display a warning notification when there are errors', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsData,
        errors: mockSectionsErrors
      })];
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM_SUCCESS,
          payload: {
            submissionId: submissionId,
            submissionObject: response
          }
        }
      });

      const serverValidationErrorsList = parseSectionErrors(mockSectionsErrors);
      const expected = cold('--(bcd)-', {
        b: new UpdateSectionDataAction(
          submissionId,
          'traditionalpageone',
          mockSectionsData.traditionalpageone as any,
          [],
          serverValidationErrorsList.traditionalpageone
        ),
        c: new UpdateSectionDataAction(
          submissionId,
          'license',
          mockSectionsData.license as any,
          serverValidationErrorsList.license || [],
          serverValidationErrorsList.license || []
        ),
        d: new UpdateSectionDataAction(
          submissionId,
          'upload',
          mockSectionsData.upload as any,
          serverValidationErrorsList.upload || [],
          serverValidationErrorsList.upload || []
        ),
      });

      expect(submissionObjectEffects.saveSubmissionSectionSuccess$).toBeObservable(expected);
      expect(notificationsServiceStub.warning).not.toHaveBeenCalled();
    });

    it('should detect new sections but not notify for it', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsDataTwo,
        errors: mockSectionsErrors
      })];
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM_SUCCESS,
          payload: {
            submissionId: submissionId,
            submissionObject: response,
          }
        }
      });

      const errorsList = parseSectionErrors(mockSectionsErrors);
      const expected = cold('--(bcde)-', {
        b: new UpdateSectionDataAction(
          submissionId,
          'traditionalpageone',
          mockSectionsDataTwo.traditionalpageone as any,
          [],
          errorsList.traditionalpageone
        ),
        c: new UpdateSectionDataAction(
          submissionId,
          'traditionalpagetwo',
          mockSectionsDataTwo.traditionalpagetwo as any,
          errorsList.traditionalpagetwo || [],
          errorsList.traditionalpagetwo || []
        ),
        d: new UpdateSectionDataAction(
          submissionId,
          'license',
          mockSectionsDataTwo.license as any,
          errorsList.license || [],
          errorsList.license || []
        ),
        e: new UpdateSectionDataAction(
          submissionId,
          'upload',
          mockSectionsDataTwo.upload as any,
          errorsList.upload || [],
          errorsList.upload || []
        ),
      });

      expect(submissionObjectEffects.saveSubmissionSectionSuccess$).toBeObservable(expected);
      expect(submissionServiceStub.notifyNewSection).not.toHaveBeenCalled();
    });

  });

  describe('saveSection$', () => {
    it('should return a SAVE_SUBMISSION_SECTION_FORM_SUCCESS action on success', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM,
          payload: {
            submissionId: submissionId,
            sectionId: 'traditionalpageone'
          }
        }
      });

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceID.and.returnValue(observableOf(mockSubmissionRestResponse));
      const expected = cold('--b-', {
        b: new SaveSubmissionSectionFormSuccessAction(
          submissionId,
          mockSubmissionRestResponse as any
        )
      });

      expect(submissionObjectEffects.saveSection$).toBeObservable(expected);
    });

    it('should return a SAVE_SUBMISSION_SECTION_FORM_ERROR action on error', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM,
          payload: {
            submissionId: submissionId,
            sectionId: 'traditionalpageone'
          }
        }
      });

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceID.and.callFake(
        () => observableThrowError('Error')
      );
      const expected = cold('--b-', {
        b: new SaveSubmissionSectionFormErrorAction(
          submissionId
        )
      });

      expect(submissionObjectEffects.saveSection$).toBeObservable(expected);
    });
  });

  describe('saveAndDepositSection$', () => {
    it('should return a DEPOSIT_SUBMISSION action on success', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_AND_DEPOSIT_SUBMISSION,
          payload: {
            submissionId: submissionId
          }
        }
      });

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsDataTwo
      })];

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceType.and.returnValue(observableOf(response));
      const expected = cold('--b-', {
        b: new DepositSubmissionAction(
          submissionId
        )
      });

      expect(submissionObjectEffects.saveAndDeposit$).toBeObservable(expected);
      expect(notificationsServiceStub.warning).not.toHaveBeenCalled();
    });

    it('should return a SAVE_SUBMISSION_FORM_SUCCESS action when there are errors', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_AND_DEPOSIT_SUBMISSION,
          payload: {
            submissionId: submissionId
          }
        }
      });

      const response = [Object.assign({}, mockSubmissionRestResponse[0], {
        sections: mockSectionsData,
        errors: mockSectionsErrors
      })];

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceType.and.returnValue(observableOf(response));

      const expected = cold('--b-', {
        b: new SaveSubmissionFormSuccessAction(submissionId, response as any[], false, true)
      });

      expect(submissionObjectEffects.saveAndDeposit$).toBeObservable(expected);
      expect(notificationsServiceStub.warning).toHaveBeenCalled();
    });

    it('should catch errors and return a SAVE_SUBMISSION_FORM_ERROR', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_AND_DEPOSIT_SUBMISSION,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionJsonPatchOperationsServiceStub.jsonPatchByResourceType.and.callFake(
        () => observableThrowError('Error')
      );
      const expected = cold('--b-', {
        b: new SaveSubmissionFormErrorAction(
          submissionId
        )
      });

      expect(submissionObjectEffects.saveAndDeposit$).toBeObservable(expected);
    });
  });

  describe('depositSubmission$', () => {
    it('should return a DEPOSIT_SUBMISSION_SUCCESS action on success', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.DEPOSIT_SUBMISSION,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionServiceStub.depositSubmission.and.returnValue(observableOf(mockSubmissionRestResponse));
      const expected = cold('--b-', {
        b: new DepositSubmissionSuccessAction(
          submissionId
        )
      });

      expect(submissionObjectEffects.depositSubmission$).toBeObservable(expected);
    });

    it('should return a DEPOSIT_SUBMISSION_ERROR action on error', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.DEPOSIT_SUBMISSION,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionServiceStub.depositSubmission.and.callFake(
        () => observableThrowError('Error')
      );
      const expected = cold('--b-', {
        b: new DepositSubmissionErrorAction(
          submissionId
        )
      });

      expect(submissionObjectEffects.depositSubmission$).toBeObservable(expected);
    });
  });

  describe('saveForLaterSubmissionSuccess$', () => {
    it('should display a new success notification and redirect to mydspace', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_FOR_LATER_SUBMISSION_FORM_SUCCESS,
          payload: {
            submissionId: submissionId,
            submissionObject: mockSubmissionRestResponse
          }
        }
      });

      submissionObjectEffects.saveForLaterSubmissionSuccess$.subscribe(() => {
        expect(notificationsServiceStub.success).toHaveBeenCalled();
        expect(submissionServiceStub.redirectToMyDSpace).toHaveBeenCalled();
      });
    });
  });

  describe('depositSubmissionSuccess$', () => {
    it('should display a new success notification and redirect to mydspace', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.DEPOSIT_SUBMISSION_SUCCESS,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionObjectEffects.depositSubmissionSuccess$.subscribe(() => {
        expect(notificationsServiceStub.success).toHaveBeenCalled();
        expect(submissionServiceStub.redirectToMyDSpace).toHaveBeenCalled();
      });
    });
  });

  describe('depositSubmissionError$', () => {
    it('should display a new error notification', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.DEPOSIT_SUBMISSION_ERROR,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionObjectEffects.depositSubmissionError$.subscribe(() => {
        expect(notificationsServiceStub.error).toHaveBeenCalled();
      });
    });
  });

  describe('saveError$', () => {
    it('should display a new error notification', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_FORM_ERROR,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionObjectEffects.saveError$.subscribe(() => {
        expect(notificationsServiceStub.error).toHaveBeenCalled();
      });
    });

    it('should display a new error notification', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.SAVE_SUBMISSION_SECTION_FORM_ERROR,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionObjectEffects.saveError$.subscribe(() => {
        expect(notificationsServiceStub.error).toHaveBeenCalled();
      });
    });
  });

  describe('discardSubmission$', () => {
    it('should return a DISCARD_SUBMISSION_SUCCESS action on success', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.DISCARD_SUBMISSION,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionServiceStub.discardSubmission.and.returnValue(observableOf(mockSubmissionRestResponse));
      const expected = cold('--b-', {
        b: new DiscardSubmissionSuccessAction(
          submissionId
        )
      });

      expect(submissionObjectEffects.discardSubmission$).toBeObservable(expected);
    });

    it('should return a DISCARD_SUBMISSION_ERROR action on error', () => {
      store.nextState({
        submission: {
          objects: submissionState
        }
      } as any);

      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.DISCARD_SUBMISSION,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionServiceStub.discardSubmission.and.callFake(
        () => observableThrowError('Error')
      );
      const expected = cold('--b-', {
        b: new DiscardSubmissionErrorAction(
          submissionId
        )
      });

      expect(submissionObjectEffects.discardSubmission$).toBeObservable(expected);
    });
  });

  describe('discardSubmissionSuccess$', () => {
    it('should display a new success notification and redirect to mydspace', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.DISCARD_SUBMISSION_SUCCESS,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionObjectEffects.discardSubmissionSuccess$.subscribe(() => {
        expect(notificationsServiceStub.success).toHaveBeenCalled();
        expect(submissionServiceStub.redirectToMyDSpace).toHaveBeenCalled();
      });
    });
  });

  describe('discardSubmissionError$', () => {
    it('should display a new error notification', () => {
      actions = hot('--a-', {
        a: {
          type: SubmissionObjectActionTypes.DISCARD_SUBMISSION_ERROR,
          payload: {
            submissionId: submissionId
          }
        }
      });

      submissionObjectEffects.discardSubmissionError$.subscribe(() => {
        expect(notificationsServiceStub.error).toHaveBeenCalled();
      });
    });
  });
});
