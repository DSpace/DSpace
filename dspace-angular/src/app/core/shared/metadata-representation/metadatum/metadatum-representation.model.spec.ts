import { MetadatumRepresentation } from './metadatum-representation.model';
import { MetadataRepresentationType } from '../metadata-representation.model';
import { MetadataValue } from '../../metadata.models';

describe('MetadatumRepresentation', () => {
  const itemType = 'Person';
  const normalMetadatum = Object.assign(new MetadataValue(), {
    key: 'dc.contributor.author',
    value: 'Test Author'
  });
  const authorityMetadatum = Object.assign(new MetadataValue(), {
    key: 'dc.contributor.author',
    value: 'Test Authority Author',
    authority: '1234'
  });

  let metadatumRepresentation: MetadatumRepresentation;

  describe('when creating a MetadatumRepresentation based on a standard Metadatum object', () => {
    beforeEach(() => {
      metadatumRepresentation = Object.assign(new MetadatumRepresentation(itemType), normalMetadatum);
    });

    it('should have a representation type of plain text', () => {
      expect(metadatumRepresentation.representationType).toEqual(MetadataRepresentationType.PlainText);
    });

    it('should return the correct value when calling getPrimaryValue', () => {
      expect(metadatumRepresentation.getValue()).toEqual(normalMetadatum.value);
    });

    it('should return the correct item type', () => {
      expect(metadatumRepresentation.itemType).toEqual(itemType);
    });
  });

  describe('when creating a MetadatumRepresentation based on an authority controlled Metadatum object', () => {
    beforeEach(() => {
      metadatumRepresentation = Object.assign(new MetadatumRepresentation(itemType), authorityMetadatum);
    });

    it('should have a representation type of plain text', () => {
      expect(metadatumRepresentation.representationType).toEqual(MetadataRepresentationType.AuthorityControlled);
    });

    it('should return the correct value when calling getValue', () => {
      expect(metadatumRepresentation.getValue()).toEqual(authorityMetadatum.value);
    });

    it('should return the correct item type', () => {
      expect(metadatumRepresentation.itemType).toEqual(itemType);
    });
  });
});
