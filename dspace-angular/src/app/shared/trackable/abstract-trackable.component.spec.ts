import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { AbstractTrackableComponent } from './abstract-trackable.component';
import { INotification, Notification } from '../notifications/models/notification.model';
import { NotificationType } from '../notifications/models/notification-type';
import { of as observableOf } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { ObjectUpdatesService } from '../../core/data/object-updates/object-updates.service';
import { NotificationsService } from '../notifications/notifications.service';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestScheduler } from 'rxjs/testing';
import { getTestScheduler } from 'jasmine-marbles';

describe('AbstractTrackableComponent', () => {
  let comp: AbstractTrackableComponent;
  let fixture: ComponentFixture<AbstractTrackableComponent>;
  let objectUpdatesService;
  let scheduler: TestScheduler;

  const infoNotification: INotification = new Notification('id', NotificationType.Info, 'info');
  const warningNotification: INotification = new Notification('id', NotificationType.Warning, 'warning');
  const successNotification: INotification = new Notification('id', NotificationType.Success, 'success');

  const notificationsService = jasmine.createSpyObj('notificationsService',
    {
      info: infoNotification,
      warning: warningNotification,
      success: successNotification
    }
  );

  const url = 'http://test-url.com/test-url';

  beforeEach(waitForAsync(() => {
    objectUpdatesService = jasmine.createSpyObj('objectUpdatesService',
      {
        saveAddFieldUpdate: {},
        discardFieldUpdates: {},
        reinstateFieldUpdates: observableOf(true),
        initialize: {},
        hasUpdates: observableOf(true),
        isReinstatable: observableOf(false), // should always return something --> its in ngOnInit
        isValidPage: observableOf(true)
      }
    );

    scheduler = getTestScheduler();

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [AbstractTrackableComponent],
      providers: [
        { provide: ObjectUpdatesService, useValue: objectUpdatesService },
        { provide: NotificationsService, useValue: notificationsService },
      ], schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AbstractTrackableComponent);
    comp = fixture.componentInstance;
    comp.url = url;

    fixture.detectChanges();
  });

  it('should discard object updates', () => {
    comp.discard();

    expect(objectUpdatesService.discardFieldUpdates).toHaveBeenCalledWith(url, infoNotification);
  });
  it('should undo the discard of object updates', () => {
    comp.reinstate();

    expect(objectUpdatesService.reinstateFieldUpdates).toHaveBeenCalledWith(url);
  });

  describe('isReinstatable', () => {
    beforeEach(() => {
      objectUpdatesService.isReinstatable.and.returnValue(observableOf(true));
    });

    it('should return an observable that emits true', () => {
      const expected = '(a|)';
      scheduler.expectObservable(comp.isReinstatable()).toBe(expected, { a: true });
    });
  });

  describe('hasChanges', () => {
    beforeEach(() => {
      objectUpdatesService.hasUpdates.and.returnValue(observableOf(true));
    });

    it('should return an observable that emits true', () => {
      const expected = '(a|)';
      scheduler.expectObservable(comp.hasChanges()).toBe(expected, { a: true });
    });
  });

});
