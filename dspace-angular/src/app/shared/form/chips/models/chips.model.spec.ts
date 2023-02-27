import { Chips } from './chips.model';
import { ChipsItem } from './chips-item.model';
import { FormFieldMetadataValueObject } from '../../builder/models/form-field-metadata-value.model';

describe('Chips model test suite', () => {
  let items: any[];
  let item: ChipsItem;
  let chips: Chips;

  beforeEach(() => {
    items = ['a', 'b', 'c'];
    chips = new Chips(items);
  });

  it('should init Chips object properly', () => {
    expect(chips.getChipsItems()).toEqual(items);
    expect(chips.displayField).toBe('display');
    expect(chips.displayObj).toBe(undefined);
    expect(chips.iconsConfig).toEqual([]);
  });

  it('should add an element to items', () => {
    items = ['a', 'b', 'c', 'd'];
    chips.add('d');
    expect(chips.getChipsItems()).toEqual(items);
  });

  it('should remove an element from items', () => {
    items = ['a', 'c'];
    item = chips.getChipByIndex(1);
    chips.remove(item);
    expect(chips.getChipsItems()).toEqual(items);
  });

  it('should update an item', () => {
    items = ['a', 'd', 'c'];
    const id = chips.getChipByIndex(1).id;
    chips.update(id, 'd');
    expect(chips.getChipsItems()).toEqual(items);
  });

  it('should update items order', () => {
    items = ['a', 'c', 'b'];
    const chipsItems = chips.getChips();
    const b = chipsItems[1];
    chipsItems[1] = chipsItems[2];
    chipsItems[2] = b;
    chips.updateOrder();
    expect(chips.getChipsItems()).toEqual(items);
  });

  it('should set a different displayField', () => {
    items = [
      {
        label: 'A',
        value: 'a'
      },
      {
        label: 'B',
        value: 'b'
      },
      {
        label: 'C',
        value: 'c'
      },
    ];
    chips = new Chips(items, 'label');
    expect(chips.displayField).toBe('label');
    expect(chips.getChipsItems()).toEqual(items);
  });

  it('should set a different displayObj', () => {
    items = [
      {
        toDisplay: new FormFieldMetadataValueObject('a', null, 'a'),
        otherProperty: 'a'
      },
      {
        toDisplay: new FormFieldMetadataValueObject('a', null, 'a'),
        otherProperty: 'a'
      },
      {
        toDisplay: new FormFieldMetadataValueObject('a', null, 'a'),
        otherProperty: 'a'
      },
    ];
    chips = new Chips(items, 'value', 'toDisplay');
    expect(chips.displayField).toBe('value');
    expect(chips.displayObj).toBe('toDisplay');
    expect(chips.getChipsItems()).toEqual(items);
  });

  it('should set iconsConfig', () => {
    items = [
      {
        toDisplay: new FormFieldMetadataValueObject('a', null, 'a'),
        otherProperty: 'a'
      },
      {
        toDisplay: new FormFieldMetadataValueObject('a', null, 'a'),
        otherProperty: 'a'
      },
      {
        toDisplay: new FormFieldMetadataValueObject('a', null, 'a'),
        otherProperty: 'a'
      },
    ];
    const iconsConfig = [{
      name: 'toDisplay',
      visibleWhenAuthorityEmpty: false,
      style: 'fa-user'
    }];
    chips = new Chips(items, 'value', 'toDisplay', iconsConfig);

    expect(chips.displayField).toBe('value');
    expect(chips.displayObj).toBe('toDisplay');
    expect(chips.iconsConfig).toEqual(iconsConfig);
    expect(chips.getChipsItems()).toEqual(items);
  });
});
