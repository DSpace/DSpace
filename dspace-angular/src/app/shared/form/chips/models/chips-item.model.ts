import isObject from 'lodash/isObject';
import uniqueId from 'lodash/uniqueId';
import { hasValue, isNotEmpty } from '../../../empty.util';
import { FormFieldMetadataValueObject } from '../../builder/models/form-field-metadata-value.model';
import { ConfidenceType } from '../../../../core/shared/confidence-type';
import { PLACEHOLDER_PARENT_METADATA } from '../../builder/ds-dynamic-form-ui/ds-dynamic-form-constants';

export interface ChipsItemIcon {
  metadata: string;
  style: string;
  visibleWhenAuthorityEmpty: boolean;
  tooltip?: any;
}

export class ChipsItem {
  public id: string;
  public display: string;
  private _item: any;
  public editMode?: boolean;
  public icons?: ChipsItemIcon[];

  private fieldToDisplay: string;
  private objToDisplay: string;

  constructor(item: any,
              fieldToDisplay: string = 'display',
              objToDisplay?: string,
              icons?: ChipsItemIcon[],
              editMode?: boolean) {

    this.id = uniqueId();
    this._item = item;
    this.fieldToDisplay = fieldToDisplay;
    this.objToDisplay = objToDisplay;
    this.setDisplayText();
    this.editMode = editMode || false;
    this.icons = icons || [];
  }

  public set item(item) {
    this._item = item;
  }

  public get item() {
    return this._item;
  }

  isNestedItem(): boolean {
    return (isNotEmpty(this.item)
      && isObject(this.item)
      && isNotEmpty(this.objToDisplay)
      && this.item[this.objToDisplay]);
  }

  hasIcons(): boolean {
     return isNotEmpty(this.icons);
  }

  hasVisibleIcons(): boolean {
    if (isNotEmpty(this.icons)) {
      let hasVisible = false;
      // check if it has at least one visible icon
      for (const icon of this.icons) {
        if (this._item.hasOwnProperty(icon.metadata)
          && (((typeof this._item[icon.metadata] === 'string') && hasValue(this._item[icon.metadata]))
            || (this._item[icon.metadata] as FormFieldMetadataValueObject).hasValue())
          && !this.hasPlaceholder(this._item[icon.metadata])) {
          if ((icon.visibleWhenAuthorityEmpty
            || (this._item[icon.metadata] as FormFieldMetadataValueObject).confidence !== ConfidenceType.CF_UNSET)
            && isNotEmpty(icon.style)) {
            hasVisible = true;
            break;
          }
        }
      }
      return hasVisible;
    } else {
      return false;
    }
  }

  setEditMode(): void {
    this.editMode = true;
  }

  updateIcons(icons: ChipsItemIcon[]): void {
    this.icons = icons;
  }

  updateItem(item: any): void {
    this._item = item;
    this.setDisplayText();
  }

  unsetEditMode(): void {
    this.editMode = false;
  }

  private setDisplayText(): void {
    let value = this._item;
    if (isObject(this._item)) {
      // Check If displayField is in an internal object
      const obj = this.objToDisplay ? this._item[this.objToDisplay] : this._item;

      if (isObject(obj) && obj) {
        value = obj[this.fieldToDisplay] || (obj as any).value;
      } else {
        value = obj;
      }
    }

    this.display = value;
  }

  private hasPlaceholder(value: any) {
    return (typeof value === 'string') ? (value === PLACEHOLDER_PARENT_METADATA) :
      (value as FormFieldMetadataValueObject).hasPlaceholder();
  }
}
