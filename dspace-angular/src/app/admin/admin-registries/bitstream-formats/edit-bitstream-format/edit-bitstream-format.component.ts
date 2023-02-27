import { map } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { Component, OnInit } from '@angular/core';
import { RemoteData } from '../../../../core/data/remote-data';
import { BitstreamFormat } from '../../../../core/shared/bitstream-format.model';
import { BitstreamFormatDataService } from '../../../../core/data/bitstream-format-data.service';
import { NotificationsService } from '../../../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { getBitstreamFormatsModuleRoute } from '../../admin-registries-routing-paths';
import { getFirstCompletedRemoteData } from '../../../../core/shared/operators';

/**
 * This component renders the edit page of a bitstream format.
 * The route parameter 'id' is used to request the bitstream format.
 */
@Component({
  selector: 'ds-edit-bitstream-format',
  templateUrl: './edit-bitstream-format.component.html',
})
export class EditBitstreamFormatComponent implements OnInit {

  /**
   * The bitstream format wrapped in a remote-data object
   */
  bitstreamFormatRD$: Observable<RemoteData<BitstreamFormat>>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationsService,
    private translateService: TranslateService,
    private bitstreamFormatDataService: BitstreamFormatDataService,
  ) {
  }

  ngOnInit(): void {
    this.bitstreamFormatRD$ = this.route.data.pipe(
      map((data) => data.bitstreamFormat as RemoteData<BitstreamFormat>)
    );
  }

  /**
   * Updates the bitstream format based on the provided bitstream format emitted by the form.
   * When successful, a success notification will be shown and the user will be navigated back to the overview page.
   * When failed, an error  notification will be shown.
   */
  updateFormat(bitstreamFormat: BitstreamFormat) {
    this.bitstreamFormatDataService.updateBitstreamFormat(bitstreamFormat).pipe(
      getFirstCompletedRemoteData(),
    ).subscribe((response: RemoteData<BitstreamFormat>) => {
        if (response.hasSucceeded) {
          this.notificationService.success(this.translateService.get('admin.registries.bitstream-formats.edit.success.head'),
            this.translateService.get('admin.registries.bitstream-formats.edit.success.content'));
          this.router.navigate([getBitstreamFormatsModuleRoute()]);
        } else {
          this.notificationService.error('admin.registries.bitstream-formats.edit.failure.head',
            'admin.registries.bitstream-formats.create.edit.content');
        }
      }
    );
  }
}
