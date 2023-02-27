import { ThemeAction, ThemeActionTypes } from './theme.actions';

export interface ThemeState {
  currentTheme: string;
}

const initialState: ThemeState = {
  currentTheme: null
};

export function themeReducer(state: ThemeState = initialState, action: ThemeAction): ThemeState {
  switch (action.type) {
    case ThemeActionTypes.SET: {
      return {
        currentTheme: action.payload.name
      };
    }
    default: {
      return state;
    }
  }
}
