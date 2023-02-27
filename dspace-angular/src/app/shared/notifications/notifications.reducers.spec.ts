import { notificationsReducer } from './notifications.reducers';
import { NewNotificationAction, RemoveAllNotificationsAction, RemoveNotificationAction } from './notifications.actions';
import { NotificationsService } from './notifications.service';
import { fakeAsync, flush, inject, TestBed, tick } from '@angular/core/testing';
import { NotificationsBoardComponent } from './notifications-board/notifications-board.component';
import { StoreModule } from '@ngrx/store';
import { NotificationComponent } from './notification/notification.component';
import { NotificationOptions } from './models/notification-options.model';
import { NotificationAnimationsType } from './models/notification-animations-type';
import { NotificationType } from './models/notification-type';
import { Notification } from './models/notification.model';
import uniqueId from 'lodash/uniqueId';
import { ChangeDetectorRef } from '@angular/core';
import { storeModuleConfig } from '../../app.reducer';

describe('Notifications reducer', () => {

  let notification1;
  let notification2;
  let notification3;
  let notificationHtml;
  let options;
  let html;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [NotificationComponent, NotificationsBoardComponent],
      providers: [
        NotificationsService,
        ChangeDetectorRef,
      ],
      imports: [
        StoreModule.forRoot({ notificationsReducer }, storeModuleConfig),
      ]
    });

    options = new NotificationOptions(
      0,
      true,
      NotificationAnimationsType.Rotate);
    notification1 = new Notification(uniqueId(), NotificationType.Success, 'title1', 'content1', options, null);
    notification2 = new Notification(uniqueId(), NotificationType.Info, 'title2', 'content2', options, null);
    notification3 = new Notification(uniqueId(), NotificationType.Warning, 'title3', 'content3', options, null);
    html = '<p>I\'m a mock test</p>';
    notificationHtml = new Notification(uniqueId(), NotificationType.Error, null, null, options, html);
  });

  it('should add 4 notifications and verify fields and length', () => {
    const state1 = notificationsReducer(undefined, new NewNotificationAction(notification1));
    const n1 = state1[0];
    expect(n1.title).toBe('title1');
    expect(n1.content).toBe('content1');
    expect(n1.type).toBe(NotificationType.Success);
    expect(n1.options).toBe(options);
    expect(n1.html).toBeNull();
    expect(state1.length).toEqual(1);

    const state2 = notificationsReducer(state1, new NewNotificationAction(notification2));
    const n2 = state2[1];
    expect(n2.title).toBe('title2');
    expect(n2.content).toBe('content2');
    expect(n2.type).toBe(NotificationType.Info);
    expect(n2.options).toBe(options);
    expect(n2.html).toBeNull();
    expect(state2.length).toEqual(2);

    const state3 = notificationsReducer(state2, new NewNotificationAction(notification3));
    const n3 = state3[2];
    expect(n3.title).toBe('title3');
    expect(n3.content).toBe('content3');
    expect(n3.type).toBe(NotificationType.Warning);
    expect(n3.options).toBe(options);
    expect(n3.html).toBeNull();
    expect(state3.length).toEqual(3);

    const state4 = notificationsReducer(state3, new NewNotificationAction(notificationHtml));
    const n4 = state4[3];
    expect(n4.title).toBeNull();
    expect(n4.content).toBeNull();
    expect(n4.type).toBe(NotificationType.Error);
    expect(n4.options).toBe(options);
    expect(n4.html).toBe(html);
    expect(state4.length).toEqual(4);
  });

  it('should add 2 notifications and remove only the first', () => {
    const state1 = notificationsReducer(undefined, new NewNotificationAction(notification1));
    expect(state1.length).toEqual(1);

    const state2 = notificationsReducer(state1, new NewNotificationAction(notification2));
    expect(state2.length).toEqual(2);

    const state3 = notificationsReducer(state2, new RemoveNotificationAction(notification1.id));
    expect(state3.length).toEqual(1);

  });

  it('should add 2 notifications and later remove all', () => {
    const state1 = notificationsReducer(undefined, new NewNotificationAction(notification1));
    expect(state1.length).toEqual(1);

    const state2 = notificationsReducer(state1, new NewNotificationAction(notification2));
    expect(state2.length).toEqual(2);

    const state3 = notificationsReducer(state2, new RemoveAllNotificationsAction());
    expect(state3.length).toEqual(0);
  });

  it('should create 2 notifications and check they close after different timeout', fakeAsync(() => {
    inject([ChangeDetectorRef], (cdr: ChangeDetectorRef) => {
      const optionsWithTimeout = new NotificationOptions(
        1000,
        true,
        NotificationAnimationsType.Rotate);
      // Timeout 1000ms
      const notification = new Notification(uniqueId(), NotificationType.Success, 'title', 'content', optionsWithTimeout, null);
      const state = notificationsReducer(undefined, new NewNotificationAction(notification));
      expect(state.length).toEqual(1);

      // Timeout default 5000ms
      const notificationBis = new Notification(uniqueId(), NotificationType.Success, 'title', 'content');
      const stateBis = notificationsReducer(state, new NewNotificationAction(notification));
      expect(stateBis.length).toEqual(2);

      tick(1000);
      cdr.detectChanges();

      const action = new NewNotificationAction(notification);
      action.type = 'NothingToDo, return only the state';

      const lastState = notificationsReducer(stateBis, action);
      expect(lastState.length).toEqual(1);

      flush();
      cdr.detectChanges();

      const finalState = notificationsReducer(lastState, action);
      expect(finalState.length).toEqual(0);
    });

  }));

});
