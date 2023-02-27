import { SetThemeAction } from './theme.actions';
import { themeReducer } from './theme.reducer';

describe('themeReducer', () => {
  const testState = {
    currentTheme: 'test'
  };

  it('should set the current theme in response to the SET action', () => {
    const expectedState = {
      currentTheme: 'newTheme'
    };
    const action = new SetThemeAction('newTheme');
    const newState = themeReducer(testState, action);

    expect(newState).toEqual(expectedState);
  });
});
