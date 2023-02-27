import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, switchMap, mergeMap } from 'rxjs/operators';
import { FollowLinkConfig, followLink } from '../../../shared/utils/follow-link-config.model';
import { RequestService } from '../../data/request.service';
import { RemoteData } from '../../data/remote-data';
import { PaginatedList } from '../../data/paginated-list.model';
import { Vocabulary } from './models/vocabulary.model';
import { VocabularyEntry } from './models/vocabulary-entry.model';
import { isNotEmpty } from '../../../shared/empty.util';
import {
  getFirstSucceededRemoteDataPayload,
  getFirstSucceededRemoteListPayload,
} from '../../shared/operators';
import { VocabularyFindOptions } from './models/vocabulary-find-options.model';
import { VocabularyEntryDetail } from './models/vocabulary-entry-detail.model';
import { RequestParam } from '../../cache/models/request-param.model';
import { VocabularyOptions } from './models/vocabulary-options.model';
import { PageInfo } from '../../shared/page-info.model';
import { FindListOptions } from '../../data/find-list-options.model';
import { VocabularyEntryDetailsDataService } from './vocabulary-entry-details.data.service';
import { VocabularyDataService } from './vocabulary.data.service';

/**
 * A service responsible for fetching/sending data from/to the REST API on the vocabularies endpoint
 */
@Injectable()
export class VocabularyService {
  protected searchTopMethod = 'top';

  constructor(
    protected requestService: RequestService,
    protected vocabularyDataService: VocabularyDataService,
    protected vocabularyEntryDetailDataService: VocabularyEntryDetailsDataService,
  ) {
  }

  /**
   * Returns an observable of {@link RemoteData} of a {@link Vocabulary}, based on an href, with a list of {@link FollowLinkConfig},
   * to automatically resolve {@link HALLink}s of the {@link Vocabulary}
   * @param href                        The url of object we want to retrieve
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<Vocabulary>>}
   *    Return an observable that emits vocabulary object
   */
  findVocabularyByHref(href: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Vocabulary>[]): Observable<RemoteData<any>> {
    return this.vocabularyDataService.findByHref(href, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Returns an observable of {@link RemoteData} of a {@link Vocabulary}, based on its ID, with a list of {@link FollowLinkConfig},
   * to automatically resolve {@link HALLink}s of the object
   * @param name              The name of {@link Vocabulary} we want to retrieve
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<Vocabulary>>}
   *    Return an observable that emits vocabulary object
   */
  findVocabularyById(name: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Vocabulary>[]): Observable<RemoteData<Vocabulary>> {
    return this.vocabularyDataService.findById(name, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Returns {@link RemoteData} of all object with a list of {@link FollowLinkConfig}, to indicate which embedded
   * info should be added to the objects
   *
   * @param options           Find list options object
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<PaginatedList<Vocabulary>>>}
   *    Return an observable that emits object list
   */
  findAllVocabularies(options: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Vocabulary>[]): Observable<RemoteData<PaginatedList<Vocabulary>>> {
    return this.vocabularyDataService.findAll(options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Return the {@link VocabularyEntry} list for a given {@link Vocabulary}
   *
   * @param vocabularyOptions  The {@link VocabularyOptions} for the request to which the entries belong
   * @param pageInfo           The {@link PageInfo} for the request
   * @return {Observable<RemoteData<PaginatedList<VocabularyEntry>>>}
   *    Return an observable that emits object list
   */
  getVocabularyEntries(vocabularyOptions: VocabularyOptions, pageInfo: PageInfo): Observable<RemoteData<PaginatedList<VocabularyEntry>>> {

    const options: VocabularyFindOptions = new VocabularyFindOptions(
      null,
      null,
      null,
      null,
      pageInfo.elementsPerPage,
      pageInfo.currentPage
    );

    // TODO remove false for the entries embed when https://github.com/DSpace/DSpace/issues/3096 is solved
    return this.findVocabularyById(vocabularyOptions.name, true, true, followLink('entries', { findListOptions: options, shouldEmbed: false })).pipe(
      getFirstSucceededRemoteDataPayload(),
      switchMap((vocabulary: Vocabulary) => vocabulary.entries),
    );
  }

  /**
   * Return the {@link VocabularyEntry} list for a given value
   *
   * @param value              The entry value to retrieve
   * @param exact              If true force the vocabulary to provide only entries that match exactly with the value
   * @param vocabularyOptions  The {@link VocabularyOptions} for the request to which the entries belong
   * @param pageInfo           The {@link PageInfo} for the request
   * @return {Observable<RemoteData<PaginatedList<VocabularyEntry>>>}
   *    Return an observable that emits object list
   */
  getVocabularyEntriesByValue(value: string, exact: boolean, vocabularyOptions: VocabularyOptions, pageInfo: PageInfo): Observable<RemoteData<PaginatedList<VocabularyEntry>>> {
    const options: VocabularyFindOptions = new VocabularyFindOptions(
      null,
      value,
      exact,
      null,
      pageInfo.elementsPerPage,
      pageInfo.currentPage
    );

    // TODO remove false for the entries embed when https://github.com/DSpace/DSpace/issues/3096 is solved
    return this.findVocabularyById(vocabularyOptions.name, true, true, followLink('entries', { findListOptions: options, shouldEmbed: false })).pipe(
      getFirstSucceededRemoteDataPayload(),
      switchMap((vocabulary: Vocabulary) => vocabulary.entries),
    );

  }

  /**
   * Return the {@link VocabularyEntry} list for a given value
   *
   * @param value              The entry value to retrieve
   * @param vocabularyOptions  The {@link VocabularyOptions} for the request to which the entry belongs
   * @return {Observable<RemoteData<PaginatedList<VocabularyEntry>>>}
   *    Return an observable that emits {@link VocabularyEntry} object
   */
  getVocabularyEntryByValue(value: string, vocabularyOptions: VocabularyOptions): Observable<VocabularyEntry> {

    return this.getVocabularyEntriesByValue(value, true, vocabularyOptions, new PageInfo()).pipe(
      getFirstSucceededRemoteListPayload(),
      map((list: VocabularyEntry[]) => {
        if (isNotEmpty(list)) {
          return list[0];
        } else {
          return null;
        }
      })
    );
  }

  /**
   * Return the {@link VocabularyEntry} list for a given ID
   *
   * @param ID                 The entry ID to retrieve
   * @param vocabularyOptions  The {@link VocabularyOptions} for the request to which the entry belongs
   * @return {Observable<RemoteData<PaginatedList<VocabularyEntry>>>}
   *    Return an observable that emits {@link VocabularyEntry} object
   */
  getVocabularyEntryByID(ID: string, vocabularyOptions: VocabularyOptions): Observable<VocabularyEntry> {
    const pageInfo = new PageInfo();
    const options: VocabularyFindOptions = new VocabularyFindOptions(
      null,
      null,
      null,
      ID,
      pageInfo.elementsPerPage,
      pageInfo.currentPage
    );

    // TODO remove false for the entries embed when https://github.com/DSpace/DSpace/issues/3096 is solved
    return this.findVocabularyById(vocabularyOptions.name, true, true, followLink('entries', { findListOptions: options, shouldEmbed: false })).pipe(
      getFirstSucceededRemoteDataPayload(),
      switchMap((vocabulary: Vocabulary) => vocabulary.entries),
      getFirstSucceededRemoteListPayload(),
      map((list: VocabularyEntry[]) => {
        if (isNotEmpty(list)) {
          return list[0];
        } else {
          return null;
        }
      })
    );
  }

  /**
   * Returns an observable of {@link RemoteData} of a {@link VocabularyEntryDetail}, based on an href, with a list of {@link FollowLinkConfig},
   * to automatically resolve {@link HALLink}s of the {@link VocabularyEntryDetail}
   * @param href                        The url of object we want to retrieve
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<VocabularyEntryDetail>>}
   *    Return an observable that emits vocabulary object
   */
  findEntryDetailByHref(href: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<VocabularyEntryDetail>[]): Observable<RemoteData<VocabularyEntryDetail>> {
    return this.vocabularyEntryDetailDataService.findByHref(href, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Returns an observable of {@link RemoteData} of a {@link VocabularyEntryDetail}, based on its ID, with a list of {@link FollowLinkConfig},
   * to automatically resolve {@link HALLink}s of the object
   * @param id                          The entry id for which to provide detailed information.
   * @param name                        The name of {@link Vocabulary} to which the entry belongs
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<VocabularyEntryDetail>>}
   *    Return an observable that emits VocabularyEntryDetail object
   */
  findEntryDetailById(id: string, name: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<VocabularyEntryDetail>[]): Observable<RemoteData<VocabularyEntryDetail>> {
    const findId = `${name}:${id}`;
    return this.vocabularyEntryDetailDataService.findById(findId, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Returns the parent detail entry for a given detail entry, with a list of {@link FollowLinkConfig},
   * to automatically resolve {@link HALLink}s of the object
   * @param value                       The entry value for which to provide parent.
   * @param name                        The name of {@link Vocabulary} to which the entry belongs
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<PaginatedList<VocabularyEntryDetail>>>}
   *    Return an observable that emits a PaginatedList of VocabularyEntryDetail
   */
  getEntryDetailParent(value: string, name: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<VocabularyEntryDetail>[]): Observable<RemoteData<VocabularyEntryDetail>> {
    const linkPath = `${name}:${value}/parent`;

    return this.vocabularyEntryDetailDataService.getBrowseEndpoint().pipe(
      map((href: string) => `${href}/${linkPath}`),
      mergeMap((href) => this.vocabularyEntryDetailDataService.findByHref(href, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow))
    );
  }

  /**
   * Returns the list of children detail entries for a given detail entry, with a list of {@link FollowLinkConfig},
   * to automatically resolve {@link HALLink}s of the object
   * @param value                       The entry value for which to provide children list.
   * @param name                        The name of {@link Vocabulary} to which the entry belongs
   * @param pageInfo                    The {@link PageInfo} for the request
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<PaginatedList<VocabularyEntryDetail>>>}
   *    Return an observable that emits a PaginatedList of VocabularyEntryDetail
   */
  getEntryDetailChildren(value: string, name: string, pageInfo: PageInfo, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<VocabularyEntryDetail>[]): Observable<RemoteData<PaginatedList<VocabularyEntryDetail>>> {
    const options: VocabularyFindOptions = new VocabularyFindOptions(
      null,
      null,
      null,
      null,
      pageInfo.elementsPerPage,
      pageInfo.currentPage
    );

    return this.vocabularyEntryDetailDataService.getBrowseEndpoint().pipe(
      map(href => `${href}/${name}:${value}/children`),
      switchMap(href => this.vocabularyEntryDetailDataService.findListByHref(href, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow))
    );
  }

  /**
   * Return the top level {@link VocabularyEntryDetail} list for a given hierarchical vocabulary
   *
   * @param name                        The name of hierarchical {@link Vocabulary} to which the
   *                                    entries belongs
   * @param pageInfo                    The {@link PageInfo} for the request
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  searchTopEntries(name: string, pageInfo: PageInfo, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<VocabularyEntryDetail>[]): Observable<RemoteData<PaginatedList<VocabularyEntryDetail>>> {
    const options: VocabularyFindOptions = new VocabularyFindOptions(
      null,
      null,
      null,
      null,
      pageInfo.elementsPerPage,
      pageInfo.currentPage
    );
    options.searchParams = [new RequestParam('vocabulary', name)];
    return this.vocabularyEntryDetailDataService.searchBy(this.searchTopMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Clear all search Top Requests
   */
  clearSearchTopRequests(): void {
    this.requestService.removeByHrefSubstring(`search/${this.searchTopMethod}`);
  }
}

