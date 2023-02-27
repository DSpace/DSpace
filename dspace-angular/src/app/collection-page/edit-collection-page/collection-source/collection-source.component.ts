import { Component, OnDestroy, OnInit } from '@angular/core';
import { AbstractTrackableComponent } from '../../../shared/trackable/abstract-trackable.component';
import {
  DynamicFormControlModel,
  DynamicFormGroupModel,
  DynamicFormLayout,
  DynamicFormService,
  DynamicInputModel,
  DynamicOptionControlModel,
  DynamicRadioGroupModel,
  DynamicSelectModel
} from '@ng-dynamic-forms/core';
import { Location } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { ObjectUpdatesService } from '../../../core/data/object-updates/object-updates.service';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { FormGroup } from '@angular/forms';
import { hasNoValue, hasValue, isNotEmpty } from '../../../shared/empty.util';
import { ContentSource, ContentSourceHarvestType } from '../../../core/shared/content-source.model';
import { Observable, Subscription } from 'rxjs';
import { RemoteData } from '../../../core/data/remote-data';
import { Collection } from '../../../core/shared/collection.model';
import { first, map, switchMap, take } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import cloneDeep from 'lodash/cloneDeep';
import { CollectionDataService } from '../../../core/data/collection-data.service';
import { getFirstSucceededRemoteData, getFirstCompletedRemoteData } from '../../../core/shared/operators';
import { MetadataConfig } from '../../../core/shared/metadata-config.model';
import { INotification } from '../../../shared/notifications/models/notification.model';
import { RequestService } from '../../../core/data/request.service';
import { environment } from '../../../../environments/environment';
import { FieldUpdate } from '../../../core/data/object-updates/field-update.model';
import { FieldUpdates } from '../../../core/data/object-updates/field-updates.model';

/**
 * Component for managing the content source of the collection
 */
@Component({
  selector: 'ds-collection-source',
  templateUrl: './collection-source.component.html',
})
export class CollectionSourceComponent extends AbstractTrackableComponent implements OnInit, OnDestroy {
  /**
   * The current collection's remote data
   */
  collectionRD$: Observable<RemoteData<Collection>>;

  /**
   * The collection's content source
   */
  contentSource: ContentSource;

  /**
   * The current update to the content source
   */
  update$: Observable<FieldUpdate>;

  /**
   * The initial harvest type we started off with
   * Used to compare changes
   */
  initialHarvestType: ContentSourceHarvestType;

  /**
   * @type {string} Key prefix used to generate form labels
   */
  LABEL_KEY_PREFIX = 'collection.edit.tabs.source.form.';

  /**
   * @type {string} Key prefix used to generate form error messages
   */
  ERROR_KEY_PREFIX = 'collection.edit.tabs.source.form.errors.';

  /**
   * @type {string} Key prefix used to generate form option labels
   */
  OPTIONS_KEY_PREFIX = 'collection.edit.tabs.source.form.options.';

  /**
   * The Dynamic Input Model for the OAI Provider
   */
  oaiSourceModel = new DynamicInputModel({
    id: 'oaiSource',
    name: 'oaiSource',
    required: true,
    validators: {
      required: null
    },
    errorMessages: {
      required: 'You must provide a set id of the target collection.'
    }
  });

  /**
   * The Dynamic Input Model for the OAI Set
   */
  oaiSetIdModel = new DynamicInputModel({
    id: 'oaiSetId',
    name: 'oaiSetId'
  });

  /**
   * The Dynamic Input Model for the Metadata Format used
   */
  metadataConfigIdModel = new DynamicSelectModel({
    id: 'metadataConfigId',
    name: 'metadataConfigId'
  });

  /**
   * The Dynamic Input Model for the type of harvesting
   */
  harvestTypeModel = new DynamicRadioGroupModel<string>({
    id: 'harvestType',
    name: 'harvestType',
    options: [
      {
        value: ContentSourceHarvestType.Metadata
      },
      {
        value: ContentSourceHarvestType.MetadataAndRef
      },
      {
        value: ContentSourceHarvestType.MetadataAndBitstreams
      }
    ]
  });

  /**
   * All input models in a simple array for easier iterations
   */
  inputModels = [this.oaiSourceModel, this.oaiSetIdModel, this.metadataConfigIdModel, this.harvestTypeModel];

  /**
   * The dynamic form fields used for editing the content source of a collection
   * @type {(DynamicInputModel | DynamicTextAreaModel)[]}
   */
  formModel: DynamicFormControlModel[] = [
    new DynamicFormGroupModel({
      id: 'oaiSourceContainer',
      group: [
        this.oaiSourceModel
      ]
    }),
    new DynamicFormGroupModel({
      id: 'oaiSetContainer',
      group: [
        this.oaiSetIdModel,
        this.metadataConfigIdModel
      ]
    }),
    new DynamicFormGroupModel({
      id: 'harvestTypeContainer',
      group: [
        this.harvestTypeModel
      ]
    })
  ];

  /**
   * Layout used for structuring the form inputs
   */
  formLayout: DynamicFormLayout = {
    oaiSource: {
      grid: {
        host: 'col-12 d-inline-block'
      }
    },
    oaiSetId: {
      grid: {
        host: 'col col-sm-6 d-inline-block'
      }
    },
    metadataConfigId: {
      grid: {
        host: 'col col-sm-6 d-inline-block'
      }
    },
    harvestType: {
      grid: {
        host: 'col-12',
        option: 'btn-outline-secondary'
      }
    },
    oaiSetContainer: {
      grid: {
        host: 'row'
      }
    },
    oaiSourceContainer: {
      grid: {
        host: 'row'
      }
    },
    harvestTypeContainer: {
      grid: {
        host: 'row'
      }
    }
  };

  /**
   * The form group of this form
   */
  formGroup: FormGroup;

  /**
   * Subscription to update the current form
   */
  updateSub: Subscription;

  /**
   * The content harvesting type used when harvesting is disabled
   */
  harvestTypeNone = ContentSourceHarvestType.None;

  /**
   * The previously selected harvesting type
   * Used for switching between ContentSourceHarvestType.None and the previously selected value when enabling / disabling harvesting
   * Defaults to ContentSourceHarvestType.Metadata
   */
  previouslySelectedHarvestType = ContentSourceHarvestType.Metadata;

  /**
   * Notifications displayed after clicking submit
   * These are cleaned up every time a user submits the form to prevent error or other notifications from staying active
   * while they shouldn't be.
   */
  displayedNotifications: INotification[] = [];

  public constructor(public objectUpdatesService: ObjectUpdatesService,
                     public notificationsService: NotificationsService,
                     protected location: Location,
                     protected formService: DynamicFormService,
                     protected translate: TranslateService,
                     protected route: ActivatedRoute,
                     protected router: Router,
                     protected collectionService: CollectionDataService,
                     protected requestService: RequestService) {
    super(objectUpdatesService, notificationsService, translate);
  }

  /**
   * Initialize properties to setup the Field Update and Form
   */
  ngOnInit(): void {
    this.notificationsPrefix = 'collection.edit.tabs.source.notifications.';
    this.discardTimeOut = environment.collection.edit.undoTimeout;
    this.url = this.router.url;
    if (this.url.indexOf('?') > 0) {
      this.url = this.url.substr(0, this.url.indexOf('?'));
    }
    this.formGroup = this.formService.createFormGroup(this.formModel);
    this.collectionRD$ = this.route.parent.data.pipe(first(), map((data) => data.dso));

    this.collectionRD$.pipe(
      getFirstSucceededRemoteData(),
      map((col) => col.payload.uuid),
      switchMap((uuid) => this.collectionService.getContentSource(uuid)),
      getFirstCompletedRemoteData(),
    ).subscribe((rd: RemoteData<ContentSource>) => {
      this.initializeOriginalContentSource(rd.payload);
    });

    this.updateFieldTranslations();
    this.translate.onLangChange
      .subscribe(() => {
        this.updateFieldTranslations();
      });
  }

  /**
   * Initialize the Field Update and subscribe on it to fire updates to the form whenever it changes
   */
  initializeOriginalContentSource(contentSource: ContentSource) {
    this.contentSource = contentSource;
    this.initialHarvestType = contentSource.harvestType;
    this.initializeMetadataConfigs();
    const initialContentSource = cloneDeep(this.contentSource);
    this.objectUpdatesService.initialize(this.url, [initialContentSource], new Date());
    this.update$ = this.objectUpdatesService.getFieldUpdates(this.url, [initialContentSource]).pipe(
      map((updates: FieldUpdates) => updates[initialContentSource.uuid])
    );
    this.updateSub = this.update$.subscribe((update: FieldUpdate) => {
      if (update) {
        const field = update.field as ContentSource;
        let configId;
        if (hasValue(this.contentSource) && isNotEmpty(this.contentSource.metadataConfigs)) {
          configId = this.contentSource.metadataConfigs[0].id;
        }
        if (hasValue(field) && hasValue(field.metadataConfigId)) {
          configId = field.metadataConfigId;
        }
        if (hasValue(field)) {
          this.formGroup.patchValue({
            oaiSourceContainer: {
              oaiSource: field.oaiSource
            },
            oaiSetContainer: {
              oaiSetId: field.oaiSetId,
              metadataConfigId: configId
            },
            harvestTypeContainer: {
              harvestType: field.harvestType
            }
          });
          this.contentSource = cloneDeep(field);
        }
        this.contentSource.metadataConfigId = configId;
      }
    });
  }

  /**
   * Fill the metadataConfigIdModel's options using the contentSource's metadataConfigs property
   */
  initializeMetadataConfigs() {
    this.metadataConfigIdModel.options = this.contentSource.metadataConfigs
      .map((metadataConfig: MetadataConfig) => Object.assign({ value: metadataConfig.id, label: metadataConfig.label }));
    if (this.metadataConfigIdModel.options.length > 0) {
      this.formGroup.patchValue({
        oaiSetContainer: {
          metadataConfigId: this.metadataConfigIdModel.options[0].value
        }
      });
    }
  }

  /**
   * Used the update translations of errors and labels on init and on language change
   */
  private updateFieldTranslations() {
    this.inputModels.forEach(
      (fieldModel: DynamicFormControlModel) => {
        this.updateFieldTranslation(fieldModel);
      }
    );
  }

  /**
   * Update the translations of a DynamicInputModel
   * @param fieldModel
   */
  private updateFieldTranslation(fieldModel: DynamicFormControlModel) {
    fieldModel.label = this.translate.instant(this.LABEL_KEY_PREFIX + fieldModel.id);
    if (isNotEmpty(fieldModel.validators)) {
      fieldModel.errorMessages = {};
      Object.keys(fieldModel.validators).forEach((key) => {
        fieldModel.errorMessages[key] = this.translate.instant(this.ERROR_KEY_PREFIX + fieldModel.id + '.' + key);
      });
    }
    if (fieldModel instanceof DynamicOptionControlModel) {
      if (isNotEmpty(fieldModel.options)) {
        fieldModel.options.forEach((option) => {
          if (hasNoValue(option.label)) {
            option.label = this.translate.instant(this.OPTIONS_KEY_PREFIX + fieldModel.id + '.' + option.value);
          }
        });
      }
    }
  }

  /**
   * Fired whenever the form receives an update and makes sure the Content Source and field update is up-to-date with the changes
   * @param event
   */
  onChange(event) {
    this.updateContentSourceField(event.model, true);
    this.saveFieldUpdate();
  }

  /**
   * Submit the edited Content Source to the REST API, re-initialize the field update and display a notification
   */
  onSubmit() {
    // Remove cached harvester request to allow for latest harvester to be displayed when switching tabs
    this.collectionRD$.pipe(
      getFirstSucceededRemoteData(),
      map((col) => col.payload.uuid),
      switchMap((uuid) => this.collectionService.getHarvesterEndpoint(uuid)),
      take(1)
    ).subscribe((endpoint) => this.requestService.removeByHrefSubstring(endpoint));
    this.requestService.setStaleByHrefSubstring(this.contentSource._links.self.href);
    // Update harvester
    this.collectionRD$.pipe(
      getFirstSucceededRemoteData(),
      map((col) => col.payload.uuid),
      switchMap((uuid) => this.collectionService.updateContentSource(uuid, this.contentSource)),
      take(1)
    ).subscribe((result: ContentSource | INotification) => {
      if (hasValue((result as any).harvestType)) {
        this.clearNotifications();
        this.initializeOriginalContentSource(result as ContentSource);
        this.displayedNotifications.push(this.notificationsService.success(this.getNotificationTitle('saved'), this.getNotificationContent('saved')));
      } else {
        this.displayedNotifications.push(result as INotification);
      }
    });
  }

  /**
   * Cancel the edit and return to the previous page
   */
  onCancel() {
    this.location.back();
  }

  /**
   * Is the current form valid to be submitted ?
   */
  isValid(): boolean {
    return (this.contentSource.harvestType === ContentSourceHarvestType.None) || this.formGroup.valid;
  }

  /**
   * Switch the external source on or off and fire a field update
   */
  changeExternalSource() {
    if (this.contentSource.harvestType === ContentSourceHarvestType.None) {
      this.contentSource.harvestType = this.previouslySelectedHarvestType;
    } else {
      this.previouslySelectedHarvestType = this.contentSource.harvestType;
      this.contentSource.harvestType = ContentSourceHarvestType.None;
    }
    this.updateContentSource(false);
  }

  /**
   * Loop over all inputs and update the Content Source with their value
   * @param updateHarvestType   When set to false, the harvestType of the contentSource will be ignored in the update
   */
  updateContentSource(updateHarvestType: boolean) {
    this.inputModels.forEach(
      (fieldModel: DynamicInputModel) => {
        this.updateContentSourceField(fieldModel, updateHarvestType);
      }
    );
    this.saveFieldUpdate();
  }

  /**
   * Update the Content Source with the value from a DynamicInputModel
   * @param fieldModel          The fieldModel to fetch the value from and update the contentSource with
   * @param updateHarvestType   When set to false, the harvestType of the contentSource will be ignored in the update
   */
  updateContentSourceField(fieldModel: DynamicInputModel, updateHarvestType: boolean) {
    if (hasValue(fieldModel.value) && !(fieldModel.id === this.harvestTypeModel.id && !updateHarvestType)) {
      this.contentSource[fieldModel.id] = fieldModel.value;
    }
  }

  /**
   * Save the current Content Source to the Object Updates cache
   */
  saveFieldUpdate() {
    this.objectUpdatesService.saveAddFieldUpdate(this.url, cloneDeep(this.contentSource));
  }

  /**
   * Clear possible active notifications
   */
  clearNotifications() {
    this.displayedNotifications.forEach((notification: INotification) => {
      this.notificationsService.remove(notification);
    });
    this.displayedNotifications = [];
  }

  /**
   * Make sure open subscriptions are closed
   */
  ngOnDestroy(): void {
    if (this.updateSub) {
      this.updateSub.unsubscribe();
    }
  }
}
