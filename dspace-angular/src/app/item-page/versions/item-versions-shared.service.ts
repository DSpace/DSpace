import { Injectable } from '@angular/core';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { RemoteData } from '../../core/data/remote-data';
import { Version } from '../../core/shared/version.model';

@Injectable({
  providedIn: 'root'
})
export class ItemVersionsSharedService {

  constructor(
    private notificationsService: NotificationsService,
    private translateService: TranslateService,
  ) {
  }

  private static msg(key: string): string {
    const translationPrefix = 'item.version.create.notification';
    return translationPrefix + '.' + key;
  }

  /**
   * Notify success/failure after creating a new version.
   *
   * @param newVersionRD the new version that has been created
   */
  public notifyCreateNewVersion(newVersionRD: RemoteData<Version>): void {
    const newVersionNumber = newVersionRD?.payload?.version;
    newVersionRD.hasSucceeded ?
      this.notificationsService.success(null, this.translateService.get(ItemVersionsSharedService.msg('success'), {version: newVersionNumber})) :
      this.notificationsService.error(null, this.translateService.get(ItemVersionsSharedService.msg(newVersionRD?.statusCode === 422 ? 'inProgress' : 'failure')));
  }

}
