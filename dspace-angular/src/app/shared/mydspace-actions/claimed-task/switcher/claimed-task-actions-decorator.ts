import { hasNoValue } from '../../../empty.util';

const workflowOptions = new Map();
const advancedWorkflowOptions = new Map();

/**
 * Decorator used for rendering ClaimedTaskActions pages by option type
 */
export function rendersWorkflowTaskOption(option: string) {
  return function decorator(component: any) {
    if (hasNoValue(workflowOptions.get(option))) {
      workflowOptions.set(option, component);
    } else {
      throw new Error(`There can't be more than one component to render ClaimedTaskActions for option "${option}"`);
    }
  };
}

/**
 * Decorator used for rendering AdvancedClaimedTaskActions pages by option type
 */
export function rendersAdvancedWorkflowTaskOption(option: string) {
  return function decorator(component: any) {
    if (hasNoValue(advancedWorkflowOptions.get(option))) {
      advancedWorkflowOptions.set(option, component);
    } else {
      throw new Error(`There can't be more than one component to render AdvancedClaimedTaskActions for option "${option}"`);
    }
  };
}

/**
 * Get the component used for rendering a ClaimedTaskActions page by option type
 */
export function getComponentByWorkflowTaskOption(option: string) {
  return workflowOptions.get(option);
}

/**
 * Get the component used for rendering a AdvancedClaimedTaskActions page by option type
 */
export function getAdvancedComponentByWorkflowTaskOption(option: string) {
  return advancedWorkflowOptions.get(option);
}
