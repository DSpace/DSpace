import findIndex from 'lodash/findIndex';
import isEqual from 'lodash/isEqual';
import isObject from 'lodash/isObject';
import { BehaviorSubject } from 'rxjs';
import { ChipsItem, ChipsItemIcon } from './chips-item.model';
import { hasValue, isNotEmpty } from '../../../empty.util';
import { MetadataIconConfig } from '../../../../../config/submission-config.interface';
import { FormFieldMetadataValueObject } from '../../builder/models/form-field-metadata-value.model';
import { VocabularyEntry } from '../../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { PLACEHOLDER_PARENT_METADATA } from '../../builder/ds-dynamic-form-ui/ds-dynamic-form-constants';

export class Chips {
  chipsItems: BehaviorSubject<ChipsItem[]>;
  displayField: string;
  displayObj: string;
  iconsConfig: MetadataIconConfig[];

  private _items: ChipsItem[];

  constructor(items: any[] = [],
              displayField: string = 'display',
              displayObj?: string,
              iconsConfig?: MetadataIconConfig[]) {

    this.displayField = displayField;
    this.displayObj = displayObj;
    this.iconsConfig = iconsConfig || [];
    if (Array.isArray(items)) {
      this.setInitialItems(items);
    }
  }

  public add(item: any): void {
    const icons = this.getChipsIcons(item);
    const chipsItem = new ChipsItem(item, this.displayField, this.displayObj, icons);

    const duplicatedIndex = findIndex(this._items, {display: chipsItem.display.trim()});
    if (duplicatedIndex === -1 || !isEqual(item, this.getChipByIndex(duplicatedIndex).item)) {
      this._items.push(chipsItem);
      this.chipsItems.next(this._items);
    }
  }

  public getChipById(id): ChipsItem {
    const index = findIndex(this._items, {id: id});
    return this.getChipByIndex(index);
  }

  public getChipByIndex(index): ChipsItem {
    if (this._items.length > 0 && this._items[index]) {
      return this._items[index];
    } else {
      return null;
    }
  }

  public getChips(): ChipsItem[] {
    return this._items;
  }

  /**
   * To use to get items before to store it
   * @returns {any[]}
   */
  public getChipsItems(): any[] {
    const out = [];
    this._items.forEach((item) => {
      out.push(item.item);
    });
    return out;
  }

  public hasItems(): boolean {
    return this._items.length > 0;
  }

  private hasPlaceholder(value) {
    if (isObject(value)) {
      return (value as any).value === PLACEHOLDER_PARENT_METADATA;
    } else {
      return value === PLACEHOLDER_PARENT_METADATA;
    }
  }

  public remove(chipsItem: ChipsItem): void {
    const index = findIndex(this._items, {id: chipsItem.id});
    this._items.splice(index, 1);
    this.chipsItems.next(this._items);
  }

  public update(id: string, item: any): void {
    const chipsItemTarget = this.getChipById(id);
    const icons = this.getChipsIcons(item);

    chipsItemTarget.updateItem(item);
    chipsItemTarget.updateIcons(icons);
    chipsItemTarget.unsetEditMode();
    this.chipsItems.next(this._items);
  }

  public updateOrder(): void {
    this.chipsItems.next(this._items);
  }

  private getChipsIcons(item) {
    const icons = [];
    if (typeof item === 'string' || item instanceof FormFieldMetadataValueObject || item instanceof VocabularyEntry) {
      return icons;
    }

    const defaultConfigIndex: number = findIndex(this.iconsConfig, {name: 'default'});
    const defaultConfig: MetadataIconConfig = (defaultConfigIndex !== -1) ? this.iconsConfig[defaultConfigIndex] : undefined;
    let config: MetadataIconConfig;
    let configIndex: number;
    let value: any;

    Object.keys(item)
      .forEach((metadata) => {

        value = item[metadata];
        configIndex = findIndex(this.iconsConfig, {name: metadata});

        config = (configIndex !== -1) ? this.iconsConfig[configIndex] : defaultConfig;

        if (hasValue(value) && isNotEmpty(config) && !this.hasPlaceholder(value)) {

          let icon: ChipsItemIcon;
          const visibleWhenAuthorityEmpty = this.displayObj !== metadata;

          // Set icon
          icon = {
            metadata,
            visibleWhenAuthorityEmpty,
            style: config.style
          };

          icons.push(icon);
        }
      });

    return icons;
  }

  /**
   * Sets initial items, used in edit mode
   */
  private setInitialItems(items: any[]): void {
    this._items = [];
    items.forEach((item) => {
      const icons = this.getChipsIcons(item);
      const chipsItem = new ChipsItem(item, this.displayField, this.displayObj, icons);
      this._items.push(chipsItem);
    });

    this.chipsItems = new BehaviorSubject<ChipsItem[]>(this._items);
  }
}
