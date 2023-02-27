import { LegacyBitstreamUrlResolver } from './legacy-bitstream-url.resolver';
import { EMPTY } from 'rxjs';
import { BitstreamDataService } from '../core/data/bitstream-data.service';
import { RemoteData } from '../core/data/remote-data';
import { TestScheduler } from 'rxjs/testing';
import { RequestEntryState } from '../core/data/request-entry-state.model';

describe(`LegacyBitstreamUrlResolver`, () => {
  let resolver: LegacyBitstreamUrlResolver;
  let bitstreamDataService: BitstreamDataService;
  let testScheduler;
  let remoteDataMocks;
  let route;
  let state;

  beforeEach(() => {
    testScheduler = new TestScheduler((actual, expected) => {
      expect(actual).toEqual(expected);
    });

    route = {
      params: {},
      queryParams: {}
    };
    state = {};
    remoteDataMocks = {
      RequestPending: new RemoteData(undefined, 0, 0, RequestEntryState.RequestPending, undefined, undefined, undefined),
      ResponsePending: new RemoteData(undefined, 0, 0, RequestEntryState.ResponsePending, undefined, undefined, undefined),
      Success: new RemoteData(0, 0, 0, RequestEntryState.Success, undefined, {}, 200),
      Error: new RemoteData(0, 0, 0, RequestEntryState.Error, 'Internal server error', undefined, 500),
    };
    bitstreamDataService = {
      findByItemHandle: () => undefined
    } as any;
    resolver = new LegacyBitstreamUrlResolver(bitstreamDataService);
  });

  describe(`resolve`, () => {
    describe(`For JSPUI-style URLs`, () => {
      beforeEach(() => {
        spyOn(bitstreamDataService, 'findByItemHandle').and.returnValue(EMPTY);
        route = Object.assign({}, route, {
          params: {
            prefix: '123456789',
            suffix: '1234',
            filename: 'some-file.pdf',
            sequence_id: '5'
          }
        });
      });
      it(`should call findByItemHandle with the handle, sequence id, and filename from the route`, () => {
        testScheduler.run(() => {
          resolver.resolve(route, state);
          expect(bitstreamDataService.findByItemHandle).toHaveBeenCalledWith(
            `${route.params.prefix}/${route.params.suffix}`,
            route.params.sequence_id,
            route.params.filename
          );
        });
      });
    });

    describe(`For XMLUI-style URLs`, () => {
      describe(`when there is a sequenceId query parameter`, () => {
        beforeEach(() => {
          spyOn(bitstreamDataService, 'findByItemHandle').and.returnValue(EMPTY);
          route = Object.assign({}, route, {
            params: {
              prefix: '123456789',
              suffix: '1234',
              filename: 'some-file.pdf',
            },
            queryParams: {
              sequenceId: '5'
            }
          });
        });
        it(`should call findByItemHandle with the handle and filename from the route, and the sequence ID from the queryParams`, () => {
          testScheduler.run(() => {
            resolver.resolve(route, state);
            expect(bitstreamDataService.findByItemHandle).toHaveBeenCalledWith(
              `${route.params.prefix}/${route.params.suffix}`,
              route.queryParams.sequenceId,
              route.params.filename
            );
          });
        });
      });
      describe(`when there's no sequenceId query parameter`, () => {
        beforeEach(() => {
          spyOn(bitstreamDataService, 'findByItemHandle').and.returnValue(EMPTY);
          route = Object.assign({}, route, {
            params: {
              prefix: '123456789',
              suffix: '1234',
              filename: 'some-file.pdf',
            },
          });
        });
        it(`should call findByItemHandle with the handle, and filename from the route`, () => {
          testScheduler.run(() => {
            resolver.resolve(route, state);
            expect(bitstreamDataService.findByItemHandle).toHaveBeenCalledWith(
              `${route.params.prefix}/${route.params.suffix}`,
              undefined,
              route.params.filename
            );
          });
        });
      });
    });
    describe(`should return and complete after the remotedata has...`, () => {
      it(`...failed`, () => {
        testScheduler.run(({ cold, expectObservable }) => {
          spyOn(bitstreamDataService, 'findByItemHandle').and.returnValue(cold('a-b-c', {
            a: remoteDataMocks.RequestPending,
            b: remoteDataMocks.ResponsePending,
            c: remoteDataMocks.Error,
          }));
          const expected = '----(c|)';
          const values = {
            c: remoteDataMocks.Error,
          };

          expectObservable(resolver.resolve(route, state)).toBe(expected, values);
        });
      });
      it(`...succeeded`, () => {
        testScheduler.run(({ cold, expectObservable }) => {
          spyOn(bitstreamDataService, 'findByItemHandle').and.returnValue(cold('a-b-c', {
            a: remoteDataMocks.RequestPending,
            b: remoteDataMocks.ResponsePending,
            c: remoteDataMocks.Success,
          }));
          const expected = '----(c|)';
          const values = {
            c: remoteDataMocks.Success,
          };

          expectObservable(resolver.resolve(route, state)).toBe(expected, values);
        });
      });
    });
  });
});
