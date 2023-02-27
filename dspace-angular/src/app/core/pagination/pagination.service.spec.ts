import { PaginationService } from './pagination.service';
import { RouterStub } from '../../shared/testing/router.stub';
import { of as observableOf } from 'rxjs';
import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import { SortDirection, SortOptions } from '../cache/models/sort-options.model';
import { FindListOptions } from '../data/find-list-options.model';


describe('PaginationService', () => {
  let service: PaginationService;
  let router;
  let routeService;

  const defaultPagination = new PaginationComponentOptions();
  const defaultSort = new SortOptions('dc.title', SortDirection.ASC);
  const defaultFindListOptions = new FindListOptions();

  beforeEach(() => {
    router = new RouterStub();
    routeService = {
      getQueryParameterValue: (param) => {
        let value;
        if (param.endsWith('.page')) {
          value = 5;
        }
        if (param.endsWith('.rpp')) {
          value = 10;
        }
        if (param.endsWith('.sd')) {
          value = 'ASC';
        }
        if (param.endsWith('.sf')) {
          value = 'score';
        }
        return observableOf(value);
      }
    };

    service = new PaginationService(routeService, router);
  });

  describe('getCurrentPagination', () => {
    it('should retrieve the current pagination info from the routerService', () => {
      service.getCurrentPagination('test-id', defaultPagination).subscribe((currentPagination) => {
        expect(currentPagination).toEqual(Object.assign(new PaginationComponentOptions(), {
          currentPage: 5,
          pageSize: 10
        }));
      });
    });
  });
  describe('getCurrentSort', () => {
    it('should retrieve the current sort info from the routerService', () => {
      service.getCurrentSort('test-id', defaultSort).subscribe((currentSort) => {
        expect(currentSort).toEqual(Object.assign(new SortOptions('score', SortDirection.ASC )));
      });
    });
    it('should return default sort when no sort specified', () => {
      // This is same as routeService (defined above), but returns no sort field or direction
      routeService = {
        getQueryParameterValue: (param) => {
          let value;
          if (param.endsWith('.page')) {
            value = 5;
          }
          if (param.endsWith('.rpp')) {
            value = 10;
          }
          return observableOf(value);
        }
      };
      service = new PaginationService(routeService, router);

      service.getCurrentSort('test-id', defaultSort).subscribe((currentSort) => {
        expect(currentSort).toEqual(defaultSort);
      });
    });
  });
  describe('getFindListOptions', () => {
    it('should retrieve the current findListOptions info from the routerService', () => {
      service.getFindListOptions('test-id', defaultFindListOptions).subscribe((findListOptions) => {
        expect(findListOptions).toEqual(Object.assign(new FindListOptions(),
          {
            sort: new SortOptions('score', SortDirection.ASC ),
            currentPage: 5,
            elementsPerPage: 10
          }));
      });
    });
  });
  describe('resetPage', () => {
    it('should call the updateRoute method with the id and page 1', () => {
      spyOn(service, 'updateRoute');
      service.resetPage('test');

      expect(service.updateRoute).toHaveBeenCalledWith('test', {page: 1});
    });
  });

  describe('updateRoute', () => {
    it('should update the route with the provided page params', () => {
      service.updateRoute('test', {page: 2, pageSize: 5, sortField: 'title', sortDirection: SortDirection.DESC});

      const navigateParams = {};
      navigateParams[`test.page`] = `2`;
      navigateParams[`test.rpp`] = `5`;
      navigateParams[`test.sf`] = `title`;
      navigateParams[`test.sd`] = `DESC`;

      expect(router.navigate).toHaveBeenCalledWith([], {queryParams: navigateParams, queryParamsHandling: 'merge'});
    });
    it('should update the route with the provided page params while keeping the existing non provided ones', () => {
      service.updateRoute('test', {page: 2});

      const navigateParams = {};
      navigateParams[`test.page`] = `2`;
      navigateParams[`test.rpp`] = `10`;
      navigateParams[`test.sf`] = `score`;
      navigateParams[`test.sd`] = `ASC`;

      expect(router.navigate).toHaveBeenCalledWith([], {queryParams: navigateParams, queryParamsHandling: 'merge'});
    });
    it('should pass on navigationExtras to router.navigate', () => {
      service.updateRoute('test', {page: 2}, undefined, undefined, { queryParamsHandling: 'preserve', replaceUrl: true, preserveFragment: true });

      const navigateParams = {};
      navigateParams[`test.page`] = `2`;
      navigateParams[`test.rpp`] = `10`;
      navigateParams[`test.sf`] = `score`;
      navigateParams[`test.sd`] = `ASC`;

      expect(router.navigate).toHaveBeenCalledWith([], {queryParams: navigateParams, queryParamsHandling: 'preserve', replaceUrl: true, preserveFragment: true });
    });
  });
  describe('updateRouteWithUrl', () => {
    it('should update the route with the provided page params and url', () => {
      service.updateRouteWithUrl('test', ['someUrl'], {page: 2, pageSize: 5, sortField: 'title', sortDirection: SortDirection.DESC});

      const navigateParams = {};
      navigateParams[`test.page`] = `2`;
      navigateParams[`test.rpp`] = `5`;
      navigateParams[`test.sf`] = `title`;
      navigateParams[`test.sd`] = `DESC`;

      expect(router.navigate).toHaveBeenCalledWith(['someUrl'], {queryParams: navigateParams, queryParamsHandling: 'merge'});
    });
    it('should update the route with the provided page params and url while keeping the existing non provided ones', () => {
      service.updateRouteWithUrl('test',['someUrl'], {page: 2});

      const navigateParams = {};
      navigateParams[`test.page`] = `2`;
      navigateParams[`test.rpp`] = `10`;
      navigateParams[`test.sf`] = `score`;
      navigateParams[`test.sd`] = `ASC`;

      expect(router.navigate).toHaveBeenCalledWith(['someUrl'], {queryParams: navigateParams, queryParamsHandling: 'merge'});
    });
    it('should pass on navigationExtras to router.navigate', () => {
      service.updateRouteWithUrl('test',['someUrl'], {page: 2}, undefined, undefined, { queryParamsHandling: 'preserve', replaceUrl: true, preserveFragment: true });

      const navigateParams = {};
      navigateParams[`test.page`] = `2`;
      navigateParams[`test.rpp`] = `10`;
      navigateParams[`test.sf`] = `score`;
      navigateParams[`test.sd`] = `ASC`;

      expect(router.navigate).toHaveBeenCalledWith(['someUrl'], {queryParams: navigateParams, queryParamsHandling: 'preserve', replaceUrl: true, preserveFragment: true });
    });
  });
  describe('clearPagination', () => {
    it('should clear the pagination next time the updateRoute/updateRouteWithUrl method is called', () => {
      service.clearPagination('test');

      const resetParams = {};
      resetParams[`test.page`] = null;
      resetParams[`test.rpp`] = null;
      resetParams[`test.sf`] = null;
      resetParams[`test.sd`] = null;


      const navigateParams = {};
      navigateParams[`another-id.page`] = `5`;
      navigateParams[`another-id.rpp`] = `10`;
      navigateParams[`another-id.sf`] = `score`;
      navigateParams[`another-id.sd`] = `ASC`;

      service.updateRoute('another-id', {});

      expect(router.navigate).toHaveBeenCalledWith([], {queryParams: Object.assign({}, resetParams, navigateParams), queryParamsHandling: 'merge'});
    });
  });
  describe('getPageParam', () => {
    it('should return the name of the page param', () => {
      const pageParam = service.getPageParam('test');
      expect(pageParam).toEqual('test.page');
    });
  });
});
