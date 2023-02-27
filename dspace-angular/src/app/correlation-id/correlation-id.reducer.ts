import { CorrelationIdAction, CorrelationIDActionTypes, SetCorrelationIdAction } from './correlation-id.actions';

const initialState = null;

/**
 * Reducer that handles actions to update the correlation ID
 * @param {string} state                the previous correlation ID (null if unset)
 * @param {CorrelationIdAction} action  the action to perform
 * @return {string}                     the new correlation ID
 */
export const correlationIdReducer = (state = initialState, action: CorrelationIdAction): string => {
  switch (action.type) {
    case CorrelationIDActionTypes.SET: {
      return (action as SetCorrelationIdAction).payload;
    }
    default: {
      return state;
    }
  }
};
