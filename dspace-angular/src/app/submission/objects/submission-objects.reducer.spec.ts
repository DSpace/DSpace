import { submissionObjectReducer, SubmissionObjectState } from './submission-objects.reducer';
import {
  CancelSubmissionFormAction,
  ChangeSubmissionCollectionAction,
  CompleteInitSubmissionFormAction,
  DeleteSectionErrorsAction,
  DeleteUploadedFileAction,
  DepositSubmissionAction,
  DepositSubmissionErrorAction,
  DepositSubmissionSuccessAction,
  DisableSectionAction,
  DiscardSubmissionAction,
  DiscardSubmissionSuccessAction,
  EditFileDataAction,
  EnableSectionAction,
  InertSectionErrorsAction,
  InitSectionAction,
  InitSubmissionFormAction,
  NewUploadedFileAction,
  RemoveSectionErrorsAction,
  ResetSubmissionFormAction,
  SaveAndDepositSubmissionAction,
  SaveForLaterSubmissionFormAction,
  SaveForLaterSubmissionFormErrorAction,
  SaveSubmissionFormAction,
  SaveSubmissionFormErrorAction,
  SaveSubmissionFormSuccessAction,
  SaveSubmissionSectionFormAction,
  SaveSubmissionSectionFormErrorAction,
  SaveSubmissionSectionFormSuccessAction,
  SectionStatusChangeAction,
  SubmissionObjectAction,
  UpdateSectionDataAction
} from './submission-objects.actions';
import { SectionsType } from '../sections/sections-type';
import {
  mockSubmissionCollectionId,
  mockSubmissionDefinitionResponse,
  mockSubmissionId,
  mockSubmissionSelfUrl,
  mockSubmissionState
} from '../../shared/mocks/submission.mock';
import { Item } from '../../core/shared/item.model';

describe('submissionReducer test suite', () => {

  const collectionId = mockSubmissionCollectionId;
  const submissionId = mockSubmissionId;
  const submissionDefinition = mockSubmissionDefinitionResponse;
  const selfUrl = mockSubmissionSelfUrl;

  let initState: any;

  beforeEach(() => {
    initState = Object.assign({}, {}, mockSubmissionState);
  });

  it('should init submission state properly', () => {
    const expectedState = {
      826: {
        collection: collectionId,
        definition: 'traditional',
        selfUrl: selfUrl,
        activeSection: null,
        sections: Object.create(null),
        isLoading: true,
        savePending: false,
        depositPending: false,
      }
    };

    const action = new InitSubmissionFormAction(collectionId, submissionId, selfUrl, submissionDefinition, {}, new Item(), null);
    const newState = submissionObjectReducer({}, action);

    expect(newState).toEqual(expectedState);
  });

  it('should complete submission initialization', () => {
    const state = Object.assign({}, initState, {
      [submissionId]: Object.assign({}, initState[submissionId], {
        isLoading: true
      })
    });

    const action = new CompleteInitSubmissionFormAction(submissionId);
    const newState = submissionObjectReducer(state, action);

    expect(newState).toEqual(initState);
  });

  it('should reset submission state properly', () => {
    const expectedState = {
      826: {
        collection: collectionId,
        definition: 'traditional',
        selfUrl: selfUrl,
        activeSection: null,
        sections: Object.create(null),
        isLoading: true,
        savePending: false,
        depositPending: false,
      }
    };

    const action = new ResetSubmissionFormAction(collectionId, submissionId, selfUrl, {}, submissionDefinition, new Item());
    const newState = submissionObjectReducer(initState, action);

    expect(newState).toEqual(expectedState);
  });

  it('should cancel submission state properly', () => {
    const expectedState = Object.create({});

    const action = new CancelSubmissionFormAction();
    const newState = submissionObjectReducer(initState, action);

    expect(newState).toEqual(expectedState);
  });

  it('should set to true savePendig flag on save', () => {
    let action: SubmissionObjectAction = new SaveSubmissionFormAction(submissionId);
    let newState = submissionObjectReducer(initState, action);

    expect(newState[826].savePending).toBeTruthy();

    action = new SaveForLaterSubmissionFormAction(submissionId);
    newState = submissionObjectReducer(initState, action);

    expect(newState[826].savePending).toBeTruthy();

    action = new SaveAndDepositSubmissionAction(submissionId);
    newState = submissionObjectReducer(initState, action);

    expect(newState[826].savePending).toBeTruthy();

    action = new SaveSubmissionSectionFormAction(submissionId, 'traditionalpageone');
    newState = submissionObjectReducer(initState, action);

    expect(newState[826].savePending).toBeTruthy();
  });

  it('should set to false savePendig flag once the save is completed', () => {
    const state = Object.assign({}, initState, {
      [submissionId]: Object.assign({}, initState[submissionId], {
        savePending: true,
      })
    });

    let action: any = new SaveSubmissionFormSuccessAction(submissionId, []);
    let newState = submissionObjectReducer(state, action);

    expect(newState[826].savePending).toBeFalsy();

    action = new SaveSubmissionSectionFormSuccessAction(submissionId, []);
    newState = submissionObjectReducer(state, action);

    expect(newState[826].savePending).toBeFalsy();

    action = new SaveSubmissionFormErrorAction(submissionId);
    newState = submissionObjectReducer(state, action);

    expect(newState[826].savePending).toBeFalsy();

    action = new SaveForLaterSubmissionFormErrorAction(submissionId);
    newState = submissionObjectReducer(state, action);

    expect(newState[826].savePending).toBeFalsy();

    action = new SaveSubmissionSectionFormErrorAction(submissionId);
    newState = submissionObjectReducer(state, action);

    expect(newState[826].savePending).toBeFalsy();
  });

  it('should change submission collection state properly', () => {
    const newCollection = '43fe1f8c-09a6-4fcf-9c78-5d4fed8f2c8f';
    const action = new ChangeSubmissionCollectionAction('826', newCollection);
    const newState = submissionObjectReducer(initState, action);

    expect(newState[826].collection).toEqual(newCollection);
  });

  it('should set to true depositPending flag on deposit', () => {
    const action = new DepositSubmissionAction('826');
    const newState = submissionObjectReducer(initState, action);

    expect(newState[826].depositPending).toBeTruthy();
  });

  it('should reset state once the deposit is completed successfully', () => {
    const state = Object.assign({}, initState, {
      [submissionId]: Object.assign({}, initState[submissionId], {
        depositPending: true,
      })
    });

    const action: any = new DepositSubmissionSuccessAction(submissionId);
    const newState = submissionObjectReducer(state, action);

    expect(newState).toEqual({});
  });

  it('should set to false depositPending flag once the deposit is completed unsuccessfully', () => {
    const action = new DepositSubmissionErrorAction('826');
    const newState = submissionObjectReducer(initState, action);

    expect(newState[826].depositPending).toBeFalsy();
  });

  it('should return same state on discard', () => {
    const action: any = new DiscardSubmissionAction(submissionId);
    const newState = submissionObjectReducer(initState, action);

    expect(newState).toEqual(initState);
  });

  it('should reset state once the discard action is completed successfully', () => {
    const action: any = new DiscardSubmissionSuccessAction(submissionId);
    const newState = submissionObjectReducer(initState, action);

    expect(newState).toEqual({});
  });

  it('should return same state once the discard action is completed unsuccessfully', () => {
    const action: any = new DiscardSubmissionAction(submissionId);
    const newState = submissionObjectReducer(initState, action);

    expect(newState).toEqual(initState);
  });

  it('should init submission section state properly', () => {
    const expectedState = {
      header: 'submit.progressbar.describe.stepone',
      config: 'https://rest.api/dspace-spring-rest/api/config/submissionforms/traditionalpageone',
      mandatory: true,
      sectionType: 'submission-form',
      visibility: undefined,
      collapsed: false,
      enabled: true,
      data: {},
      errorsToShow: [],
      serverValidationErrors: [],
      isLoading: false,
      isValid: true
    } as any;

    let action: any = new InitSubmissionFormAction(collectionId, submissionId, selfUrl, submissionDefinition, {}, new Item(), null);
    let newState = submissionObjectReducer({}, action);

    action = new InitSectionAction(
      submissionId,
      'traditionalpageone',
      'submit.progressbar.describe.stepone',
      'https://rest.api/dspace-spring-rest/api/config/submissionforms/traditionalpageone',
      true,
      SectionsType.SubmissionForm,
      undefined,
      true,
      {},
      null);

    newState = submissionObjectReducer(newState, action);

    expect(newState[826].sections.traditionalpageone).toEqual(expectedState);
  });

  it('should enable submission section properly', () => {

    const action = new EnableSectionAction(submissionId, 'traditionalpagetwo');

    const newState = submissionObjectReducer(initState, action);

    expect(newState[826].sections.traditionalpagetwo.enabled).toBeTruthy();
  });

  it('should enable submission section properly', () => {

    let action: SubmissionObjectAction = new EnableSectionAction(submissionId, 'traditionalpagetwo');
    let newState = submissionObjectReducer(initState, action);

    action = new DisableSectionAction(submissionId, 'traditionalpagetwo');
    newState = submissionObjectReducer(newState, action);

    expect(newState[826].sections.traditionalpagetwo.enabled).toBeFalsy();
  });

  it('should set to true/false submission section status', () => {

    let action = new SectionStatusChangeAction(submissionId, 'traditionalpageone', true);
    let newState = submissionObjectReducer(initState, action);

    expect(newState[826].sections.traditionalpageone.isValid).toBeTruthy();

    action = new SectionStatusChangeAction(submissionId, 'traditionalpageone', false);
    newState = submissionObjectReducer(newState, action);

    expect(newState[826].sections.traditionalpageone.isValid).toBeFalsy();
  });

  it('should update submission section data properly', () => {
    const data = {
      'dc.contributor.author': [
        {
          value: 'Author, Test',
          language: null,
          authority: null,
          display: 'Author, Test',
          confidence: -1,
          place: 0
        }
      ],
      'dc.title': [
        {
          value: 'Title Test',
          language: null,
          authority: null,
          display: 'Title Test',
          confidence: -1,
          place: 0
        }
      ],
      'dc.date.issued': [
        {
          value: '2015',
          language: null,
          authority: null,
          display: '2015',
          confidence: -1,
          place: 0
        }
      ]
    } as any;

    const action = new UpdateSectionDataAction(submissionId, 'traditionalpageone', data, [], []);
    const newState = submissionObjectReducer(initState, action);

    expect(newState[826].sections.traditionalpageone.data).toEqual(data);
  });

  it('should update submission section metadata properly', () => {
    const data = {
    } as any;
    const metadata = ['dc.title', 'dc.contributor.author'];

    const action = new UpdateSectionDataAction(submissionId, 'traditionalpageone', data, [], [], metadata);
    const newState = submissionObjectReducer(initState, action);

    expect(newState[826].sections.traditionalpageone.metadata).toEqual(metadata);
  });

  it('should add submission section errors properly', () => {
    const errors = [
      {
        path: '/sections/license',
        message: 'error.validation.license.notgranted'
      }
    ];

    const action = new UpdateSectionDataAction(submissionId, 'traditionalpageone', {}, errors, errors);
    const newState = submissionObjectReducer(initState, action);

    expect(newState[826].sections.traditionalpageone.errorsToShow).toEqual(errors);
  });

  it('should remove all submission section errors properly', () => {
    const action: any = new RemoveSectionErrorsAction(submissionId, 'traditionalpageone');
    let newState;

    newState = submissionObjectReducer(initState, action);

    expect(newState[826].sections.traditionalpageone.errorsToShow).toEqual([]);
  });

  it('should add submission section error properly', () => {
    const error = {
      path: '/sections/traditionalpageone/dc.title/0',
      message: 'error.validation.traditionalpageone.required'
    };

    const action = new InertSectionErrorsAction(submissionId, 'traditionalpageone', error);
    const newState = submissionObjectReducer(initState, action);

    expect(newState[826].sections.traditionalpageone.errorsToShow).toEqual([error]);
  });

  it('should remove specified submission section error/s properly', () => {
    const errors = [
      {
        path: '/sections/traditionalpageone/dc.contributor.author',
        message: 'error.validation.required'
      },
      {
        path: '/sections/traditionalpageone/dc.date.issued',
        message: 'error.validation.required'
      }
    ];
    const error = {
      path: '/sections/traditionalpageone/dc.contributor.author',
      message: 'error.validation.required'
    };

    const expectedErrors = [{
      path: '/sections/traditionalpageone/dc.date.issued',
      message: 'error.validation.required'
    }];

    let action: any = new UpdateSectionDataAction(submissionId, 'traditionalpageone', {}, errors, errors);
    let newState = submissionObjectReducer(initState, action);

    action = new DeleteSectionErrorsAction(submissionId, 'traditionalpageone', error);
    newState = submissionObjectReducer(newState, action);

    expect(newState[826].sections.traditionalpageone.errorsToShow).toEqual(expectedErrors);

    action = new UpdateSectionDataAction(submissionId, 'traditionalpageone', {}, errors, errors);
    newState = submissionObjectReducer(initState, action);

    action = new DeleteSectionErrorsAction(submissionId, 'traditionalpageone', errors);
    newState = submissionObjectReducer(newState, action);

    expect(newState[826].sections.traditionalpageone.errorsToShow).toEqual([]);
  });

  it('should add a new file', () => {
    const uuid = '8cd86fba-70c8-483d-838a-70d28e7ed570';
    const fileData: any = {
      uuid: uuid,
      metadata: {
        'dc.title': [
          {
            value: '28297_389341539060_6452876_n.jpg',
            language: null,
            authority: null,
            display: '28297_389341539060_6452876_n.jpg',
            confidence: -1,
            place: 0
          }
        ]
      },
      accessConditions: [],
      format: {
        id: 16,
        shortDescription: 'JPEG',
        description: 'Joint Photographic Experts Group/JPEG File Interchange Format (JFIF)',
        mimetype: 'image/jpeg',
        supportLevel: 0,
        internal: false,
        extensions: null,
        type: 'bitstreamformat'
      },
      sizeBytes: 22737,
      checkSum: {
        checkSumAlgorithm: 'MD5',
        value: '8722864dd671912f94a999ac7c4949d2'
      },
      url: 'https://rest.api/dspace-spring-rest/api/core/bitstreams/8cd86fba-70c8-483d-838a-70d28e7ed570/content'
    };
    const expectedState = {
      files: [fileData]
    };

    const action = new NewUploadedFileAction(submissionId, 'upload', uuid, fileData);
    const newState = submissionObjectReducer(initState, action);

    expect(newState[826].sections.upload.data).toEqual(expectedState);
  });

  it('should remove a file', () => {
    const uuid = '8cd86fba-70c8-483d-838a-70d28e7ed570';
    const uuid2 = '7e2f4ba9-9316-41fd-844a-1ef435f41a42';
    const fileData: any = {
      uuid: uuid,
      metadata: {
        'dc.title': [
          {
            value: 'image_test.jpg',
            language: null,
            authority: null,
            display: 'image_test.jpg',
            confidence: -1,
            place: 0
          }
        ]
      },
      accessConditions: [],
      format: {
        id: 16,
        shortDescription: 'JPEG',
        description: 'Joint Photographic Experts Group/JPEG File Interchange Format (JFIF)',
        mimetype: 'image/jpeg',
        supportLevel: 0,
        internal: false,
        extensions: null,
        type: 'bitstreamformat'
      },
      sizeBytes: 22737,
      checkSum: {
        checkSumAlgorithm: 'MD5',
        value: '8722864dd671912f94a999ac7c4949d2'
      },
      url: 'https://rest.api/dspace-spring-rest/api/core/bitstreams/8cd86fba-70c8-483d-838a-70d28e7ed570/content'
    };
    const fileData2: any = {
      uuid: uuid2,
      metadata: {
        'dc.title': [
          {
            value: 'image_test.jpg',
            language: null,
            authority: null,
            display: 'image_test.jpg',
            confidence: -1,
            place: 0
          }
        ]
      },
      accessConditions: [],
      format: {
        id: 16,
        shortDescription: 'JPEG',
        description: 'Joint Photographic Experts Group/JPEG File Interchange Format (JFIF)',
        mimetype: 'image/jpeg',
        supportLevel: 0,
        internal: false,
        extensions: null,
        type: 'bitstreamformat'
      },
      sizeBytes: 22737,
      checkSum: {
        checkSumAlgorithm: 'MD5',
        value: '8722864dd671912f94a999ac7c4949d2'
      },
      url: 'https://rest.api/dspace-spring-rest/api/core/bitstreams/7e2f4ba9-9316-41fd-844a-1ef435f41a42/content'
    };

    const state: SubmissionObjectState = Object.assign({}, initState, {
      [submissionId]: Object.assign({}, initState[submissionId], {
        sections: Object.assign({}, initState[submissionId].sections, {
          upload: Object.assign({}, initState[submissionId].sections.upload, {
            data: {
              files: [fileData, fileData2]
            }
          })
        })
      })
    });

    const expectedState = {
      files: [fileData]
    };

    const action = new DeleteUploadedFileAction(submissionId, 'upload', uuid2);
    const newState = submissionObjectReducer(state, action);

    expect(newState[826].sections.upload.data).toEqual(expectedState);
  });

  it('should edit file data', () => {
    const uuid = '8cd86fba-70c8-483d-838a-70d28e7ed570';
    const fileData: any = {
      uuid: uuid,
      metadata: {
        'dc.title': [
          {
            value: 'image_test.jpg',
            language: null,
            authority: null,
            display: 'image_test.jpg',
            confidence: -1,
            place: 0
          }
        ]
      },
      accessConditions: [],
      format: {
        id: 16,
        shortDescription: 'JPEG',
        description: 'Joint Photographic Experts Group/JPEG File Interchange Format (JFIF)',
        mimetype: 'image/jpeg',
        supportLevel: 0,
        internal: false,
        extensions: null,
        type: 'bitstreamformat'
      },
      sizeBytes: 22737,
      checkSum: {
        checkSumAlgorithm: 'MD5',
        value: '8722864dd671912f94a999ac7c4949d2'
      },
      url: 'https://rest.api/dspace-spring-rest/api/core/bitstreams/8cd86fba-70c8-483d-838a-70d28e7ed570/content'
    };
    const fileData2: any = {
      uuid: uuid,
      metadata: {
        'dc.title': [
          {
            value: 'New title',
            language: null,
            authority: null,
            display: 'New title',
            confidence: -1,
            place: 0
          }
        ]
      },
      accessConditions: [],
      format: {
        id: 16,
        shortDescription: 'JPEG',
        description: 'Joint Photographic Experts Group/JPEG File Interchange Format (JFIF)',
        mimetype: 'image/jpeg',
        supportLevel: 0,
        internal: false,
        extensions: null,
        type: 'bitstreamformat'
      },
      sizeBytes: 22737,
      checkSum: {
        checkSumAlgorithm: 'MD5',
        value: '8722864dd671912f94a999ac7c4949d2'
      },
      url: 'https://rest.api/dspace-spring-rest/api/core/bitstreams/7e2f4ba9-9316-41fd-844a-1ef435f41a42/content'
    };

    const state: SubmissionObjectState = Object.assign({}, initState, {
      [submissionId]: Object.assign({}, initState[submissionId], {
        sections: Object.assign({}, initState[submissionId].sections, {
          upload: Object.assign({}, initState[submissionId].sections.upload, {
            data: {
              files: [fileData]
            }
          })
        })
      })
    });

    const expectedState = {
      files: [fileData2]
    };

    const action = new EditFileDataAction(submissionId, 'upload', uuid, fileData2);
    const newState = submissionObjectReducer(state, action);

    expect(newState[826].sections.upload.data).toEqual(expectedState);
  });

});
