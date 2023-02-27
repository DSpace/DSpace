import { inheritSerialization } from 'cerialize';
import { inheritLinkAnnotations, typedObject } from '../../cache/builders/build-decorators';
import { CLAIMED_TASK } from './claimed-task-object.resource-type';
import { TaskObject } from './task-object.model';

/**
 * A model class for a ClaimedTask.
 */
@typedObject
@inheritSerialization(TaskObject)
@inheritLinkAnnotations(TaskObject)
export class ClaimedTask extends TaskObject {
  static type = CLAIMED_TASK;
}
