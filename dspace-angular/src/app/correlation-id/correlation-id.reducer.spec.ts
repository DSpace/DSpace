import { correlationIdReducer } from './correlation-id.reducer';
import { SetCorrelationIdAction } from './correlation-id.actions';

describe('correlationIdReducer', () => {
  it('should set the correlatinId with SET action', () => {
    const initialState = null;
    const currentState = correlationIdReducer(initialState, new SetCorrelationIdAction('new ID'));

    expect(currentState).toBe('new ID');
  });

  it('should leave correlatinId unchanged otherwise', () => {
    const initialState = null;

    let currentState = correlationIdReducer(initialState, { type: 'unknown' } as any);
    expect(currentState).toBe(null);

    currentState = correlationIdReducer(currentState, new SetCorrelationIdAction('new ID'));
    currentState = correlationIdReducer(currentState, { type: 'unknown' } as any);

    expect(currentState).toBe('new ID');
  });
});
