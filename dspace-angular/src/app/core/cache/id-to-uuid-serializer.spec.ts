import { IDToUUIDSerializer } from './id-to-uuid-serializer';

describe('IDToUUIDSerializer', () => {
  let serializer: IDToUUIDSerializer;
  const prefix = 'test-prefix';

  beforeEach(() => {
    serializer = new IDToUUIDSerializer(prefix);
  });

  describe('Serialize', () => {
    it('should return undefined', () => {
      expect(serializer.Serialize('some-uuid')).toBeUndefined();
    });
  });

  describe('Deserialize', () => {
    describe('when ID is defined', () => {
      it('should prepend the prefix to the ID', () => {
        const id = 'some-id';
        expect(serializer.Deserialize(id)).toBe(`${prefix}-${id}`);
      });
    });

    describe('when ID is null or undefined', () => {
      it('should return null or undefined', () => {
        expect(serializer.Deserialize(null)).toBeNull();
        expect(serializer.Deserialize(undefined)).toBeUndefined();
      });
    });

  });

});
