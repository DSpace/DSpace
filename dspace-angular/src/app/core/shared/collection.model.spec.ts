import { Collection } from './collection.model';

describe('Collection', () => {

  describe('Collection handle value', () => {

    let metadataValue;

    beforeEach(() => {
      metadataValue = { 'dc.identifier.uri': [{ value: '123456789/1' }] };
    });

    it('should return the handle value from metadata', () => {
      const community = Object.assign(new Collection(), { metadata: metadataValue });
      expect(community.handle).toEqual('123456789/1');
    });

    it('should return undefined if the handle value from metadata is not present', () => {
      const community = Object.assign(new Collection(), {});
      expect(community.handle).toEqual(undefined);
    });
  });

});
