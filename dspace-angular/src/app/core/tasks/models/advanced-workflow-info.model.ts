import { autoserialize } from 'cerialize';

/**
 * An abstract model class for a {@link AdvancedWorkflowInfo}
 */
export abstract class AdvancedWorkflowInfo {

  @autoserialize
  id: string;

}
