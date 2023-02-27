// eslint-disable-next-line import/no-namespace
import * as deepFreeze from 'deep-freeze';
import { RemoveFromObjectCacheAction } from './object-cache.actions';
import { serverSyncBufferReducer } from './server-sync-buffer.reducer';
import { RestRequestMethod } from '../data/rest-request-method';
import { AddToSSBAction, EmptySSBAction } from './server-sync-buffer.actions';

class NullAction extends RemoveFromObjectCacheAction {
  type = null;
  payload = null;

  constructor() {
    super(null);
  }
}

describe('serverSyncBufferReducer', () => {
  const selfLink1 = 'https://localhost:8080/api/core/items/1698f1d3-be98-4c51-9fd8-6bfedcbd59b7';
  const selfLink2 = 'https://localhost:8080/api/core/items/28b04544-1766-4e82-9728-c4e93544ecd3';
  const testState = {
    buffer:
      [
        {
          href: selfLink1,
          method: RestRequestMethod.PATCH,
        },
        {
          href: selfLink2,
          method: RestRequestMethod.GET,
        }
      ]
  };
  const newSelfLink = 'https://localhost:8080/api/core/items/1ce6b5ae-97e1-4e5a-b4b0-f9029bad10c0';

  deepFreeze(testState);

  it('should return the current state when no valid actions have been made', () => {
    const action = new NullAction();
    const newState = serverSyncBufferReducer(testState, action);

    expect(newState).toEqual(testState);
  });

  it('should start with an empty buffer array', () => {
    const action = new NullAction();
    const initialState = serverSyncBufferReducer(undefined, action);

    expect(initialState).toEqual({ buffer: [] });
  });

  it('should perform the ADD action without affecting the previous state', () => {
    const action = new AddToSSBAction(selfLink1, RestRequestMethod.POST);
    // testState has already been frozen above
    serverSyncBufferReducer(testState, action);
  });

  it('should perform the EMPTY action without affecting the previous state', () => {
    const action = new EmptySSBAction();
    // testState has already been frozen above
    serverSyncBufferReducer(testState, action);
  });

  it('should empty the buffer if the EmptySSBAction is dispatched without a payload', () => {
    const action = new EmptySSBAction();
    // testState has already been frozen above
    const emptyState = serverSyncBufferReducer(testState, action);
    expect(emptyState).toEqual({ buffer: [] });
  });

  it('should empty the buffer partially if the EmptySSBAction is dispatched with a payload', () => {
    const action = new EmptySSBAction(RestRequestMethod.PATCH);
    // testState has already been frozen above
    const emptyState = serverSyncBufferReducer(testState, action);
    expect(emptyState).toEqual({ buffer: testState.buffer.filter((entry) => entry.method !== RestRequestMethod.PATCH) });
  });

  it('should add an entry to the buffer if the AddSSBAction is dispatched', () => {
    const action = new AddToSSBAction(newSelfLink, RestRequestMethod.PUT);
    // testState has already been frozen above
    const newState = serverSyncBufferReducer(testState, action);
    expect(newState.buffer).toContain({
      href: newSelfLink, method: RestRequestMethod.PUT
    })
    ;
  });
});
