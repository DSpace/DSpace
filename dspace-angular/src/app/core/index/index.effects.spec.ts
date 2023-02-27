import { Observable } from 'rxjs';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { UUIDIndexEffects } from './index.effects';
import { cold, hot } from 'jasmine-marbles';
import { AddToObjectCacheAction } from '../cache/object-cache.actions';
import { Item } from '../shared/item.model';
import { AddToIndexAction } from './index.actions';
import { provideMockStore } from '@ngrx/store/testing';
import { NoOpAction } from '../../shared/ngrx/no-op.action';
import { IndexName } from './index-name.model';

describe('ObjectUpdatesEffects', () => {
  let indexEffects: UUIDIndexEffects;
  let actions: Observable<any>;
  let objectToCache;
  let timeCompleted;
  let msToLive;
  let requestUUID;
  let alternativeLink;
  let selfLink;
  let otherLink;
  let initialState;

  function init() {
    selfLink = 'rest.org/items/6ca6549c-3db2-4288-8ce4-4a3bce011860';
    otherLink = 'rest.org/items/51b682e7-7c48-402b-b2b4-ff163f399180';
    objectToCache = {
      type: Item.type,
      uuid: '3601eaed-8fdb-487f-8b89-4d7647314143',
      _links: {
        self: { href: selfLink },
        anotherLink: { href: otherLink }
      }
    };
    timeCompleted = new Date().getDate();
    msToLive = 90000;
    requestUUID = '324e5a5c-06f7-428d-b3ba-cc322c5dde39';
    alternativeLink = 'rest.org/alternative-link/1234';
    initialState = {
      core: {
        index: {
          [IndexName.REQUEST]: {
            [selfLink]: requestUUID
          }
        }
      }
    };
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      providers: [
        UUIDIndexEffects,
        provideMockActions(() => actions),
        provideMockStore({ initialState }),
      ],
    });
  }));

  beforeEach(() => {
    indexEffects = TestBed.get(UUIDIndexEffects);
  });

  describe('addAlternativeObjectLink$', () => {
    let action;
    it('should emit a new ADD_TO_INDEX action when a AddToObjectCacheAction with alternativeLink is dispatched', () => {
      action = new AddToObjectCacheAction(objectToCache, timeCompleted, msToLive, requestUUID, alternativeLink);
      const newAction = new AddToIndexAction(
        IndexName.ALTERNATIVE_OBJECT_LINK,
        alternativeLink,
        objectToCache._links.self.href
      );
      actions = hot('--a-', { a: action });
      const expected = cold('--b-', { b: newAction });
      expect(indexEffects.addAlternativeObjectLink$).toBeObservable(expected);
    });

    it('should emit NO_ACTION when a AddToObjectCacheAction without an alternativeLink is dispatched', () => {
      action = new AddToObjectCacheAction(objectToCache, timeCompleted, msToLive, requestUUID, undefined);
      actions = hot('--a-', { a: action });
      const expected = cold('--b-', { b: new NoOpAction() });
      expect(indexEffects.addAlternativeObjectLink$).toBeObservable(expected);
    });

    it('should emit NO_ACTION when a AddToObjectCacheAction with an alternativeLink that\'s the same as the objectToCache\'s selfLink is dispatched', () => {
      action = new AddToObjectCacheAction(objectToCache, timeCompleted, msToLive, requestUUID, objectToCache._links.self.href);
      actions = hot('--a-', { a: action });
      const expected = cold('--b-', { b: new NoOpAction() });
      expect(indexEffects.addAlternativeObjectLink$).toBeObservable(expected);
    });
  });
});
