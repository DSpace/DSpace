import { TestBed } from '@angular/core/testing';

import { cold, hot } from 'jasmine-marbles';
import { provideMockActions } from '@ngrx/effects/testing';
import { Store } from '@ngrx/store';
import { Observable, of as observableOf } from 'rxjs';

import { JsonPatchOperationsEffects } from './json-patch-operations.effects';
import { JsonPatchOperationsState } from './json-patch-operations.reducer';

import { FlushPatchOperationsAction, JsonPatchOperationsActionTypes } from './json-patch-operations.actions';

describe('JsonPatchOperationsEffects test suite', () => {
  let jsonPatchOperationsEffects: JsonPatchOperationsEffects;
  let actions: Observable<any>;
  const store: Store<JsonPatchOperationsState> = jasmine.createSpyObj('store', {
    /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
    dispatch: {},
    /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
    select: observableOf(true)
  });
  const testJsonPatchResourceType = 'testResourceType';
  const testJsonPatchResourceId = 'testResourceId';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        JsonPatchOperationsEffects,
        { provide: Store, useValue: store },
        provideMockActions(() => actions),
        // other providers
      ],
    });

    jsonPatchOperationsEffects = TestBed.inject(JsonPatchOperationsEffects);
  });

  describe('commit$', () => {
    it('should return a FLUSH_JSON_PATCH_OPERATIONS action in response to a COMMIT_JSON_PATCH_OPERATIONS action', () => {
      actions = hot('--a-', {
        a: {
          type: JsonPatchOperationsActionTypes.COMMIT_JSON_PATCH_OPERATIONS,
          payload: { resourceType: testJsonPatchResourceType, resourceId: testJsonPatchResourceId }
        }
      });

      const expected = cold('--b-', {
        b: new FlushPatchOperationsAction(testJsonPatchResourceType, testJsonPatchResourceId)
      });

      expect(jsonPatchOperationsEffects.commit$).toBeObservable(expected);
    });
  });
});
