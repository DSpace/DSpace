import { Injectable } from '@angular/core';

import { of as observableOf } from 'rxjs';
import { first } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import uniqueId from 'lodash/uniqueId';

import { INotification, Notification } from './models/notification.model';
import { NotificationType } from './models/notification-type';
import { NotificationOptions } from './models/notification-options.model';

import { NewNotificationAction, RemoveAllNotificationsAction, RemoveNotificationAction } from './notifications.actions';
import { environment } from '../../../environments/environment';

@Injectable()
export class NotificationsService {

  constructor(private store: Store<Notification>,
              private translate: TranslateService) {
  }

  private add(notification: Notification) {
    let notificationAction;
    notificationAction = new NewNotificationAction(notification);
    this.store.dispatch(notificationAction);
  }

  success(title: any = observableOf(''),
          content: any = observableOf(''),
          options: Partial<NotificationOptions> = {},
          html: boolean = false): INotification {
    const notificationOptions = { ...this.getDefaultOptions(), ...options };
    const notification = new Notification(uniqueId(), NotificationType.Success, title, content, notificationOptions, html);
    this.add(notification);
    return notification;
  }

  error(title: any = observableOf(''),
        content: any = observableOf(''),
        options: Partial<NotificationOptions> = {},
        html: boolean = false): INotification {
    const notificationOptions = { ...this.getDefaultOptions(), ...options };
    const notification = new Notification(uniqueId(), NotificationType.Error, title, content, notificationOptions, html);
    this.add(notification);
    return notification;
  }

  info(title: any = observableOf(''),
       content: any = observableOf(''),
       options: Partial<NotificationOptions> = {},
       html: boolean = false): INotification {
    const notificationOptions = { ...this.getDefaultOptions(), ...options };
    const notification = new Notification(uniqueId(), NotificationType.Info, title, content, notificationOptions, html);
    this.add(notification);
    return notification;
  }

  warning(title: any = observableOf(''),
          content: any = observableOf(''),
          options: NotificationOptions = this.getDefaultOptions(),
          html: boolean = false): INotification {
    const notificationOptions = { ...this.getDefaultOptions(), ...options };
    const notification = new Notification(uniqueId(), NotificationType.Warning, title, content, notificationOptions, html);
    this.add(notification);
    return notification;
  }

  notificationWithAnchor(notificationType: NotificationType,
                         options: NotificationOptions,
                         href: string,
                         hrefTranslateLabel: string,
                         messageTranslateLabel: string,
                         interpolateParam: string) {
    this.translate.get(hrefTranslateLabel)
      .pipe(first())
      .subscribe((hrefMsg) => {
        const anchor = `<a class="align-baseline btn btn-link p-0 m-0" href="${href}" >
                        <strong>${hrefMsg}</strong>
                      </a>`;
        const interpolateParams = Object.create({});
        interpolateParams[interpolateParam] = anchor;
        this.translate.get(messageTranslateLabel, interpolateParams)
          .pipe(first())
          .subscribe((m) => {
            switch (notificationType) {
              case NotificationType.Success:
                this.success(null, m, options, true);
                break;
              case NotificationType.Error:
                this.error(null, m, options, true);
                break;
              case NotificationType.Info:
                this.info(null, m, options, true);
                break;
              case NotificationType.Warning:
                this.warning(null, m, options, true);
                break;
            }
          });
      });
  }

  remove(notification: INotification) {
    const actionRemove = new RemoveNotificationAction(notification.id);
    this.store.dispatch(actionRemove);
  }

  removeAll() {
    const actionRemoveAll = new RemoveAllNotificationsAction();
    this.store.dispatch(actionRemoveAll);
  }

  private getDefaultOptions(): NotificationOptions {
    return new NotificationOptions(
      environment.notifications.timeOut,
      environment.notifications.clickToClose,
      environment.notifications.animate
    );
  }
}
