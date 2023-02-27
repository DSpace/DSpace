import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';
import { provideMockActions } from '@ngrx/effects/testing';
import { cold, hot } from 'jasmine-marbles';
import { ObjectCacheEffects } from './object-cache.effects';
import { ResetObjectCacheTimestampsAction } from './object-cache.actions';
import { StoreActionTypes } from '../../store.actions';

describe('ObjectCacheEffects', () => {
  let cacheEffects: ObjectCacheEffects;
  let actions: Observable<any>;
  const timestamp = 10000;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ObjectCacheEffects,
        provideMockActions(() => actions),
        // other providers
      ],
    });

    cacheEffects = TestBed.inject(ObjectCacheEffects);
  });

  describe('fixTimestampsOnRehydrate$', () => {

    it('should return a RESET_TIMESTAMPS action in response to a REHYDRATE action', () => {
      spyOn(Date.prototype, 'getTime').and.callFake(() => {
        return timestamp;
      });
      actions = hot('--a-', { a: { type: StoreActionTypes.REHYDRATE, payload: {} } });

      const expected = cold('--b-', { b: new ResetObjectCacheTimestampsAction(new Date().getTime()) });

      expect(cacheEffects.fixTimestampsOnRehydrate).toBeObservable(expected);
    });
  });
});
