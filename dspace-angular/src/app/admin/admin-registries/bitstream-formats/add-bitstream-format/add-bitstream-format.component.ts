import { Router } from '@angular/router';
import { Component } from '@angular/core';
import { BitstreamFormat } from '../../../../core/shared/bitstream-format.model';
import { BitstreamFormatDataService } from '../../../../core/data/bitstream-format-data.service';
import { NotificationsService } from '../../../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { getBitstreamFormatsModuleRoute } from '../../admin-registries-routing-paths';
import { RemoteData } from '../../../../core/data/remote-data';
import { getFirstCompletedRemoteData } from '../../../../core/shared/operators';

/**
 * This component renders the page to create a new bitstream format.
 */
@Component({
  selector: 'ds-add-bitstream-format',
  templateUrl: './add-bitstream-format.component.html',
})
export class AddBitstreamFormatComponent {

  constructor(
    private router: Router,
    private notificationService: NotificationsService,
    private translateService: TranslateService,
    private bitstreamFormatDataService: BitstreamFormatDataService,
  ) {
  }

  /**
   * Creates a new bitstream format based on the provided bitstream format emitted by the form.
   * When successful, a success notification will be shown and the user will be navigated back to the overview page.
   * When failed, an error  notification will be shown.
   * @param bitstreamFormat
   */
  createBitstreamFormat(bitstreamFormat: BitstreamFormat) {
    this.bitstreamFormatDataService.createBitstreamFormat(bitstreamFormat).pipe(
      getFirstCompletedRemoteData(),
    ).subscribe((response: RemoteData<BitstreamFormat>) => {
        if (response.hasSucceeded) {
          this.notificationService.success(this.translateService.get('admin.registries.bitstream-formats.create.success.head'),
            this.translateService.get('admin.registries.bitstream-formats.create.success.content'));
          this.router.navigate([getBitstreamFormatsModuleRoute()]);
          this.bitstreamFormatDataService.clearBitStreamFormatRequests().subscribe();
        } else {
          this.notificationService.error(this.translateService.get('admin.registries.bitstream-formats.create.failure.head'),
            this.translateService.get('admin.registries.bitstream-formats.create.failure.content'));
        }
      }
    );
  }
}
