import { StatusCodeOnlyResponseParsingService } from './status-code-only-response-parsing.service';

describe('StatusCodeOnlyResponseParsingService', () => {
  let service;
  let statusCode;
  let statusText;

  beforeEach(() => {
    service = new StatusCodeOnlyResponseParsingService();
  });

  describe('parse', () => {

    it('should return a RestResponse that doesn\'t contain the response body', () => {
      const payload = 'd9128e44-183b-479d-aa2e-d39435838bf6';
      const result = service.parse(undefined, {
        payload,
        statusCode: 201,
        statusText: '201'
      });

      expect(JSON.stringify(result).indexOf(payload)).toBe(-1);
    });

    describe('when the response is successful', () => {
      beforeEach(() => {
        statusCode = 201;
        statusText = `${statusCode}`;
      });

      it('should return a success RestResponse', () => {
        const result = service.parse(undefined, {
          statusCode,
          statusText
        });

        expect(result.isSuccessful).toBe(true);
      });

      it('should return a RestResponse with the correct status code', () => {
        const result = service.parse(undefined, {
          statusCode,
          statusText
        });

        expect(result.statusCode).toBe(statusCode);
      });

      it('should return a RestResponse with the correct status text', () => {
        const result = service.parse(undefined, {
          statusCode,
          statusText
        });

        expect(result.statusText).toBe(statusText);
      });
    });

    describe('when the response is unsuccessful', () => {
      beforeEach(() => {
        statusCode = 400;
        statusText = `${statusCode}`;
      });

      it('should return an error RestResponse', () => {
        const result = service.parse(undefined, {
          statusCode,
          statusText
        });

        expect(result.isSuccessful).toBe(false);
      });

      it('should return a RestResponse with the correct status code', () => {
        const result = service.parse(undefined, {
          statusCode,
          statusText
        });

        expect(result.statusCode).toBe(statusCode);
      });

      it('should return a RestResponse with the correct status text', () => {
        const result = service.parse(undefined, {
          statusCode,
          statusText
        });

        expect(result.statusText).toBe(statusText);
      });
    });
  });
});
