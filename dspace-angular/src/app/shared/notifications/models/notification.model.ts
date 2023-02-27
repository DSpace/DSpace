import { INotificationOptions, NotificationOptions } from './notification-options.model';
import { NotificationType } from './notification-type';
import { isEmpty } from '../../empty.util';
import { Observable } from 'rxjs';

export interface INotification {
  id: string;
  type: NotificationType;
  title?: Observable<string> | string;
  content?: Observable<string> | string;
  options?: INotificationOptions;
  html?: boolean;
}

export class Notification implements INotification {
  public id: string;
  public type: NotificationType;
  public title: Observable<string> | string;
  public content: Observable<string> | string;
  public options: INotificationOptions;
  public html: boolean;

  constructor(id: string,
              type: NotificationType,
              title?: Observable<string> | string,
              content?: Observable<string> | string,
              options?: NotificationOptions,
              html?: boolean) {

    this.id = id;
    this.type = type;
    this.title = title;
    this.content = content;
    this.options = isEmpty(options) ? new NotificationOptions() : options;
    this.html = html;
  }

}
