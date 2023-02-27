import { ChipsItem, ChipsItemIcon } from './chips-item.model';
import { FormFieldMetadataValueObject } from '../../builder/models/form-field-metadata-value.model';

describe('ChipsItem model test suite', () => {
  let item: ChipsItem;

  beforeEach(() => {
    item = new ChipsItem('a');
  });

  it('should init ChipsItem object properly', () => {
    expect(item.item).toBe('a');
    expect(item.display).toBe('a');
    expect(item.editMode).toBe(false);
    expect(item.icons).toEqual([]);
  });

  it('should update item', () => {
    item.updateItem('b');

    expect(item.item).toBe('b');
  });

  it('should set editMode', () => {
    item.setEditMode();

    expect(item.editMode).toBe(true);
  });

  it('should unset editMode', () => {
    item.unsetEditMode();

    expect(item.editMode).toBe(false);
  });

  it('should update icons', () => {
    const icons: ChipsItemIcon[] = [{ metadata: 'test', visibleWhenAuthorityEmpty: false, style: 'fas fa-plus' }];
    item.updateIcons(icons);

    expect(item.icons).toEqual(icons);
  });

  it('should return true if has icons', () => {
    const icons: ChipsItemIcon[] = [{ metadata: 'test', visibleWhenAuthorityEmpty: false, style: 'fas fa-plus' }];
    item.updateIcons(icons);
    const hasIcons = item.hasIcons();

    expect(hasIcons).toBe(true);
  });

  it('should return false if has not icons', () => {
    const hasIcons = item.hasIcons();

    expect(hasIcons).toBe(false);
  });

  it('should set display property with a different fieldToDisplay', () => {
    item = new ChipsItem(
      {
        label: 'A',
        value: 'a'
      },
      'label');

    expect(item.display).toBe('A');
  });

  it('should set display property with a different objToDisplay', () => {
    item = new ChipsItem(
      {
        toDisplay: new FormFieldMetadataValueObject('a', null, 'a'),
        otherProperty: 'other'
      },
      'value', 'toDisplay');

    expect(item.display).toBe('a');
  });
});
