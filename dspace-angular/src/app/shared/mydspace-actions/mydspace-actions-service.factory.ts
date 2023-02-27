import { ResourceType } from '../../core/shared/resource-type';
import { WorkspaceitemDataService } from '../../core/submission/workspaceitem-data.service';
import { ClaimedTaskDataService } from '../../core/tasks/claimed-task-data.service';
import { PoolTaskDataService } from '../../core/tasks/pool-task-data.service';
import { WorkflowItemDataService } from '../../core/submission/workflowitem-data.service';
import { ItemDataService } from '../../core/data/item-data.service';
import { Item } from '../../core/shared/item.model';
import { PoolTask } from '../../core/tasks/models/pool-task-object.model';
import { ClaimedTask } from '../../core/tasks/models/claimed-task-object.model';
import { WorkspaceItem } from '../../core/submission/models/workspaceitem.model';
import { WorkflowItem } from '../../core/submission/models/workflowitem.model';
import { CacheableObject } from '../../core/cache/cacheable-object.model';
import { IdentifiableDataService } from '../../core/data/base/identifiable-data.service';

/**
 * Class to return DataService for given ResourceType
 */
export class MyDSpaceActionsServiceFactory<T extends CacheableObject, TService extends IdentifiableDataService<T>> {
  public getConstructor(type: ResourceType): TService {
    switch (type) {
      case Item.type: {
        return ItemDataService as any;
      }
      case WorkspaceItem.type: {
        return WorkspaceitemDataService as any;
      }
      case WorkflowItem.type: {
        return WorkflowItemDataService as any;
      }
      case ClaimedTask.type: {
        return ClaimedTaskDataService as any;
      }
      case PoolTask.type: {
        return PoolTaskDataService as any;
      }
      default: {
        return undefined;
      }
    }
  }
}
