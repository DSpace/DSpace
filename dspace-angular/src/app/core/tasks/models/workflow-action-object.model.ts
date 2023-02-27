import { inheritSerialization, autoserialize } from 'cerialize';
import { typedObject } from '../../cache/builders/build-decorators';
import { DSpaceObject } from '../../shared/dspace-object.model';
import { WORKFLOW_ACTION } from './workflow-action-object.resource-type';
import { AdvancedWorkflowInfo } from './advanced-workflow-info.model';

/**
 * A model class for a WorkflowAction
 */
@typedObject
@inheritSerialization(DSpaceObject)
export class WorkflowAction extends DSpaceObject {
  static type = WORKFLOW_ACTION;

  /**
   * The workflow action's identifier
   */
  @autoserialize
  id: string;

  /**
   * The options available for this workflow action
   */
  @autoserialize
  options: string[];

  /**
   * Whether this action has advanced options
   */
  @autoserialize
  advanced: boolean;

  /**
   * The advanced options that the user can select at this action
   */
  @autoserialize
  advancedOptions: string[];

  /**
   * The advanced info required by the advanced options
   */
  @autoserialize
  advancedInfo: AdvancedWorkflowInfo[];

}
