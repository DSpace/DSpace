import { ContentSourceSetSerializer } from './content-source-set-serializer';

describe('ContentSourceSetSerializer', () => {
  let serializer: ContentSourceSetSerializer;

  beforeEach(() => {
    serializer = new ContentSourceSetSerializer();
  });

  describe('Serialize', () => {
    it('should return all when the value is empty', () => {
      expect(serializer.Serialize('')).toEqual('all');
    });
    it('should return the value when it is not empty', () => {
      expect(serializer.Serialize('test-value')).toEqual('test-value');
    });
  });
  describe('Deserialize', () => {
    it('should return an empty value when the value is \'all\'', () => {
      expect(serializer.Deserialize('all')).toEqual('');
    });
    it('should return the value when it is not \'all\'', () => {
      expect(serializer.Deserialize('test-value')).toEqual('test-value');
    });
  });
});
