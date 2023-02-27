  /* eslint-disable max-classes-per-file */
import { getComponentByWorkflowTaskOption, rendersWorkflowTaskOption } from './claimed-task-actions-decorator';

describe('ClaimedTaskActions decorator function', () => {
  const option1 = 'test_option_1';
  const option2 = 'test_option_2';
  const option3 = 'test_option_3';

  class Test1Action {
  }

  class Test2Action {
  }

  class Test3Action {
  }

  /* eslint-enable max-classes-per-file */

  beforeAll(() => {
    rendersWorkflowTaskOption(option1)(Test1Action);
    rendersWorkflowTaskOption(option2)(Test2Action);
    rendersWorkflowTaskOption(option3)(Test3Action);
  });

  describe('If there\'s an exact match', () => {
    it('should return the matching class', () => {
      const component = getComponentByWorkflowTaskOption(option1);
      expect(component).toEqual(Test1Action);

      const component2 = getComponentByWorkflowTaskOption(option2);
      expect(component2).toEqual(Test2Action);

      const component3 = getComponentByWorkflowTaskOption(option3);
      expect(component3).toEqual(Test3Action);
    });
  });

  describe('If there\'s no match', () => {
    it('should return unidentified', () => {
      const component = getComponentByWorkflowTaskOption('non-existing-option');
      expect(component).toBeUndefined();
    });
  });
});
