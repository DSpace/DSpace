import { ListableObject } from '../../object-collection/shared/listable-object.model';
import { typedObject } from '../../../core/cache/builders/build-decorators';
import { TypedObject } from '../../../core/cache/typed-object.model';
import { LISTABLE_NOTIFICATION_OBJECT } from './listable-notification-object.resource-type';
import { GenericConstructor } from '../../../core/shared/generic-constructor';
import { NotificationType } from '../../notifications/models/notification-type';
import { ResourceType } from '../../../core/shared/resource-type';

/**
 * Object representing a notification message inside a list of objects
 */
@typedObject
export class ListableNotificationObject extends ListableObject implements TypedObject {

  static type: ResourceType = LISTABLE_NOTIFICATION_OBJECT;
  type: ResourceType = LISTABLE_NOTIFICATION_OBJECT;

  protected renderTypes: string[];

  constructor(
    public notificationType: NotificationType = NotificationType.Error,
    public message: string = 'listable-notification-object.default-message',
    ...renderTypes: string[]
  ) {
    super();
    this.renderTypes = renderTypes;
  }

  /**
   * Method that returns as which type of object this object should be rendered.
   */
  getRenderTypes(): (string | GenericConstructor<ListableObject>)[] {
    return [...this.renderTypes, this.constructor as GenericConstructor<ListableObject>];
  }

}
