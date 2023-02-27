import { Registration } from '../shared/registration.model';
import { RegistrationResponseParsingService } from './registration-response-parsing.service';
import { ParsedResponse } from '../cache/response.models';

describe('RegistrationResponseParsingService', () => {
  describe('parse', () => {
    const registration = Object.assign(new Registration(), { email: 'test@email.org', token: 'test-token' });
    const registrationResponseParsingService = new RegistrationResponseParsingService();

    const data = {
      payload: { email: 'test@email.org', token: 'test-token' },
      statusCode: 200,
      statusText: 'Success'
    };

    it('should parse a registration response', () => {
      const expected = registrationResponseParsingService.parse({} as any, data);

      expect(expected).toEqual(new ParsedResponse(200, undefined, registration));
    });
  });
});
