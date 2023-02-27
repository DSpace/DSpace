import { ProcessBreadcrumbResolver } from './process-breadcrumb.resolver';
import { Process } from './processes/process.model';
import { ProcessDataService } from '../core/data/processes/process-data.service';
import { createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';

describe('ProcessBreadcrumbResolver', () => {
  describe('resolve', () => {
    let resolver: ProcessBreadcrumbResolver;
    let processDataService: ProcessDataService;
    let processBreadcrumbService: any;
    let process: Process;
    let id: string;
    let path: string;
    beforeEach(() => {
      id = '12345';
      process = Object.assign(new Process(), { id });
      path = 'rest.com/path/to/breadcrumb/12345';
      processBreadcrumbService = {};
      processDataService = {
        findById: () => createSuccessfulRemoteDataObject$(process)
      } as any;
      resolver = new ProcessBreadcrumbResolver(processBreadcrumbService, processDataService);
    });

    it('should resolve the breadcrumb config', (done) => {
      const resolvedConfig = resolver.resolve({ data: { breadcrumbKey: process }, params: { id: id} } as any, {url: path} as any);
      const expectedConfig = { provider: processBreadcrumbService, key: process, url: path};
      resolvedConfig.subscribe((config) => {
        expect(config).toEqual(expectedConfig);
        done();
      });
    });

    it('should resolve throw an error when no breadcrumbKey is defined', () => {
      expect(() => {
        resolver.resolve({ data: {} } as any, undefined);
      }).toThrow();
    });
  });
});
