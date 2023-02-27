import { inheritSerialization } from 'cerialize';
import { inheritLinkAnnotations, typedObject } from '../../cache/builders/build-decorators';
import { POOL_TASK } from './pool-task-object.resource-type';
import { TaskObject } from './task-object.model';

/**
 * A model class for a PoolTask.
 */
@typedObject
@inheritSerialization(TaskObject)
@inheritLinkAnnotations(TaskObject)
export class PoolTask extends TaskObject {
  static type = POOL_TASK;
}
