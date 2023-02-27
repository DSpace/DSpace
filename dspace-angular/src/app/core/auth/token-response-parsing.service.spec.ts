import { TokenResponseParsingService } from './token-response-parsing.service';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { TokenResponse } from '../cache/response.models';

describe('TokenResponseParsingService', () => {
  let service: TokenResponseParsingService;

  beforeEach(() => {
    service = new TokenResponseParsingService();
  });

  describe('parse', () => {
    it('should return a TokenResponse containing the token', () => {
      const data = {
        payload: {
          token: 'valid-token'
        },
        statusCode: 200,
        statusText: 'OK'
      } as RawRestResponse;
      const expected = new TokenResponse(data.payload.token, true, 200, 'OK');
      expect(service.parse(undefined, data)).toEqual(expected);
    });

    it('should return an empty TokenResponse when payload doesn\'t contain a token', () => {
      const data = {
        payload: {},
        statusCode: 200,
        statusText: 'OK'
      } as RawRestResponse;
      const expected = new TokenResponse(null, false, 200, 'OK');
      expect(service.parse(undefined, data)).toEqual(expected);
    });

    it('should return an error TokenResponse when the response failed', () => {
      const data = {
        payload: {},
        statusCode: 400,
        statusText: 'BAD REQUEST'
      } as RawRestResponse;
      const expected = new TokenResponse(null, false, 400, 'BAD REQUEST');
      expect(service.parse(undefined, data)).toEqual(expected);
    });
  });
});
