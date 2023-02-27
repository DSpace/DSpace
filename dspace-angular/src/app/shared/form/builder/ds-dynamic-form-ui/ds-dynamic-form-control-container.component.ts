import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ComponentFactoryResolver,
  ContentChildren,
  EventEmitter, Inject,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  QueryList,
  SimpleChanges,
  Type,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { FormArray, FormGroup } from '@angular/forms';

import {
  DYNAMIC_FORM_CONTROL_TYPE_ARRAY,
  DYNAMIC_FORM_CONTROL_TYPE_CHECKBOX,
  DYNAMIC_FORM_CONTROL_TYPE_CHECKBOX_GROUP,
  DYNAMIC_FORM_CONTROL_TYPE_DATEPICKER,
  DYNAMIC_FORM_CONTROL_TYPE_GROUP,
  DYNAMIC_FORM_CONTROL_TYPE_INPUT,
  DYNAMIC_FORM_CONTROL_TYPE_RADIO_GROUP,
  DYNAMIC_FORM_CONTROL_TYPE_SELECT,
  DYNAMIC_FORM_CONTROL_TYPE_TEXTAREA,
  DYNAMIC_FORM_CONTROL_TYPE_TIMEPICKER,
  DynamicDatePickerModel,
  DynamicFormArrayGroupModel,
  DynamicFormArrayModel,
  DynamicFormComponentService,
  DynamicFormControl,
  DynamicFormControlContainerComponent,
  DynamicFormControlEvent,
  DynamicFormControlEventType,
  DynamicFormControlModel,
  DynamicFormLayout,
  DynamicFormLayoutService,
  DynamicFormRelationService,
  DynamicFormValidationService,
  DynamicTemplateDirective,
} from '@ng-dynamic-forms/core';
import {
  DynamicNGBootstrapCalendarComponent,
  DynamicNGBootstrapCheckboxComponent,
  DynamicNGBootstrapCheckboxGroupComponent,
  DynamicNGBootstrapInputComponent,
  DynamicNGBootstrapRadioGroupComponent,
  DynamicNGBootstrapSelectComponent,
  DynamicNGBootstrapTextAreaComponent,
  DynamicNGBootstrapTimePickerComponent
} from '@ng-dynamic-forms/ui-ng-bootstrap';
import { TranslateService } from '@ngx-translate/core';
import { ReorderableRelationship } from './existing-metadata-list-element/existing-metadata-list-element.component';

import { DYNAMIC_FORM_CONTROL_TYPE_ONEBOX } from './models/onebox/dynamic-onebox.model';
import { DYNAMIC_FORM_CONTROL_TYPE_SCROLLABLE_DROPDOWN } from './models/scrollable-dropdown/dynamic-scrollable-dropdown.model';
import { DYNAMIC_FORM_CONTROL_TYPE_TAG } from './models/tag/dynamic-tag.model';
import { DYNAMIC_FORM_CONTROL_TYPE_DSDATEPICKER } from './models/date-picker/date-picker.model';
import { DYNAMIC_FORM_CONTROL_TYPE_LOOKUP } from './models/lookup/dynamic-lookup.model';
import { DynamicListCheckboxGroupModel } from './models/list/dynamic-list-checkbox-group.model';
import { DynamicListRadioGroupModel } from './models/list/dynamic-list-radio-group.model';
import { hasNoValue, hasValue, isNotEmpty, isNotUndefined } from '../../../empty.util';
import { DYNAMIC_FORM_CONTROL_TYPE_LOOKUP_NAME } from './models/lookup/dynamic-lookup-name.model';
import { DsDynamicTagComponent } from './models/tag/dynamic-tag.component';
import { DsDatePickerComponent } from './models/date-picker/date-picker.component';
import { DsDynamicListComponent } from './models/list/dynamic-list.component';
import { DsDynamicOneboxComponent } from './models/onebox/dynamic-onebox.component';
import { DsDynamicScrollableDropdownComponent } from './models/scrollable-dropdown/dynamic-scrollable-dropdown.component';
import { DsDynamicLookupComponent } from './models/lookup/dynamic-lookup.component';
import { DsDynamicFormGroupComponent } from './models/form-group/dynamic-form-group.component';
import { DsDynamicFormArrayComponent } from './models/array-group/dynamic-form-array.component';
import { DsDynamicRelationGroupComponent } from './models/relation-group/dynamic-relation-group.components';
import { DsDatePickerInlineComponent } from './models/date-picker-inline/dynamic-date-picker-inline.component';
import { DYNAMIC_FORM_CONTROL_TYPE_CUSTOM_SWITCH } from './models/custom-switch/custom-switch.model';
import { CustomSwitchComponent } from './models/custom-switch/custom-switch.component';
import { find, map, startWith, switchMap, take } from 'rxjs/operators';
import { combineLatest as observableCombineLatest, Observable, Subscription } from 'rxjs';
import { DsDynamicTypeBindRelationService } from './ds-dynamic-type-bind-relation.service';
import { SearchResult } from '../../../search/models/search-result.model';
import { DSpaceObject } from '../../../../core/shared/dspace-object.model';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { RelationshipDataService } from '../../../../core/data/relationship-data.service';
import { SelectableListService } from '../../../object-list/selectable-list/selectable-list.service';
import { DsDynamicDisabledComponent } from './models/disabled/dynamic-disabled.component';
import { DYNAMIC_FORM_CONTROL_TYPE_DISABLED } from './models/disabled/dynamic-disabled.model';
import { DsDynamicLookupRelationModalComponent } from './relation-lookup-modal/dynamic-lookup-relation-modal.component';
import {
  getAllSucceededRemoteData,
  getFirstSucceededRemoteData,
  getFirstSucceededRemoteDataPayload,
  getPaginatedListPayload,
  getRemoteDataPayload
} from '../../../../core/shared/operators';
import { RemoteData } from '../../../../core/data/remote-data';
import { Item } from '../../../../core/shared/item.model';
import { ItemDataService } from '../../../../core/data/item-data.service';
import { Store } from '@ngrx/store';
import { AppState } from '../../../../app.reducer';
import { SubmissionObjectDataService } from '../../../../core/submission/submission-object-data.service';
import { SubmissionObject } from '../../../../core/submission/models/submission-object.model';
import { PaginatedList } from '../../../../core/data/paginated-list.model';
import { ItemSearchResult } from '../../../object-collection/shared/item-search-result.model';
import { Relationship } from '../../../../core/shared/item-relationships/relationship.model';
import { Collection } from '../../../../core/shared/collection.model';
import { MetadataValue, VIRTUAL_METADATA_PREFIX } from '../../../../core/shared/metadata.models';
import { FormService } from '../../form.service';
import { SelectableListState } from '../../../object-list/selectable-list/selectable-list.reducer';
import { SubmissionService } from '../../../../submission/submission.service';
import { followLink } from '../../../utils/follow-link-config.model';
import { paginatedRelationsToItems } from '../../../../item-page/simple/item-types/shared/item-relationships-utils';
import { RelationshipOptions } from '../models/relationship-options.model';
import { FormBuilderService } from '../form-builder.service';
import { DYNAMIC_FORM_CONTROL_TYPE_RELATION_GROUP } from './ds-dynamic-form-constants';
import { FormFieldMetadataValueObject } from '../models/form-field-metadata-value.model';
import { APP_CONFIG, AppConfig } from '../../../../../config/app-config.interface';
import { itemLinksToFollow } from '../../../utils/relation-query.utils';

export function dsDynamicFormControlMapFn(model: DynamicFormControlModel): Type<DynamicFormControl> | null {
  switch (model.type) {
    case DYNAMIC_FORM_CONTROL_TYPE_ARRAY:
      return DsDynamicFormArrayComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_CHECKBOX:
      return DynamicNGBootstrapCheckboxComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_CHECKBOX_GROUP:
      return (model instanceof DynamicListCheckboxGroupModel) ? DsDynamicListComponent : DynamicNGBootstrapCheckboxGroupComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_DATEPICKER:
      const datepickerModel = model as DynamicDatePickerModel;

      return datepickerModel.inline ? DynamicNGBootstrapCalendarComponent : DsDatePickerInlineComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_GROUP:
      return DsDynamicFormGroupComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_INPUT:
      return DynamicNGBootstrapInputComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_RADIO_GROUP:
      return (model instanceof DynamicListRadioGroupModel) ? DsDynamicListComponent : DynamicNGBootstrapRadioGroupComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_SELECT:
      return DynamicNGBootstrapSelectComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_TEXTAREA:
      return DynamicNGBootstrapTextAreaComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_TIMEPICKER:
      return DynamicNGBootstrapTimePickerComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_ONEBOX:
      return DsDynamicOneboxComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_SCROLLABLE_DROPDOWN:
      return DsDynamicScrollableDropdownComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_TAG:
      return DsDynamicTagComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_RELATION_GROUP:
      return DsDynamicRelationGroupComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_DSDATEPICKER:
      return DsDatePickerComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_LOOKUP:
      return DsDynamicLookupComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_LOOKUP_NAME:
      return DsDynamicLookupComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_DISABLED:
      return DsDynamicDisabledComponent;

    case DYNAMIC_FORM_CONTROL_TYPE_CUSTOM_SWITCH:
      return CustomSwitchComponent;

    default:
      return null;
  }
}

@Component({
  selector: 'ds-dynamic-form-control-container',
  styleUrls: ['./ds-dynamic-form-control-container.component.scss'],
  templateUrl: './ds-dynamic-form-control-container.component.html',
  changeDetection: ChangeDetectionStrategy.Default
})
export class DsDynamicFormControlContainerComponent extends DynamicFormControlContainerComponent implements OnInit, OnChanges, OnDestroy {
  @ContentChildren(DynamicTemplateDirective) contentTemplateList: QueryList<DynamicTemplateDirective>;
  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('templates') inputTemplateList: QueryList<DynamicTemplateDirective>;
  @Input() hasMetadataModel: any;
  @Input() formId: string;
  @Input() formGroup: FormGroup;
  @Input() formModel: DynamicFormControlModel[];
  @Input() asBootstrapFormGroup = false;
  @Input() bindId = true;
  @Input() context: any | null = null;
  @Input() group: FormGroup;
  @Input() hostClass: string[];
  @Input() hasErrorMessaging = false;
  @Input() layout = null as DynamicFormLayout;
  @Input() model: any;
  relationshipValue$: Observable<ReorderableRelationship>;
  isRelationship: boolean;
  modalRef: NgbModalRef;
  item: Item;
  item$: Observable<Item>;
  collection: Collection;
  listId: string;
  searchConfig: string;
  value: MetadataValue;
  /**
   * List of subscriptions to unsubscribe from
   */
  private subs: Subscription[] = [];

  /* eslint-disable @angular-eslint/no-output-rename */
  @Output('dfBlur') blur: EventEmitter<DynamicFormControlEvent> = new EventEmitter<DynamicFormControlEvent>();
  @Output('dfChange') change: EventEmitter<DynamicFormControlEvent> = new EventEmitter<DynamicFormControlEvent>();
  @Output('dfFocus') focus: EventEmitter<DynamicFormControlEvent> = new EventEmitter<DynamicFormControlEvent>();
  @Output('ngbEvent') customEvent: EventEmitter<DynamicFormControlEvent> = new EventEmitter<DynamicFormControlEvent>();
  /* eslint-enable @angular-eslint/no-output-rename */
  @ViewChild('componentViewContainer', { read: ViewContainerRef, static: true }) componentViewContainerRef: ViewContainerRef;

  private showErrorMessagesPreviousStage: boolean;

  /**
   * Determines whether to request embedded thumbnail.
   */
  fetchThumbnail: boolean;

  get componentType(): Type<DynamicFormControl> | null {
    return dsDynamicFormControlMapFn(this.model);
  }

  constructor(
    protected componentFactoryResolver: ComponentFactoryResolver,
    protected dynamicFormComponentService: DynamicFormComponentService,
    protected layoutService: DynamicFormLayoutService,
    protected validationService: DynamicFormValidationService,
    protected typeBindRelationService: DsDynamicTypeBindRelationService,
    protected translateService: TranslateService,
    protected relationService: DynamicFormRelationService,
    private modalService: NgbModal,
    private relationshipService: RelationshipDataService,
    private selectableListService: SelectableListService,
    private itemService: ItemDataService,
    private zone: NgZone,
    private store: Store<AppState>,
    private submissionObjectService: SubmissionObjectDataService,
    private ref: ChangeDetectorRef,
    private formService: FormService,
    private formBuilderService: FormBuilderService,
    private submissionService: SubmissionService,
    @Inject(APP_CONFIG) protected appConfig: AppConfig,
  ) {
    super(ref, componentFactoryResolver, layoutService, validationService, dynamicFormComponentService, relationService);
    this.fetchThumbnail = this.appConfig.browseBy.showThumbnails;
  }

  /**
   * Sets up the necessary variables for when this control can be used to add relationships to the submitted item
   */
  ngOnInit(): void {
    this.isRelationship = hasValue(this.model.relationship);
    const isWrapperAroundRelationshipList = hasValue(this.model.relationshipConfig);

    if (this.isRelationship || isWrapperAroundRelationshipList) {
      const config = this.model.relationshipConfig || this.model.relationship;
      const relationshipOptions = Object.assign(new RelationshipOptions(), config);
      this.listId = `list-${this.model.submissionId}-${relationshipOptions.relationshipType}`;
      this.setItem();

      if (isWrapperAroundRelationshipList || !this.model.repeatable) {
        const subscription = this.selectableListService.getSelectableList(this.listId).pipe(
          find((list: SelectableListState) => hasNoValue(list)),
          switchMap(() => this.item$.pipe(take(1))),
          switchMap((item) => {
            const relationshipsRD$ = this.relationshipService.getItemRelationshipsByLabel(item,
              relationshipOptions.relationshipType,
              undefined,
              true,
              true,
              followLink('leftItem'),
              followLink('rightItem'),
              followLink('relationshipType')
            );
            relationshipsRD$.pipe(
              getFirstSucceededRemoteDataPayload(),
              getPaginatedListPayload()
            ).subscribe((relationships: Relationship[]) => {
              // set initial namevariants for pre-existing relationships
              relationships.forEach((relationship: Relationship) => {
                const relationshipMD: MetadataValue = item.firstMetadata(relationshipOptions.metadataField, { authority: `${VIRTUAL_METADATA_PREFIX}${relationship.id}` });
                const nameVariantMD: MetadataValue = item.firstMetadata(this.model.metadataFields, { authority: `${VIRTUAL_METADATA_PREFIX}${relationship.id}` });
                if (hasValue(relationshipMD) && isNotEmpty(relationshipMD.value) && hasValue(nameVariantMD) && isNotEmpty(nameVariantMD.value)) {
                  this.relationshipService.setNameVariant(this.listId, relationshipMD.value, nameVariantMD.value);
                }
              });
            });

            return relationshipsRD$.pipe(
              paginatedRelationsToItems(item.uuid),
              getFirstSucceededRemoteData(),
              map((items: RemoteData<PaginatedList<Item>>) => items.payload.page.map((i) => Object.assign(new ItemSearchResult(), { indexableObject: i }))),
            );
          })
        ).subscribe((relatedItems: SearchResult<Item>[]) => this.selectableListService.select(this.listId, relatedItems));
        this.subs.push(subscription);
      }

      if (hasValue(this.model.metadataValue)) {
        this.value = Object.assign(new FormFieldMetadataValueObject(), this.model.metadataValue);
      } else {
        this.value = Object.assign(new FormFieldMetadataValueObject(), this.model.value);
      }

      if (hasValue(this.value) && this.value.isVirtual) {
        const relationship$ = this.relationshipService.findById(this.value.virtualValue,
          true,
          true,
          ... itemLinksToFollow(this.fetchThumbnail)).pipe(
            getAllSucceededRemoteData(),
            getRemoteDataPayload());
        this.relationshipValue$ = observableCombineLatest([this.item$.pipe(take(1)), relationship$]).pipe(
          switchMap(([item, relationship]: [Item, Relationship]) =>
            relationship.leftItem.pipe(
              getAllSucceededRemoteData(),
              getRemoteDataPayload(),
              map((leftItem: Item) => {
                return new ReorderableRelationship(relationship, leftItem.uuid !== item.uuid, this.store, this.model.submissionId);
              }),
            )
          ),
          startWith(undefined)
        );
      }
    }
  }

  get isCheckbox(): boolean {
    return this.model.type === DYNAMIC_FORM_CONTROL_TYPE_CHECKBOX || this.model.type === DYNAMIC_FORM_CONTROL_TYPE_CUSTOM_SWITCH;
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes && !this.isRelationship && hasValue(this.group.get(this.model.id))) {
      super.ngOnChanges(changes);
      if (this.model && this.model.placeholder) {
        this.model.placeholder = this.translateService.instant(this.model.placeholder);
      }
      if (this.model.typeBindRelations && this.model.typeBindRelations.length > 0) {
        this.subscriptions.push(...this.typeBindRelationService.subscribeRelations(this.model, this.control));
      }
    }
  }

  ngDoCheck() {
    if (isNotUndefined(this.showErrorMessagesPreviousStage) && this.showErrorMessagesPreviousStage !== this.showErrorMessages) {
      this.showErrorMessagesPreviousStage = this.showErrorMessages;
      this.forceShowErrorDetection();
    }
  }

  ngAfterViewInit() {
    this.showErrorMessagesPreviousStage = this.showErrorMessages;
  }

  protected createFormControlComponent(): void {
    super.createFormControlComponent();
    if (this.componentType !== null) {
      let index;

      if (this.context && this.context instanceof DynamicFormArrayGroupModel) {
        index = this.context.index;
      }
      const instance = this.dynamicFormComponentService.getFormControlRef(this.model, index);
      if (instance) {
        (instance as any).formModel = this.formModel;
        (instance as any).formGroup = this.formGroup;
      }
    }
  }

  /**
   * Since Form Control Components created dynamically have 'OnPush' change detection strategy,
   * changes are not propagated. So use this method to force an update
   */
  protected forceShowErrorDetection() {
    if (this.showErrorMessages) {
      this.destroyFormControlComponent();
      this.createFormControlComponent();
    }
  }

  onChangeLanguage(event) {
    if (isNotEmpty((this.model as any).value)) {
      this.onChange(event);
    }
  }

  hasRelationship() {
    return isNotEmpty(this.model) && this.model.hasOwnProperty('relationship') && isNotEmpty(this.model.relationship);
  }

  isVirtual() {
    const value: FormFieldMetadataValueObject = this.model.metadataValue;
    return isNotEmpty(value) && value.isVirtual;
  }

  public hasResultsSelected(): Observable<boolean> {
    return this.model.value.pipe(map((list: SearchResult<DSpaceObject>[]) => isNotEmpty(list)));
  }

  /**
   * Open a modal where the user can select relationships to be added to item being submitted
   */
  openLookup() {
    this.modalRef = this.modalService.open(DsDynamicLookupRelationModalComponent, {
      size: 'lg'
    });

    if (hasValue(this.model.value)) {
      this.focus.emit({
        $event: new Event('focus'),
        context: this.context,
        control: this.control,
        model: this.model,
        type: DynamicFormControlEventType.Focus
      } as DynamicFormControlEvent);

      this.change.emit({
        $event: new Event('change'),
        context: this.context,
        control: this.control,
        model: this.model,
        type: DynamicFormControlEventType.Change
      } as DynamicFormControlEvent);
    }

    this.submissionService.dispatchSave(this.model.submissionId);

    const modalComp = this.modalRef.componentInstance;

    if (hasValue(this.model.value) && !this.model.readOnly) {
      if (typeof this.model.value === 'string') {
        modalComp.query = this.model.value;
      } else if (typeof this.model.value.value === 'string') {
        modalComp.query = this.model.value.value;
      }
    }

    modalComp.repeatable = this.model.repeatable;
    modalComp.listId = this.listId;
    modalComp.relationshipOptions = this.model.relationship;
    modalComp.label = this.model.relationship.relationshipType;
    modalComp.metadataFields = this.model.metadataFields;
    modalComp.item = this.item;
    modalComp.collection = this.collection;
    modalComp.submissionId = this.model.submissionId;
  }

  /**
   * Callback for the remove event,
   * remove the current control from its array
   */
  onRemove(): void {
    const arrayContext: DynamicFormArrayModel = (this.context as DynamicFormArrayGroupModel).context;
    const path = this.formBuilderService.getPath(arrayContext);
    const formArrayControl = this.group.root.get(path) as FormArray;
    this.formBuilderService.removeFormArrayGroup(this.context.index, formArrayControl, arrayContext);
    if (this.model.parent.context.groups.length === 0) {
      this.formBuilderService.addFormArrayGroup(formArrayControl, arrayContext);
    }
  }

  /**
   * Unsubscribe from all subscriptions
   */
  ngOnDestroy(): void {
    this.subs
      .filter((sub) => hasValue(sub))
      .forEach((sub) => sub.unsubscribe());
  }

  get hasHint(): boolean {
    return isNotEmpty(this.model.hint) && this.model.hint !== '&nbsp;';
  }

  /**
   *  Initialize this.item$ based on this.model.submissionId
   */
  private setItem() {
    const submissionObject$ = this.submissionObjectService
      .findById(this.model.submissionId, true, true, followLink('item'), followLink('collection')).pipe(
        getAllSucceededRemoteData(),
        getRemoteDataPayload()
      );

    this.item$ = submissionObject$.pipe(switchMap((submissionObject: SubmissionObject) => (submissionObject.item as Observable<RemoteData<Item>>).pipe(getAllSucceededRemoteData(), getRemoteDataPayload())));
    const collection$ = submissionObject$.pipe(switchMap((submissionObject: SubmissionObject) => (submissionObject.collection as Observable<RemoteData<Collection>>).pipe(getAllSucceededRemoteData(), getRemoteDataPayload())));

    this.subs.push(this.item$.subscribe((item) => this.item = item));
    this.subs.push(collection$.subscribe((collection) => this.collection = collection));

  }
}
