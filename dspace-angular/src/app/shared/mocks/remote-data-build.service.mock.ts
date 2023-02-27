import { Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { RemoteDataBuildService } from '../../core/cache/builders/remote-data-build.service';
import { PaginatedList, buildPaginatedList } from '../../core/data/paginated-list.model';
import { RemoteData } from '../../core/data/remote-data';
import { PageInfo } from '../../core/shared/page-info.model';
import { hasValue } from '../empty.util';
import { createSuccessfulRemoteDataObject$ } from '../remote-data.utils';
import { RequestEntry } from '../../core/data/request-entry.model';

export function getMockRemoteDataBuildService(toRemoteDataObservable$?: Observable<RemoteData<any>>, buildList$?: Observable<RemoteData<PaginatedList<any>>>): RemoteDataBuildService {
  return {
    toRemoteDataObservable: (requestEntry$: Observable<RequestEntry>, payload$: Observable<any>) => {

      if (hasValue(toRemoteDataObservable$)) {
        return toRemoteDataObservable$;
      } else {
        return payload$.pipe(map((payload) => ({
          payload
        } as RemoteData<any>)));
      }
    },
    buildSingle: (href$: string | Observable<string>) => createSuccessfulRemoteDataObject$({}),
    buildList: (href$: string | Observable<string>) => {
      if (hasValue(buildList$)) {
        return buildList$;
      } else {
        return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), []));
      }
    },
    buildFromRequestUUID: (id: string) => createSuccessfulRemoteDataObject$({}),
    buildFromRequestUUIDAndAwait: (id: string, callback: (rd?: RemoteData<any>) => Observable<any>) => createSuccessfulRemoteDataObject$({}),
    buildFromHref: (href: string) => createSuccessfulRemoteDataObject$({})
  } as RemoteDataBuildService;

}

export function getMockRemoteDataBuildServiceHrefMap(toRemoteDataObservable$?: Observable<RemoteData<any>>, buildListHrefMap$?: { [href: string]: Observable<RemoteData<PaginatedList<any>>>; }): RemoteDataBuildService {
  return {
    toRemoteDataObservable: (requestEntry$: Observable<RequestEntry>, payload$: Observable<any>) => {

      if (hasValue(toRemoteDataObservable$)) {
        return toRemoteDataObservable$;
      } else {
        return payload$.pipe(map((payload) => ({
          payload
        } as RemoteData<any>)));
      }
    },
    buildSingle: (href$: string | Observable<string>) => createSuccessfulRemoteDataObject$({}),
    buildList: (href$: string | Observable<string>) => {
      if (typeof href$ === 'string') {
        if (hasValue(buildListHrefMap$[href$])) {
          return buildListHrefMap$[href$];
        } else {
          return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), []));
        }
      }
      return href$.pipe(
        switchMap((href: string) => {
          if (hasValue(buildListHrefMap$[href])) {
            return buildListHrefMap$[href];
          } else {
            return createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), []));
          }
        })
      );
    },
    buildFromRequestUUID: (id: string) => createSuccessfulRemoteDataObject$({}),
    buildFromRequestUUIDAndAwait: (id: string, callback: (rd?: RemoteData<any>) => Observable<any>) => createSuccessfulRemoteDataObject$({}),
    buildFromHref: (href: string) => createSuccessfulRemoteDataObject$({})
  } as RemoteDataBuildService;

}
