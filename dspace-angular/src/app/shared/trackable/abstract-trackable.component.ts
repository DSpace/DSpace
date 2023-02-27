import { ObjectUpdatesService } from '../../core/data/object-updates/object-updates.service';
import { NotificationsService } from '../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { Component } from '@angular/core';

/**
 * Abstract Component that is able to track changes made in the inheriting component using the ObjectUpdateService
 */
@Component({
  selector: 'ds-abstract-trackable',
  template: ''
})
export class AbstractTrackableComponent {

  /**
   * The time span for being able to undo discarding changes
   */
  public discardTimeOut: number;
  public message: string;
  public url: string;
  public notificationsPrefix = 'static-pages.form.notification';

  constructor(
    public objectUpdatesService: ObjectUpdatesService,
    public notificationsService: NotificationsService,
    public translateService: TranslateService,
  ) {

  }

  /**
   * Request the object updates service to discard all current changes to this item
   * Shows a notification to remind the user that they can undo this
   */
  discard() {
    const undoNotification = this.notificationsService.info(this.getNotificationTitle('discarded'), this.getNotificationContent('discarded'), {timeOut: this.discardTimeOut});
    this.objectUpdatesService.discardFieldUpdates(this.url, undoNotification);
  }

  /**
   * Request the object updates service to undo discarding all changes to this item
   */
  reinstate() {
    this.objectUpdatesService.reinstateFieldUpdates(this.url);
  }

  /**
   * Checks whether or not the object is currently reinstatable
   */
  isReinstatable(): Observable<boolean> {
    return this.objectUpdatesService.isReinstatable(this.url);
  }

  /**
   * Checks whether or not there are currently updates for this object
   */
  hasChanges(): Observable<boolean> {
    return this.objectUpdatesService.hasUpdates(this.url);
  }

  /**
   * Get translated notification title
   * @param key
   */
  getNotificationTitle(key: string) {
    return this.translateService.instant(this.notificationsPrefix + key + '.title');
  }

  /**
   * Get translated notification content
   * @param key
   */
  getNotificationContent(key: string) {
    return this.translateService.instant(this.notificationsPrefix + key + '.content');

  }
}
