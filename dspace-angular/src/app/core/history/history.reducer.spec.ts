import { AddUrlToHistoryAction } from './history.actions';
import { historyReducer } from './history.reducer';

class NullAction extends AddUrlToHistoryAction {
  type = null;

  constructor() {
    super('test');
  }
}

describe('historyReducer', () => {

  const emptyState = [];
  const state = ['testUrl', 'testUrl2', 'testUrl3'];

  it('should return the current state when no valid actions have been made', () => {
    const action = new NullAction();
    const newState = historyReducer(emptyState, action);

    expect(newState).toEqual(emptyState);
  });

  it('should add url to history', () => {
    let action = new AddUrlToHistoryAction('testUrl');
    let newState = historyReducer(emptyState, action);

    action = new AddUrlToHistoryAction('testUrl2');
    newState = historyReducer(newState, action);

    action = new AddUrlToHistoryAction('testUrl3');
    newState = historyReducer(newState, action);

    expect(newState).toEqual(state);
  });

});
