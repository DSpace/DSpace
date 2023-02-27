import { Component, Inject, Injector, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { AlertType } from '../../shared/alert/aletr-type';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { DsoEditMetadataForm } from './dso-edit-metadata-form';
import { map } from 'rxjs/operators';
import { ActivatedRoute, Data } from '@angular/router';
import { combineLatest as observableCombineLatest } from 'rxjs/internal/observable/combineLatest';
import { Subscription } from 'rxjs/internal/Subscription';
import { RemoteData } from '../../core/data/remote-data';
import { hasNoValue, hasValue } from '../../shared/empty.util';
import { BehaviorSubject } from 'rxjs/internal/BehaviorSubject';
import {
  getFirstCompletedRemoteData,
} from '../../core/shared/operators';
import { UpdateDataService } from '../../core/data/update-data.service';
import { ResourceType } from '../../core/shared/resource-type';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { MetadataFieldSelectorComponent } from './metadata-field-selector/metadata-field-selector.component';
import { Observable } from 'rxjs/internal/Observable';
import { ArrayMoveChangeAnalyzer } from '../../core/data/array-move-change-analyzer.service';
import { DATA_SERVICE_FACTORY } from '../../core/data/base/data-service.decorator';
import { GenericConstructor } from '../../core/shared/generic-constructor';
import { HALDataService } from '../../core/data/base/hal-data-service.interface';

@Component({
  selector: 'ds-dso-edit-metadata',
  styleUrls: ['./dso-edit-metadata.component.scss'],
  templateUrl: './dso-edit-metadata.component.html',
})
/**
 * Component showing a table of all metadata on a DSpaceObject and options to modify them
 */
export class DsoEditMetadataComponent implements OnInit, OnDestroy {
  /**
   * DSpaceObject to edit metadata for
   */
  @Input() dso: DSpaceObject;

  /**
   * Reference to the component responsible for showing a metadata-field selector
   * Used to validate its contents (existing metadata field) before adding a new metadata value
   */
  @ViewChild(MetadataFieldSelectorComponent) metadataFieldSelectorComponent: MetadataFieldSelectorComponent;

  /**
   * Resolved update data-service for the given DSpaceObject (depending on its type, e.g. ItemDataService for an Item)
   * Used to send the PATCH request
   */
  @Input() updateDataService: UpdateDataService<DSpaceObject>;

  /**
   * Type of the DSpaceObject in String
   * Used to resolve i18n messages
   */
  dsoType: string;

  /**
   * A dynamic form object containing all information about the metadata and the changes made to them, see {@link DsoEditMetadataForm}
   */
  form: DsoEditMetadataForm;

  /**
   * The metadata field entered by the user for a new metadata value
   */
  newMdField: string;

  // Properties determined by the state of the dynamic form, updated by onValueSaved()
  isReinstatable: boolean;
  hasChanges: boolean;
  isEmpty: boolean;

  /**
   * Whether or not the form is currently being submitted
   */
  saving$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  /**
   * Tracks for which metadata-field a drag operation is taking place
   * Null when no drag is currently happening for any field
   * This is a BehaviorSubject that is passed down to child components, to give them the power to alter the state
   */
  draggingMdField$: BehaviorSubject<string> = new BehaviorSubject<string>(null);

  /**
   * Whether or not the metadata field is currently being validated
   */
  loadingFieldValidation$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  /**
   * Combination of saving$ and loadingFieldValidation$
   * Emits true when any of the two emit true
   */
  savingOrLoadingFieldValidation$: Observable<boolean>;

  /**
   * The AlertType enumeration for access in the component's template
   * @type {AlertType}
   */
  public AlertTypeEnum = AlertType;

  /**
   * Subscription for updating the current DSpaceObject
   * Unsubscribed from in ngOnDestroy()
   */
  dsoUpdateSubscription: Subscription;

  constructor(protected route: ActivatedRoute,
              protected notificationsService: NotificationsService,
              protected translateService: TranslateService,
              protected parentInjector: Injector,
              protected arrayMoveChangeAnalyser: ArrayMoveChangeAnalyzer<number>,
              @Inject(DATA_SERVICE_FACTORY) protected getDataServiceFor: (resourceType: ResourceType) => GenericConstructor<HALDataService<any>>) {
  }

  /**
   * Read the route (or parent route)'s data to retrieve the current DSpaceObject
   * After it's retrieved, initialise the data-service and form
   */
  ngOnInit(): void {
    if (hasNoValue(this.dso)) {
      this.dsoUpdateSubscription = observableCombineLatest([this.route.data, this.route.parent.data]).pipe(
        map(([data, parentData]: [Data, Data]) => Object.assign({}, data, parentData)),
        map((data: any) => data.dso)
      ).subscribe((rd: RemoteData<DSpaceObject>) => {
        this.dso = rd.payload;
        this.initDataService();
        this.initForm();
      });
    } else {
      this.initDataService();
      this.initForm();
    }
    this.savingOrLoadingFieldValidation$ = observableCombineLatest([this.saving$, this.loadingFieldValidation$]).pipe(
      map(([saving, loading]: [boolean, boolean]) => saving || loading),
    );
  }

  /**
   * Initialise (resolve) the data-service for the current DSpaceObject
   */
  initDataService(): void {
    let type: ResourceType;
    if (typeof this.dso.type === 'string') {
      type = new ResourceType(this.dso.type);
    } else {
      type = this.dso.type;
    }
    if (hasNoValue(this.updateDataService)) {
      const provider = this.getDataServiceFor(type);
      this.updateDataService = Injector.create({
        providers: [],
        parent: this.parentInjector
      }).get(provider);
    }
    this.dsoType = type.value;
  }

  /**
   * Initialise the dynamic form object by passing the DSpaceObject's metadata
   * Call onValueSaved() to update the form's state properties
   */
  initForm(): void {
    this.form = new DsoEditMetadataForm(this.dso.metadata);
    this.onValueSaved();
  }

  /**
   * Update the form's state properties
   */
  onValueSaved(): void {
    this.hasChanges = this.form.hasChanges();
    this.isReinstatable = this.form.isReinstatable();
    this.isEmpty = Object.keys(this.form.fields).length === 0;
  }

  /**
   * Submit the current changes to the form by retrieving json PATCH operations from the form and sending it to the
   * DSpaceObject's data-service
   * Display notificiations and reset the form afterwards if successful
   */
  submit(): void {
    this.saving$.next(true);
    this.updateDataService.patch(this.dso, this.form.getOperations(this.arrayMoveChangeAnalyser)).pipe(
      getFirstCompletedRemoteData()
    ).subscribe((rd: RemoteData<DSpaceObject>) => {
      this.saving$.next(false);
      if (rd.hasFailed) {
        this.notificationsService.error(this.translateService.instant(`${this.dsoType}.edit.metadata.notifications.error.title`), rd.errorMessage);
      } else {
        this.notificationsService.success(
            this.translateService.instant(`${this.dsoType}.edit.metadata.notifications.saved.title`),
            this.translateService.instant(`${this.dsoType}.edit.metadata.notifications.saved.content`)
        );
        this.dso = rd.payload;
        this.initForm();
      }
    });
  }

  /**
   * Confirm the newly added value
   * @param saved Whether or not the value was manually saved (only then, add the value to its metadata field)
   */
  confirmNewValue(saved: boolean): void {
    if (saved) {
      this.setMetadataField();
    }
  }

  /**
   * Set the metadata field of the temporary added new metadata value
   * This will move the new value to its respective parent metadata field
   * Validate the metadata field first
   */
  setMetadataField(): void {
    this.form.resetReinstatable();
    this.loadingFieldValidation$.next(true);
    this.metadataFieldSelectorComponent.validate().subscribe((valid: boolean) => {
      this.loadingFieldValidation$.next(false);
      if (valid) {
        this.form.setMetadataField(this.newMdField);
        this.onValueSaved();
      }
    });
  }

  /**
   * Add a new temporary metadata value
   */
  add(): void {
    this.newMdField = undefined;
    this.form.add();
  }

  /**
   * Discard all changes within the current form
   */
  discard(): void {
    this.form.discard();
    this.onValueSaved();
  }

  /**
   * Restore any changes previously discarded from the form
   */
  reinstate(): void {
    this.form.reinstate();
    this.onValueSaved();
  }

  /**
   * Unsubscribe from any open subscriptions
   */
  ngOnDestroy(): void {
    if (hasValue(this.dsoUpdateSubscription)) {
      this.dsoUpdateSubscription.unsubscribe();
    }
  }

}
