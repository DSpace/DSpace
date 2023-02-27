import { HandleService } from './handle.service';

describe('HandleService', () => {
  let service: HandleService;

  beforeEach(() => {
    service = new HandleService();
  });

  describe(`normalizeHandle`, () => {
    it(`should simply return an already normalized handle`, () => {
      let input, output;

      input = '123456789/123456';
      output = service.normalizeHandle(input);
      expect(output).toEqual(input);

      input = '12.3456.789/123456';
      output = service.normalizeHandle(input);
      expect(output).toEqual(input);
    });

    it(`should normalize a handle url`, () => {
      let input, output;

      input = 'https://hdl.handle.net/handle/123456789/123456';
      output = service.normalizeHandle(input);
      expect(output).toEqual('123456789/123456');

      input = 'https://rest.api/server/handle/123456789/123456';
      output = service.normalizeHandle(input);
      expect(output).toEqual('123456789/123456');
    });

    it(`should return null if the input doesn't contain a handle`, () => {
      let input, output;

      input = 'https://hdl.handle.net/handle/123456789';
      output = service.normalizeHandle(input);
      expect(output).toBeNull();

      input = 'something completely different';
      output = service.normalizeHandle(input);
      expect(output).toBeNull();
    });
  });
});
