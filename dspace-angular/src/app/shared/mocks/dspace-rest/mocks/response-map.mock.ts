import { InjectionToken } from '@angular/core';
// import mockSubmissionResponse from './mock-submission-response.json';
// import mockPublicationResponse from './mock-publication-response.json';
// import mockUntypedItemResponse from './mock-untyped-item-response.json';

export class ResponseMapMock extends Map<string, any> {}

export const MOCK_RESPONSE_MAP: InjectionToken<ResponseMapMock> = new InjectionToken<ResponseMapMock>('mockResponseMap');

/**
 * List of endpoints with their matching mock response
 * Note that this list is only used in development mode
 * In production the actual endpoints on the REST server will be called
 */
export const mockResponseMap: ResponseMapMock = new Map([
  // [ '/config/submissionforms/traditionalpageone', mockSubmissionResponse ]
  // [ '/api/pid/find', mockPublicationResponse ],
  // [ '/api/pid/find', mockUntypedItemResponse ],
]);
