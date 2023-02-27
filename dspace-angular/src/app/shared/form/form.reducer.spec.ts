import { formReducer } from './form.reducer';
import {
  FormAddError,
  FormAddTouchedAction,
  FormChangeAction,
  FormClearErrorsAction,
  FormInitAction,
  FormRemoveAction,
  FormRemoveErrorAction,
  FormStatusChangeAction
} from './form.actions';

describe('formReducer', () => {

  it('should set init state of the form', () => {
    const state = {
      testForm: {
        data: {
          author: null,
          title: null,
          date: null,
          description: null
        },
        valid: false,
        errors: [],
        touched: {}
      }
    };
    const formId = 'testForm';
    const formData = {
      author: null,
      title: null,
      date: null,
      description: null
    };
    const valid = false;
    const action = new FormInitAction(formId, formData, valid);
    const newState = formReducer({}, action);

    expect(newState).toEqual(state);
  });

  it('should update state of the form when it\'s already present', () => {
    const initState = {
      testForm: {
        data: {
          author: null,
          title: null,
          date: null,
          description: null
        },
        valid: false,
        errors: [],
        touched: {}
      }
    };
    const formId = 'testForm';
    const formData = {
      author: null,
      title: 'title',
      date: null,
      description: null
    };
    const state = {
      testForm: {
        data: {
          author: null,
          title: 'title',
          date: null,
          description: null
        },
        valid: false,
        errors: [],
        touched: {}
      }
    };

    const valid = false;
    const action = new FormInitAction(formId, formData, valid);
    const newState = formReducer(initState, action);

    expect(newState).toEqual(state);
  });

  it('should change form data on form change', () => {
    const initState = {
      testForm: {
        data: {
          author: null,
          title: null,
          date: null,
          description: null
        },
        valid: false,
        errors: [],
        touched: {}
      }
    };
    const state = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: false,
        errors: [],
        touched: {}
      }
    };
    const formId = 'testForm';
    const formData = {
      author: null,
      title: ['test'],
      date: null,
      description: null
    };

    const action = new FormChangeAction(formId, formData);
    const newState = formReducer(initState, action);

    expect(newState).toEqual(state);
  });

  it('should change form status on form status change', () => {
    const initState = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: false,
        errors: [],
        touched: {}
      }
    };
    const state = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: true,
        errors: [],
        touched: {}
      }
    };
    const formId = 'testForm';

    const action = new FormStatusChangeAction(formId, true);
    const newState = formReducer(initState, action);

    expect(newState).toEqual(state);
  });

  it('should add error to form state', () => {
    const initState = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: true,
        errors: [],
        touched: {}
      }
    };

    const expectedErrors = [
      {
        fieldId: 'title',
        fieldIndex: 0,
        message: 'Not valid'
      }
    ];

    const formId = 'testForm';
    const fieldId = 'title';
    const fieldIndex = 0;
    const message = 'Not valid';

    const action = new FormAddError(formId, fieldId, fieldIndex, message);
    const newState = formReducer(initState, action);

    expect(newState.testForm.errors).toEqual(expectedErrors);
  });

  it('should remove errors from field', () => {
    const initState = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: true,
        errors: [
          {
            fieldId: 'author',
            fieldIndex: 0,
            message: 'error.validation.required'
          },
          {
            fieldId: 'title',
            fieldIndex: 0,
            message: 'error.validation.required'
          }
        ],
        touched: {}
      }
    };

    const expectedErrors = [
      {
        fieldId: 'title',
        fieldIndex: 0,
        message: 'error.validation.required'
      }
    ];

    const formId = 'testForm';
    const fieldId = 'author';
    const fieldIndex = 0;

    const action = new FormRemoveErrorAction(formId, fieldId, fieldIndex);
    const newState = formReducer(initState, action);

    expect(newState.testForm.errors).toEqual(expectedErrors);
  });

  it('should remove form state', () => {
    const initState = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: true,
        errors: [],
        touched: {}
      }
    };

    const formId = 'testForm';

    const action = new FormRemoveAction(formId);
    const newState = formReducer(initState, action);

    expect(newState).toEqual({});
  });

  it('should clear form errors', () => {
    const initState = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: true,
        errors: [
          {
            fieldId: 'author',
            fieldIndex: 0,
            message: 'error.validation.required'
          }
        ],
        touched: {}
      }
    };

    const formId = 'testForm';

    const action = new FormClearErrorsAction(formId);
    const newState = formReducer(initState, action);

    expect(newState.testForm.errors).toEqual([]);
  });

  it('should set new touched field to the form state', () => {
    const initState = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: false,
        errors: [],
        touched: {}
      }
    };
    const state = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: false,
        errors: [],
        touched: {
          title: true
        }
      }
    };
    const formId = 'testForm';
    const touched = ['title'];

    const action = new FormAddTouchedAction(formId, touched);
    const newState = formReducer(initState, action);

    expect(newState).toEqual(state);
  });

  it('should add new touched field to the form state', () => {
    const initState = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: false,
        errors: [],
        touched: {
          title: true
        }
      }
    };
    const state = {
      testForm: {
        data: {
          author: null,
          title: ['test'],
          date: null,
          description: null
        },
        valid: false,
        errors: [],
        touched: {
          title: true,
          author: true
        }
      }
    };
    const formId = 'testForm';
    const touched = ['author'];

    const action = new FormAddTouchedAction(formId, touched);
    const newState = formReducer(initState, action);

    expect(newState).toEqual(state);
  });

});
