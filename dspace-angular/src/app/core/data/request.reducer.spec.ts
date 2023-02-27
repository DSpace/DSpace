// eslint-disable-next-line import/no-namespace
import * as deepFreeze from 'deep-freeze';
import {
  RequestConfigureAction,
  RequestErrorAction,
  RequestExecuteAction,
  RequestRemoveAction,
  RequestStaleAction,
  RequestSuccessAction,
  ResetResponseTimestampsAction
} from './request.actions';
import { GetRequest } from './request.models';
import { requestReducer} from './request.reducer';
import { RequestEntryState } from './request-entry-state.model';
import { RequestState } from './request-state.model';

class NullAction extends RequestSuccessAction {
  type = null;
  payload = null;

  constructor() {
    super(null, null);
  }
}

describe('requestReducer', () => {
  const id1 = 'clients/eca2ea1d-6a6a-4f62-8907-176d5fec5014';
  const id2 = 'clients/eb7cde2e-a03f-4f0b-ac5d-888a4ef2b4eb';
  const link1 = 'https://dspace7.4science.it/dspace-spring-rest/api/core/items/567a639f-f5ff-4126-807c-b7d0910808c8';
  const link2 = 'https://dspace7.4science.it/dspace-spring-rest/api/core/items/1911e8a4-6939-490c-b58b-a5d70f8d91fb';
  const testInitState: RequestState = {
    [id1]: {
      request: new GetRequest(id1, link1),
      state: RequestEntryState.RequestPending,
      response: undefined,
      lastUpdated: undefined
    }
  };
  const testSuccessState = {
    [id1]: {
      state: RequestEntryState.Success,
      lastUpdated: 0
    }
  };
  const testErrorState = {
    [id1]: {
      state: RequestEntryState.Error,
      lastUpdated: 0
    }
  };
  deepFreeze(testInitState);
  deepFreeze(testSuccessState);
  deepFreeze(testErrorState);

  it('should return the current state when no valid actions have been made', () => {
    const action = new NullAction();
    const newState = requestReducer(testInitState, action);

    expect(newState).toEqual(testInitState);
  });

  it('should start with an empty state', () => {
    const action = new NullAction();
    const initialState = requestReducer(undefined, action);

    expect(initialState).toEqual(Object.create(null));
  });

  it('should add the new RestRequest and set state to RequestPending for the given RestRequest in the state, in response to a CONFIGURE action', () => {
    const state = testInitState;
    const request = new GetRequest(id2, link2);

    const action = new RequestConfigureAction(request);
    const newState = requestReducer(state, action);

    expect(newState[id2].request.uuid).toEqual(id2);
    expect(newState[id2].request.href).toEqual(link2);
    expect(newState[id2].state).toEqual(RequestEntryState.RequestPending);
    expect(newState[id2].response).toBeNull();
  });

  it('should set state to ResponsePending for the given RestRequest in the state, in response to an EXECUTE action', () => {
    const state = testInitState;

    const action = new RequestExecuteAction(id1);
    const newState = requestReducer(state, action);

    expect(newState[id1].request.uuid).toEqual(id1);
    expect(newState[id1].request.href).toEqual(link1);
    expect(newState[id1].state).toEqual(RequestEntryState.ResponsePending);
    expect(newState[id1].response).toEqual(undefined);
  });

  it('should set state to Success for the given RestRequest in the state, in response to a SUCCESS action', () => {
    const state = testInitState;

    const action = new RequestSuccessAction(id1, 200);
    const newState = requestReducer(state, action);

    expect(newState[id1].request.uuid).toEqual(id1);
    expect(newState[id1].request.href).toEqual(link1);
    expect(newState[id1].state).toEqual(RequestEntryState.Success);
    expect(newState[id1].response.statusCode).toEqual(200);
  });

  it('should set state to Error for the given RestRequest in the state, in response to an ERROR action', () => {
    const state = testInitState;

    const action = new RequestErrorAction(id1, 404, 'Not Found');
    const newState = requestReducer(state, action);

    expect(newState[id1].request.uuid).toEqual(id1);
    expect(newState[id1].request.href).toEqual(link1);
    expect(newState[id1].state).toEqual(RequestEntryState.Error);
    expect(newState[id1].response.statusCode).toEqual(404);
    expect(newState[id1].response.errorMessage).toEqual('Not Found');
  });

  it('should update the response\'s timeCompleted for the given RestRequest in the state, in response to a RESET_TIMESTAMPS action', () => {
    const update = Object.assign({}, testInitState[id1], {
      response: {
        timeCompleted: 10,
        statusCode: 200
      }
    });
    const state = Object.assign({}, testInitState, { [id1]: update });
    const timeStamp = 1000;
    const action = new ResetResponseTimestampsAction(timeStamp);
    const newState = requestReducer(state, action);

    expect(newState[id1].request.uuid).toEqual(state[id1].request.uuid);
    expect(newState[id1].request.href).toEqual(state[id1].request.href);
    expect(newState[id1].state).toEqual(state[id1].state);
    expect(newState[id1].response.statusCode).toEqual(update.response.statusCode);
    expect(newState[id1].response.timeCompleted).toBe(timeStamp);
    expect(newState[id1].lastUpdated).toBe(timeStamp);
  });

  it('should remove the correct request, in response to a REMOVE action', () => {
    const state = testInitState;

    const action = new RequestRemoveAction(id1);
    const newState = requestReducer(state, action);

    expect(newState[id1]).toBeNull();
  });

  describe(`for an entry with state: Success`, () => {
    it(`should set the state to SuccessStale, in response to a STALE action`, () => {
      const state = testSuccessState;

      const action = new RequestStaleAction(id1);
      const newState = requestReducer(state, action);

      expect(newState[id1].state).toEqual(RequestEntryState.SuccessStale);
      expect(newState[id1].lastUpdated).toBe(action.lastUpdated);
    });
  });

  describe(`for an entry with state: Error`, () => {
    it(`should set the state to ErrorStale, in response to a STALE action`, () => {
      const state = testErrorState;

      const action = new RequestStaleAction(id1);
      const newState = requestReducer(state, action);

      expect(newState[id1].state).toEqual(RequestEntryState.ErrorStale);
      expect(newState[id1].lastUpdated).toBe(action.lastUpdated);
    });
  });

});
