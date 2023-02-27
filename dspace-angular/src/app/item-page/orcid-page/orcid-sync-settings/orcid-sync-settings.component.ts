import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { TranslateService } from '@ngx-translate/core';
import { Operation } from 'fast-json-patch';
import { of } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { RemoteData } from '../../../core/data/remote-data';
import { ResearcherProfileDataService } from '../../../core/profile/researcher-profile-data.service';
import { Item } from '../../../core/shared/item.model';
import { getFirstCompletedRemoteData } from '../../../core/shared/operators';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { ResearcherProfile } from '../../../core/profile/model/researcher-profile.model';

@Component({
  selector: 'ds-orcid-sync-setting',
  templateUrl: './orcid-sync-settings.component.html',
  styleUrls: ['./orcid-sync-settings.component.scss']
})
export class OrcidSyncSettingsComponent implements OnInit {

  /**
   * The item for which showing the orcid settings
   */
  @Input() item: Item;

  /**
   * The prefix used for i18n keys
   */
  messagePrefix = 'person.page.orcid';

  /**
   * The current synchronization mode
   */
  currentSyncMode: string;

  /**
   * The current synchronization mode for publications
   */
  currentSyncPublications: string;

  /**
   * The current synchronization mode for funding
   */
  currentSyncFunding: string;

  /**
   * The synchronization options
   */
  syncModes: { value: string, label: string }[];

  /**
   * The synchronization options for publications
   */
  syncPublicationOptions: { value: string, label: string }[];

  /**
   * The synchronization options for funding
   */
  syncFundingOptions: { value: string, label: string }[];

  /**
   * The profile synchronization options
   */
  syncProfileOptions: { value: string, label: string, checked: boolean }[];

  /**
   * An event emitted when settings are updated
   */
  @Output() settingsUpdated: EventEmitter<void> = new EventEmitter<void>();

  constructor(private researcherProfileService: ResearcherProfileDataService,
              private notificationsService: NotificationsService,
              private translateService: TranslateService) {
  }

  /**
   * Init orcid settings form
   */
  ngOnInit() {
    this.syncModes = [
      {
        label: this.messagePrefix + '.synchronization-mode.batch',
        value: 'BATCH'
      },
      {
        label: this.messagePrefix + '.synchronization-mode.manual',
        value: 'MANUAL'
      }
    ];

    this.syncPublicationOptions = ['DISABLED', 'ALL']
      .map((value) => {
        return {
          label: this.messagePrefix + '.sync-publications.' + value.toLowerCase(),
          value: value,
        };
      });

    this.syncFundingOptions = ['DISABLED', 'ALL']
      .map((value) => {
        return {
          label: this.messagePrefix + '.sync-fundings.' + value.toLowerCase(),
          value: value,
        };
      });

    const syncProfilePreferences = this.item.allMetadataValues('dspace.orcid.sync-profile');

    this.syncProfileOptions = ['BIOGRAPHICAL', 'IDENTIFIERS']
      .map((value) => {
        return {
          label: this.messagePrefix + '.sync-profile.' + value.toLowerCase(),
          value: value,
          checked: syncProfilePreferences.includes(value)
        };
      });

    this.currentSyncMode = this.getCurrentPreference('dspace.orcid.sync-mode', ['BATCH', 'MANUAL'], 'MANUAL');
    this.currentSyncPublications = this.getCurrentPreference('dspace.orcid.sync-publications', ['DISABLED', 'ALL'], 'DISABLED');
    this.currentSyncFunding = this.getCurrentPreference('dspace.orcid.sync-fundings', ['DISABLED', 'ALL'], 'DISABLED');
  }

  /**
   * Generate path operations to save orcid synchronization preferences
   *
   * @param form The form group
   */
  onSubmit(form: FormGroup): void {
    const operations: Operation[] = [];
    this.fillOperationsFor(operations, '/orcid/mode', form.value.syncMode);
    this.fillOperationsFor(operations, '/orcid/publications', form.value.syncPublications);
    this.fillOperationsFor(operations, '/orcid/fundings', form.value.syncFundings);

    const syncProfileValue = this.syncProfileOptions
      .map((syncProfileOption => syncProfileOption.value))
      .filter((value) => form.value['syncProfile_' + value])
      .join(',');

    this.fillOperationsFor(operations, '/orcid/profile', syncProfileValue);

    if (operations.length === 0) {
      return;
    }

    this.researcherProfileService.findByRelatedItem(this.item).pipe(
      getFirstCompletedRemoteData(),
      switchMap((profileRD: RemoteData<ResearcherProfile>) => {
        if (profileRD.hasSucceeded) {
          return this.researcherProfileService.patch(profileRD.payload, operations).pipe(
            getFirstCompletedRemoteData(),
          );
        } else {
          return of(profileRD);
        }
      }),
    ).subscribe((remoteData: RemoteData<ResearcherProfile>) => {
      if (remoteData.isSuccess) {
        this.notificationsService.success(this.translateService.get(this.messagePrefix + '.synchronization-settings-update.success'));
        this.settingsUpdated.emit();
      } else {
        this.notificationsService.error(this.translateService.get(this.messagePrefix + '.synchronization-settings-update.error'));
      }
    });
  }

  /**
   * Retrieve setting saved in the item's metadata
   *
   * @param metadataField The metadata name that contains setting
   * @param allowedValues The allowed values
   * @param defaultValue  The default value
   * @private
   */
  private getCurrentPreference(metadataField: string, allowedValues: string[], defaultValue: string): string {
    const currentPreference = this.item.firstMetadataValue(metadataField);
    return (currentPreference && allowedValues.includes(currentPreference)) ? currentPreference : defaultValue;
  }

  /**
   * Generate a replace patch operation
   *
   * @param operations
   * @param path
   * @param currentValue
   */
  private fillOperationsFor(operations: Operation[], path: string, currentValue: string): void {
    operations.push({
      path: path,
      op: 'replace',
      value: currentValue
    });
  }

}
