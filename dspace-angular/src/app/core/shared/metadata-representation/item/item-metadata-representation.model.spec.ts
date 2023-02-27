import { MetadataRepresentationType } from '../metadata-representation.model';
import { ItemMetadataRepresentation } from './item-metadata-representation.model';
import { Item } from '../../item.model';
import { MetadataValue } from '../../metadata.models';

describe('ItemMetadataRepresentation', () => {
  const valuePrefix = 'Test value for ';
  const item = new Item();
  const itemType = 'Item Type';
  let itemMetadataRepresentation: ItemMetadataRepresentation;
  item.metadata = {
    'dc.title': [
      {
        value: `${valuePrefix}dc.title`
      }
    ] as MetadataValue[],
    'dc.contributor.author': [
      {
        value: `${valuePrefix}dc.contributor.author`
      }
    ] as MetadataValue[]
  };

  for (const metadataField of Object.keys(item.metadata)) {
    describe(`when creating an ItemMetadataRepresentation`, () => {
      beforeEach(() => {
        item.metadata['dspace.entity.type'] = [
          Object.assign(new MetadataValue(), {
            value: itemType
          })
        ];
        itemMetadataRepresentation = Object.assign(new ItemMetadataRepresentation(item.metadata[metadataField][0]), item);
      });

      it('should have a representation type of item', () => {
        expect(itemMetadataRepresentation.representationType).toEqual(MetadataRepresentationType.Item);
      });

      it('should return the correct value when calling getValue', () => {
        expect(itemMetadataRepresentation.getValue()).toEqual(`${valuePrefix}${metadataField}`);
      });

      it('should return the correct item type', () => {
        expect(itemMetadataRepresentation.itemType).toEqual(item.firstMetadataValue('dspace.entity.type'));
      });
    });
  }
});
