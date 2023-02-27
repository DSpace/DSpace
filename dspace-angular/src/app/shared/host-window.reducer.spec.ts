// eslint-disable-next-line import/no-namespace
import * as deepFreeze from 'deep-freeze';
import { hostWindowReducer } from './search/host-window.reducer';
import { HostWindowResizeAction } from './host-window.actions';

class NullAction extends HostWindowResizeAction {
  type = null;

  constructor() {
    super(0, 0);
  }
}

describe('hostWindowReducer', () => {

  it('should return the current state when no valid actions have been made', () => {
    const state = { width: 800, height: 600 };
    const action = new NullAction();
    const newState = hostWindowReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with width = null and height = null', () => {
    const action = new NullAction();
    const initialState = hostWindowReducer(undefined, action);

    expect(initialState.width).toEqual(null);
    expect(initialState.height).toEqual(null);
  });

  it('should update the width and height in the state in response to a RESIZE action', () => {
    const state = { width: 800, height: 600 };
    const action = new HostWindowResizeAction(1024, 768);
    const newState = hostWindowReducer(state, action);

    expect(newState.width).toEqual(1024);
    expect(newState.height).toEqual(768);
  });

  it('should perform the RESIZE action without affecting the previous state', () => {
    const state = { width: 800, height: 600 };
    deepFreeze(state);

    const action = new HostWindowResizeAction(1024, 768);
    hostWindowReducer(state, action);
  });

});
