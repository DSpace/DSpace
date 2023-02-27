import { I18nBreadcrumbResolver } from './i18n-breadcrumb.resolver';
import { URLCombiner } from '../url-combiner/url-combiner';

describe('I18nBreadcrumbResolver', () => {
  describe('resolve', () => {
    let resolver: I18nBreadcrumbResolver;
    let i18nBreadcrumbService: any;
    let i18nKey: string;
    let route: any;
    let parentSegment;
    let segment;
    let expectedPath;
    beforeEach(() => {
      i18nKey = 'example.key';
      parentSegment = 'path';
      segment = 'breadcrumb';
      route = {
        data: { breadcrumbKey: i18nKey },
        routeConfig: {
          path: segment
        },
        parent: {
          routeConfig: {
            path: parentSegment
          }
        } as any
      };
      expectedPath = new URLCombiner(parentSegment, segment).toString();
      i18nBreadcrumbService = {};
      resolver = new I18nBreadcrumbResolver(i18nBreadcrumbService);
    });

    it('should resolve the breadcrumb config', () => {
      const resolvedConfig = resolver.resolve(route, {} as any);
      const expectedConfig = { provider: i18nBreadcrumbService, key: i18nKey, url: expectedPath };
      expect(resolvedConfig).toEqual(expectedConfig);
    });

    it('should resolve throw an error when no breadcrumbKey is defined', () => {
      expect(() => {
        resolver.resolve({ data: {} } as any, undefined);
      }).toThrow();
    });
  });
});
