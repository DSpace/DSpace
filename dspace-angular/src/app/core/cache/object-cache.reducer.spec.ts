// eslint-disable-next-line import/no-namespace
import * as deepFreeze from 'deep-freeze';
import { Operation } from 'fast-json-patch';
import { Item } from '../shared/item.model';
import {
  AddDependentsObjectCacheAction,
  AddPatchObjectCacheAction,
  AddToObjectCacheAction,
  ApplyPatchObjectCacheAction,
  RemoveDependentsObjectCacheAction,
  RemoveFromObjectCacheAction,
  ResetObjectCacheTimestampsAction,
} from './object-cache.actions';

import { objectCacheReducer } from './object-cache.reducer';

class NullAction extends RemoveFromObjectCacheAction {
  type = null;
  payload = null;

  constructor() {
    super(null);
  }
}

describe('objectCacheReducer', () => {
  const requestUUID1 = '8646169a-a8fc-4b31-a368-384c07867eb1';
  const requestUUID2 = 'bd36820b-4bf7-4d58-bd80-b832058b7279';
  const selfLink1 = 'https://localhost:8080/api/core/items/1698f1d3-be98-4c51-9fd8-6bfedcbd59b7';
  const selfLink2 = 'https://localhost:8080/api/core/items/28b04544-1766-4e82-9728-c4e93544ecd3';
  const newName = 'new different name';
  const altLink1 = 'https://alternative.link/endpoint/1234';
  const altLink2 = 'https://alternative.link/endpoint/5678';
  const altLink3 = 'https://alternative.link/endpoint/9123';
  const altLink4 = 'https://alternative.link/endpoint/4567';
  const testState = {
    [selfLink1]: {
      data: {
        type: Item.type,
        self: selfLink1,
        foo: 'bar',
        _links: { self: { href: selfLink1 } },
      },
      alternativeLinks: [altLink1, altLink2],
      timeCompleted: new Date().getTime(),
      msToLive: 900000,
      requestUUIDs: [requestUUID1],
      dependentRequestUUIDs: [],
      patches: [],
      isDirty: false,
    },
    [selfLink2]: {
      data: {
        type: Item.type,
        self: selfLink2,
        foo: 'baz',
        _links: { self: { href: selfLink2 } }
      },
      alternativeLinks: [altLink3, altLink4],
      timeCompleted: new Date().getTime(),
      msToLive: 900000,
      requestUUIDs: [requestUUID2],
      dependentRequestUUIDs: [requestUUID1],
      patches: [],
      isDirty: false
    }
  };
  deepFreeze(testState);

  it('should return the current state when no valid actions have been made', () => {
    const action = new NullAction();
    const newState = objectCacheReducer(testState, action);

    expect(newState).toEqual(testState);
  });

  it('should start with an empty cache', () => {
    const action = new NullAction();
    const initialState = objectCacheReducer(undefined, action);

    expect(initialState).toEqual(Object.create(null));
  });

  it('should add the payload to the cache in response to an ADD action', () => {
    const state = Object.create(null);
    const objectToCache = { self: selfLink1, type: Item.type, _links: { self: { href: selfLink1 } } };
    const timeCompleted = new Date().getTime();
    const msToLive = 900000;
    const requestUUID = requestUUID1;
    const action = new AddToObjectCacheAction(objectToCache, timeCompleted, msToLive, requestUUID, altLink1);
    const newState = objectCacheReducer(state, action);

    expect(newState[selfLink1].data).toEqual(objectToCache);
    expect(newState[selfLink1].timeCompleted).toEqual(timeCompleted);
    expect(newState[selfLink1].msToLive).toEqual(msToLive);
    expect(newState[selfLink1].alternativeLinks.includes(altLink1)).toBeTrue();
  });

  it('should overwrite an object in the cache in response to an ADD action if it already exists', () => {
    const objectToCache = {
      self: selfLink1,
      foo: 'baz',
      somethingElse: true,
      type: Item.type,
      _links: { self: { href: selfLink1 } },
    };
    const timeCompleted = new Date().getTime();
    const msToLive = 900000;
    const requestUUID = requestUUID1;
    const action = new AddToObjectCacheAction(objectToCache, timeCompleted, msToLive, requestUUID, altLink1);
    const newState = objectCacheReducer(testState, action);

    /* eslint-disable @typescript-eslint/dot-notation */
    expect(newState[selfLink1].data['foo']).toBe('baz');
    expect(newState[selfLink1].data['somethingElse']).toBe(true);
    /* eslint-enable @typescript-eslint/dot-notation */
  });

  it('should perform the ADD action without affecting the previous state', () => {
    const state = Object.create(null);
    const objectToCache = { self: selfLink1, type: Item.type, _links: { self: { href: selfLink1 } } };
    const timeCompleted = new Date().getTime();
    const msToLive = 900000;
    const requestUUID = requestUUID1;
    const action = new AddToObjectCacheAction(objectToCache, timeCompleted, msToLive, requestUUID, altLink1);
    deepFreeze(state);

    objectCacheReducer(state, action);
  });

  it('should remove the specified object from the cache in response to the REMOVE action', () => {
    const action = new RemoveFromObjectCacheAction(selfLink1);
    const newState = objectCacheReducer(testState, action);

    expect(testState[selfLink1]).not.toBeUndefined();
    expect(newState[selfLink1]).toBeUndefined();
  });

  it('shouldn\'t do anything in response to the REMOVE action for an object that isn\'t cached', () => {
    const wrongKey = 'this isn\'t cached';
    const action = new RemoveFromObjectCacheAction(wrongKey);
    const newState = objectCacheReducer(testState, action);

    expect(testState[wrongKey]).toBeUndefined();
    expect(newState).toEqual(testState);
  });

  it('should perform the REMOVE action without affecting the previous state', () => {
    const action = new RemoveFromObjectCacheAction(selfLink1);
    // testState has already been frozen above
    objectCacheReducer(testState, action);
  });

  it('should set the timestamp of all objects in the cache in response to a RESET_TIMESTAMPS action', () => {
    const newTimestamp = new Date().getTime();
    const action = new ResetObjectCacheTimestampsAction(newTimestamp);
    const newState = objectCacheReducer(testState, action);
    Object.keys(newState).forEach((key) => {
      expect(newState[key].timeCompleted).toEqual(newTimestamp);
    });
  });

  it('should perform the RESET_TIMESTAMPS action without affecting the previous state', () => {
    const action = new ResetObjectCacheTimestampsAction(new Date().getTime());
    // testState has already been frozen above
    objectCacheReducer(testState, action);
  });

  it('should perform the ADD_PATCH action without affecting the previous state', () => {
    const action = new AddPatchObjectCacheAction(selfLink1, [{
      op: 'replace',
      path: '/name',
      value: 'random string'
    }]);
    // testState has already been frozen above
    objectCacheReducer(testState, action);
  });

  it('should when the ADD_PATCH action dispatched', () => {
    const patch = [{ op: 'add', path: '/name', value: newName } as Operation];
    const action = new AddPatchObjectCacheAction(selfLink1, patch);
    const newState = objectCacheReducer(testState, action);
    expect(newState[selfLink1].patches.map((p) => p.operations)).toContain(patch);
  });

  it('should when the APPLY_PATCH action dispatched', () => {
    const patch = [{ op: 'add', path: '/name', value: newName } as Operation];
    const addPatchAction = new AddPatchObjectCacheAction(selfLink1, patch);
    const stateWithPatch = objectCacheReducer(testState, addPatchAction);

    const action = new ApplyPatchObjectCacheAction(selfLink1);
    const newState = objectCacheReducer(stateWithPatch, action);
    expect(newState[selfLink1].patches).toEqual([]);
    expect((newState[selfLink1].data as any).name).toEqual(newName);
  });

  it('should add dependent requests on ADD_DEPENDENTS', () => {
    let newState = objectCacheReducer(testState, new AddDependentsObjectCacheAction(selfLink1, ['new', 'newer', 'newest']));
    expect(newState[selfLink1].dependentRequestUUIDs).toEqual(['new', 'newer', 'newest']);

    newState = objectCacheReducer(newState, new AddDependentsObjectCacheAction(selfLink2, ['more']));
    expect(newState[selfLink2].dependentRequestUUIDs).toEqual([requestUUID1, 'more']);
  });

  it('should clear dependent requests on REMOVE_DEPENDENTS', () => {
    let newState = objectCacheReducer(testState, new RemoveDependentsObjectCacheAction(selfLink1));
    expect(newState[selfLink1].dependentRequestUUIDs).toEqual([]);

    newState = objectCacheReducer(newState, new RemoveDependentsObjectCacheAction(selfLink2));
    expect(newState[selfLink2].dependentRequestUUIDs).toEqual([]);
  });

});
