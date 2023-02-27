import { TestBed, waitForAsync } from '@angular/core/testing';
import { Observable, Subject } from 'rxjs';
import { provideMockActions } from '@ngrx/effects/testing';
import { cold, hot } from 'jasmine-marbles';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { ObjectUpdatesEffects } from './object-updates.effects';
import {
  DiscardObjectUpdatesAction,
  ObjectUpdatesAction,
  ReinstateObjectUpdatesAction,
  RemoveFieldUpdateAction,
  RemoveObjectUpdatesAction
} from './object-updates.actions';
import {
  INotification,
  Notification
} from '../../../shared/notifications/models/notification.model';
import { NotificationType } from '../../../shared/notifications/models/notification-type';
import { filter } from 'rxjs/operators';
import { hasValue } from '../../../shared/empty.util';
import { NoOpAction } from '../../../shared/ngrx/no-op.action';

describe('ObjectUpdatesEffects', () => {
  let updatesEffects: ObjectUpdatesEffects;
  let actions: Observable<any>;
  let testURL = 'www.dspace.org/dspace7';
  let testUUID = '20e24c2f-a00a-467c-bdee-c929e79bf08d';
  const fakeID = 'id';
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      providers: [
        ObjectUpdatesEffects,
        provideMockActions(() => actions),
        {
          provide: NotificationsService,
          useValue: {
            remove: (notification) => { /* empty */
            }
          }
        },
      ],
    });
  }));

  beforeEach(() => {
    testURL = 'www.dspace.org/dspace7';
    testUUID = '20e24c2f-a00a-467c-bdee-c929e79bf08d';
    updatesEffects = TestBed.inject(ObjectUpdatesEffects);
    (updatesEffects as any).actionMap$[testURL] = new Subject<ObjectUpdatesAction>();
    (updatesEffects as any).notificationActionMap$[fakeID] = new Subject<ObjectUpdatesAction>();
    (updatesEffects as any).notificationActionMap$[(updatesEffects as any).allIdentifier] = new Subject<ObjectUpdatesAction>();
  });

  describe('mapLastActions$', () => {
    describe('When any ObjectUpdatesAction is triggered', () => {
      let action;
      let emittedAction;
      beforeEach(() => {
        action = new RemoveObjectUpdatesAction(testURL);
      });
      it('should emit the action from the actionMap\'s value which key matches the action\'s URL', () => {
        action = new RemoveObjectUpdatesAction(testURL);
        actions = hot('--a-', { a: action });
        (updatesEffects as any).actionMap$[testURL].subscribe((act) => emittedAction = act);
        const expected = cold('--b-', { b: undefined });

        expect(updatesEffects.mapLastActions$).toBeObservable(expected);
        expect(emittedAction).toBe(action);
      });
    });
  });

  describe('removeAfterDiscardOrReinstateOnUndo$', () => {
    describe('When an ObjectUpdatesActionTypes.DISCARD action is triggered', () => {
      let infoNotification: INotification;
      let removeAction;
      describe('When there is no user interactions before the timeout is finished', () => {
        beforeEach(() => {
          infoNotification = new Notification('id', NotificationType.Info, 'info');
          infoNotification.options.timeOut = 0;
          removeAction = new RemoveObjectUpdatesAction(testURL);
        });
        it('should return a RemoveObjectUpdatesAction', () => {
          actions = hot('a|', { a: new DiscardObjectUpdatesAction(testURL, infoNotification) });
          updatesEffects.removeAfterDiscardOrReinstateOnUndo$.pipe(
            filter(((action) => hasValue(action))))
            .subscribe((t) => {
                expect(t).toEqual(removeAction);
              }
            )
          ;
        });
      });

      describe('When there a REINSTATE action is fired before the timeout is finished', () => {
        beforeEach(() => {
          infoNotification = new Notification('id', NotificationType.Info, 'info');
          infoNotification.options.timeOut = 10;
        });
        it('should return an action with type NO_ACTION', () => {
          actions = hot('a', { a: new DiscardObjectUpdatesAction(testURL, infoNotification) });
          actions = hot('b', { b: new ReinstateObjectUpdatesAction(testURL) });
          updatesEffects.removeAfterDiscardOrReinstateOnUndo$.subscribe((t) => {
              expect(t).toEqual(new NoOpAction());
            }
          );
        });
      });

      describe('When there any ObjectUpdates action - other than REINSTATE - is fired before the timeout is finished', () => {
        beforeEach(() => {
          infoNotification = new Notification('id', NotificationType.Info, 'info');
          infoNotification.options.timeOut = 10;
        });
        it('should return a RemoveObjectUpdatesAction', () => {
          actions = hot('a', { a: new DiscardObjectUpdatesAction(testURL, infoNotification) });
          actions = hot('b', { b: new RemoveFieldUpdateAction(testURL, testUUID) });

          updatesEffects.removeAfterDiscardOrReinstateOnUndo$.subscribe((t) =>
            expect(t).toEqual(new RemoveObjectUpdatesAction(testURL))
          );
        });
      });
    });
  });
});
