import { TestBed } from '@angular/core/testing';

import { provideMockActions } from '@ngrx/effects/testing';
import { Store, StoreModule } from '@ngrx/store';
import { cold, hot } from 'jasmine-marbles';
import { Observable, of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { getMockRequestService } from '../../shared/mocks/request.service.mock';
import { StoreMock } from '../../shared/testing/store.mock';
import { RequestService } from '../data/request.service';
import { RestRequestMethod } from '../data/rest-request-method';
import { DSpaceObject } from '../shared/dspace-object.model';
import { ApplyPatchObjectCacheAction } from './object-cache.actions';
import { ObjectCacheService } from './object-cache.service';
import {
  CommitSSBAction,
  EmptySSBAction,
  ServerSyncBufferActionTypes
} from './server-sync-buffer.actions';
import { ServerSyncBufferEffects } from './server-sync-buffer.effects';
import { storeModuleConfig } from '../../app.reducer';
import { NoOpAction } from '../../shared/ngrx/no-op.action';

describe('ServerSyncBufferEffects', () => {
  let ssbEffects: ServerSyncBufferEffects;
  let actions: Observable<any>;
  let testScheduler: TestScheduler;
  const testConfig = {
    cache:
      {
        autoSync:
          {
            timePerMethod: {},
            defaultTime: 0
          }
      }
  };
  const selfLink = 'https://rest.api/endpoint/1698f1d3-be98-4c51-9fd8-6bfedcbd59b7';
  let store;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot({}, storeModuleConfig),
      ],
      providers: [
        ServerSyncBufferEffects,
        provideMockActions(() => actions),
        { provide: RequestService, useValue: getMockRequestService() },
        {
          provide: ObjectCacheService, useValue: {
            getObjectBySelfLink: (link) => {
              const object = Object.assign(new DSpaceObject(), {
                _links: { self: { href: link } }
              });
              return observableOf(object);
            },
            getByHref: (link) => {
              const object = Object.assign(new DSpaceObject(), {
                _links: {
                  self: { href: link }
                }
              });
              return observableOf(object);
            }
          }
        },
        { provide: Store, useClass: StoreMock }
        // other providers
      ],
    });

    store = TestBed.inject(Store);
    ssbEffects = TestBed.inject(ServerSyncBufferEffects);
  });

  describe('setTimeoutForServerSync', () => {
    beforeEach(() => {
      testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });
    });

    it('should return a COMMIT action in response to an ADD action', () => {
      // eslint-disable-next-line @typescript-eslint/no-shadow
      testScheduler.run(({ hot, expectObservable }) => {
        actions = hot('a', {
          a: {
            type: ServerSyncBufferActionTypes.ADD,
            payload: { href: selfLink, method: RestRequestMethod.PUT }
          }
        });

        expectObservable(ssbEffects.setTimeoutForServerSync).toBe('b', {
          b: new CommitSSBAction(RestRequestMethod.PUT)
        });
      });
    });
  });

  describe('commitServerSyncBuffer', () => {
    describe('when the buffer is not empty', () => {
      beforeEach(() => {
        store
          .subscribe((state) => {
            (state as any).core = Object({});
            (state as any).core['cache/syncbuffer'] = {
              buffer: [{
                href: selfLink,
                method: RestRequestMethod.PATCH
              }]
            };
          });
      });
      it('should return a list of actions in response to a COMMIT action', () => {
        actions = hot('a', {
          a: {
            type: ServerSyncBufferActionTypes.COMMIT,
            payload: RestRequestMethod.PATCH
          }
        });

        const expected = cold('(bc)', {
          b: new ApplyPatchObjectCacheAction(selfLink),
          c: new EmptySSBAction(RestRequestMethod.PATCH)
        });

        expect(ssbEffects.commitServerSyncBuffer).toBeObservable(expected);
      });
    });

    describe('when the buffer is empty', () => {
      beforeEach(() => {
        store
          .subscribe((state) => {
            (state as any).core = Object({});
            (state as any).core['cache/syncbuffer'] = {
              buffer: []
            };
          });
      });
      it('should return a placeholder action in response to a COMMIT action', () => {
        store.subscribe();
        actions = hot('a', {
          a: {
            type: ServerSyncBufferActionTypes.COMMIT,
            payload: { method: RestRequestMethod.PATCH }
          }
        });
        const expected = cold('b', { b: new NoOpAction() });

        expect(ssbEffects.commitServerSyncBuffer).toBeObservable(expected);
      });
    });
  });
});
