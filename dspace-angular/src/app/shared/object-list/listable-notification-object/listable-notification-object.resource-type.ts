import { ResourceType } from '../../../core/shared/resource-type';

/**
 * The resource type for {@link ListableNotificationObject}
 *
 * Needs to be in a separate file to prevent circular
 * dependencies in webpack.
 */
export const LISTABLE_NOTIFICATION_OBJECT = new ResourceType('listable-notification-object');
