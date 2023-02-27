import { typedObject } from '../../cache/builders/build-decorators';
import { inheritSerialization, autoserialize } from 'cerialize';
import { SELECT_REVIEWER_ADVANCED_WORKFLOW_INFO } from './advanced-workflow-info.resource-type';
import { AdvancedWorkflowInfo } from './advanced-workflow-info.model';
import { ResourceType } from '../../shared/resource-type';

/**
 * A model class for a {@link SelectReviewerAdvancedWorkflowInfo}
 */
@typedObject
@inheritSerialization(AdvancedWorkflowInfo)
export class SelectReviewerAdvancedWorkflowInfo extends AdvancedWorkflowInfo {

  static type: ResourceType = SELECT_REVIEWER_ADVANCED_WORKFLOW_INFO;

  @autoserialize
  group: string;

}
