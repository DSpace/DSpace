import { Config } from './config.interface';
import { NotificationAnimationsType } from '../app/shared/notifications/models/notification-animations-type';

export interface INotificationBoardOptions extends Config {
  rtl: boolean;
  position: ['top' | 'bottom' | 'middle', 'right' | 'left' | 'center'];
  maxStack: number;
  timeOut: number;
  clickToClose: boolean;
  animate: NotificationAnimationsType;
}
