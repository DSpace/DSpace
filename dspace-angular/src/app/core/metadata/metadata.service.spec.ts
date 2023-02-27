import { fakeAsync, tick } from '@angular/core/testing';
import { Meta, Title } from '@angular/platform-browser';
import { NavigationEnd, Router } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';
import { Observable, of as observableOf, of } from 'rxjs';

import { RemoteData } from '../data/remote-data';
import { Item } from '../shared/item.model';

import {
  ItemMock,
  MockBitstream1,
  MockBitstream3,
  MockBitstream2
} from '../../shared/mocks/item.mock';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { PaginatedList } from '../data/paginated-list.model';
import { Bitstream } from '../shared/bitstream.model';
import { MetadataValue } from '../shared/metadata.models';

import { MetadataService } from './metadata.service';
import { RootDataService } from '../data/root-data.service';
import { Bundle } from '../shared/bundle.model';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { getMockTranslateService } from '../../shared/mocks/translate.service.mock';
import { DSONameService } from '../breadcrumbs/dso-name.service';
import { HardRedirectService } from '../services/hard-redirect.service';
import { getMockStore } from '@ngrx/store/testing';
import { AddMetaTagAction, ClearMetaTagAction } from './meta-tag.actions';
import { AuthorizationDataService } from '../data/feature-authorization/authorization-data.service';
import { AppConfig } from '../../../config/app-config.interface';

describe('MetadataService', () => {
  let metadataService: MetadataService;

  let meta: Meta;

  let title: Title;

  let dsoNameService: DSONameService;

  let bundleDataService;
  let bitstreamDataService;
  let rootService: RootDataService;
  let translateService: TranslateService;
  let hardRedirectService: HardRedirectService;
  let authorizationService: AuthorizationDataService;

  let router: Router;
  let store;

  let appConfig: AppConfig;

  const initialState = { 'core': { metaTag: { tagsInUse: ['title', 'description'] }}};


  beforeEach(() => {
    rootService = jasmine.createSpyObj({
      findRoot: createSuccessfulRemoteDataObject$({ dspaceVersion: 'mock-dspace-version' })
    });
    bitstreamDataService = jasmine.createSpyObj({
      findListByHref: createSuccessfulRemoteDataObject$(createPaginatedList([MockBitstream3])),
    });
    bundleDataService = jasmine.createSpyObj({
      findByItemAndName: mockBundleRD$([MockBitstream3])
    });
    translateService = getMockTranslateService();
    meta = jasmine.createSpyObj('meta', {
      addTag: {},
      removeTag: {}
    });
    title = jasmine.createSpyObj({
      setTitle: {}
    });
    dsoNameService = jasmine.createSpyObj({
      getName: ItemMock.firstMetadataValue('dc.title')
    });
    router = {
      url: '/items/0ec7ff22-f211-40ab-a69e-c819b0b1f357',
      events: of(new NavigationEnd(1, '', '')),
      routerState: {
        root: {}
      }
    } as any as Router;
    hardRedirectService = jasmine.createSpyObj( {
      getCurrentOrigin: 'https://request.org',
    });
    authorizationService = jasmine.createSpyObj('authorizationService', {
      isAuthorized: observableOf(true)
    });

    // @ts-ignore
    store = getMockStore({ initialState });
    spyOn(store, 'dispatch');

    appConfig = {
      item: {
        bitstream: {
          pageSize: 5
        }
      }
    } as any;

    metadataService = new MetadataService(
      router,
      translateService,
      meta,
      title,
      dsoNameService,
      bundleDataService,
      bitstreamDataService,
      undefined,
      rootService,
      store,
      hardRedirectService,
      appConfig,
      authorizationService
    );
  });

  it('items page should set meta tags', fakeAsync(() => {
    (metadataService as any).processRouteChange({
      data: {
        value: {
          dso: createSuccessfulRemoteDataObject(ItemMock),
        }
      }
    });
    tick();
    expect(title.setTitle).toHaveBeenCalledWith('Test PowerPoint Document');
    expect(meta.addTag).toHaveBeenCalledWith({
      name: 'citation_title',
      content: 'Test PowerPoint Document'
    });
    expect(meta.addTag).toHaveBeenCalledWith({ name: 'citation_author', content: 'Doe, Jane' });
    expect(meta.addTag).toHaveBeenCalledWith({
      name: 'citation_publication_date',
      content: '1650-06-26'
    });
    expect(meta.addTag).toHaveBeenCalledWith({ name: 'citation_issn', content: '123456789' });
    expect(meta.addTag).toHaveBeenCalledWith({ name: 'citation_language', content: 'en' });
    expect(meta.addTag).toHaveBeenCalledWith({
      name: 'citation_keywords',
      content: 'keyword1; keyword2; keyword3'
    });
  }));

  it('items page should set meta tags as published Thesis', fakeAsync(() => {
    (metadataService as any).processRouteChange({
      data: {
        value: {
          dso: createSuccessfulRemoteDataObject(mockPublisher(mockType(ItemMock, 'Thesis'))),
        }
      }
    });
    tick();
    expect(meta.addTag).toHaveBeenCalledWith({
      name: 'citation_dissertation_name',
      content: 'Test PowerPoint Document'
    });
    expect(meta.addTag).toHaveBeenCalledWith({
      name: 'citation_pdf_url',
      content: 'https://request.org/bitstreams/4db100c1-e1f5-4055-9404-9bc3e2d15f29/download'
    });
  }));

  it('items page should set meta tags as published Technical Report', fakeAsync(() => {
    (metadataService as any).processRouteChange({
      data: {
        value: {
          dso: createSuccessfulRemoteDataObject(mockPublisher(mockType(ItemMock, 'Technical Report'))),
        }
      }
    });
    tick();
    expect(meta.addTag).toHaveBeenCalledWith({
      name: 'citation_technical_report_institution',
      content: 'Mock Publisher'
    });
  }));

  it('route titles should overwrite dso titles', fakeAsync(() => {
    (translateService.get as jasmine.Spy).and.returnValues(of('DSpace :: '), of('Translated Route Title'));
    (metadataService as any).processRouteChange({
      data: {
        value: {
          dso: createSuccessfulRemoteDataObject(ItemMock),
          title: 'route.title.key',
        }
      }
    });
    tick();
    expect(title.setTitle).toHaveBeenCalledTimes(2);
    expect((title.setTitle as jasmine.Spy).calls.argsFor(0)).toEqual(['Test PowerPoint Document']);
    expect((title.setTitle as jasmine.Spy).calls.argsFor(1)).toEqual(['DSpace :: Translated Route Title']);
  }));

  it('other navigation should add title and description', fakeAsync(() => {
    (translateService.get as jasmine.Spy).and.returnValues(of('DSpace :: '), of('Dummy Title'), of('This is a dummy item component for testing!'));
    (metadataService as any).processRouteChange({
      data: {
        value: {
          title: 'Dummy Title',
          description: 'This is a dummy item component for testing!'
        }
      }
    });
    tick();
    expect(title.setTitle).toHaveBeenCalledWith('DSpace :: Dummy Title');
    expect(meta.addTag).toHaveBeenCalledWith({
      name: 'title',
      content: 'DSpace :: Dummy Title'
    });
    expect(meta.addTag).toHaveBeenCalledWith({
      name: 'description',
      content: 'This is a dummy item component for testing!'
    });
  }));

  describe(`listenForRouteChange`, () => {
    it(`should call processRouteChange`, fakeAsync(() => {
      spyOn(metadataService as any, 'processRouteChange').and.callFake(() => undefined);
      metadataService.listenForRouteChange();
      tick();
      expect((metadataService as any).processRouteChange).toHaveBeenCalled();
    }));
    it(`should add Generator`, fakeAsync(() => {
      spyOn(metadataService as any, 'processRouteChange').and.callFake(() => undefined);
      metadataService.listenForRouteChange();
      tick();
      expect(meta.addTag).toHaveBeenCalledWith({
        name: 'Generator',
        content: 'mock-dspace-version'
      });
    }));
  });

  describe('citation_abstract_html_url', () => {
    it('should use dc.identifier.uri if available', fakeAsync(() => {
      (metadataService as any).processRouteChange({
        data: {
          value: {
            dso: createSuccessfulRemoteDataObject(mockUri(ItemMock, 'https://ddg.gg')),
          }
        }
      });
      tick();
      expect(meta.addTag).toHaveBeenCalledWith({
        name: 'citation_abstract_html_url',
        content: 'https://ddg.gg'
      });
    }));

    it('should use current route as fallback', fakeAsync(() => {
      (metadataService as any).processRouteChange({
        data: {
          value: {
            dso: createSuccessfulRemoteDataObject(mockUri(ItemMock)),
          }
        }
      });
      tick();
      expect(meta.addTag).toHaveBeenCalledWith({
        name: 'citation_abstract_html_url',
        content: 'https://request.org/items/0ec7ff22-f211-40ab-a69e-c819b0b1f357'
      });
    }));
  });

  describe('citation_*_institution / citation_publisher', () => {
    it('should use citation_dissertation_institution tag for dissertations', fakeAsync(() => {
      (metadataService as any).processRouteChange({
        data: {
          value: {
            dso: createSuccessfulRemoteDataObject(mockPublisher(mockType(ItemMock, 'Thesis'))),
          }
        }
      });
      tick();
      expect(meta.addTag).toHaveBeenCalledWith({
        name: 'citation_dissertation_institution',
        content: 'Mock Publisher'
      });
      expect(meta.addTag).not.toHaveBeenCalledWith(jasmine.objectContaining({ name: 'citation_technical_report_institution' }));
      expect(meta.addTag).not.toHaveBeenCalledWith(jasmine.objectContaining({ name: 'citation_publisher' }));
    }));

    it('should use citation_tech_report_institution tag for tech reports', fakeAsync(() => {
      (metadataService as any).processRouteChange({
        data: {
          value: {
            dso: createSuccessfulRemoteDataObject(mockPublisher(mockType(ItemMock, 'Technical Report'))),
          }
        }
      });
      tick();
      expect(meta.addTag).not.toHaveBeenCalledWith(jasmine.objectContaining({ name: 'citation_dissertation_institution' }));
      expect(meta.addTag).toHaveBeenCalledWith({
        name: 'citation_technical_report_institution',
        content: 'Mock Publisher'
      });
      expect(meta.addTag).not.toHaveBeenCalledWith(jasmine.objectContaining({ name: 'citation_publisher' }));
    }));

    it('should use citation_publisher for other item types', fakeAsync(() => {
      (metadataService as any).processRouteChange({
        data: {
          value: {
            dso: createSuccessfulRemoteDataObject(mockPublisher(mockType(ItemMock, 'Some Other Type'))),
          }
        }
      });
      tick();
      expect(meta.addTag).not.toHaveBeenCalledWith(jasmine.objectContaining({ name: 'citation_dissertation_institution' }));
      expect(meta.addTag).not.toHaveBeenCalledWith(jasmine.objectContaining({ name: 'citation_technical_report_institution' }));
      expect(meta.addTag).toHaveBeenCalledWith({
        name: 'citation_publisher',
        content: 'Mock Publisher'
      });
    }));
  });

  describe('citation_pdf_url', () => {
    it('should link to primary Bitstream URL regardless of format', fakeAsync(() => {
      (bundleDataService.findByItemAndName as jasmine.Spy).and.returnValue(mockBundleRD$([], MockBitstream3));

      (metadataService as any).processRouteChange({
        data: {
          value: {
            dso: createSuccessfulRemoteDataObject(ItemMock),
          }
        }
      });
      tick();
      expect(meta.addTag).toHaveBeenCalledWith({
        name: 'citation_pdf_url',
        content: 'https://request.org/bitstreams/4db100c1-e1f5-4055-9404-9bc3e2d15f29/download'
      });
    }));

    describe('bitstream not download allowed', () => {
      it('should not have citation_pdf_url', fakeAsync(() => {
        (bundleDataService.findByItemAndName as jasmine.Spy).and.returnValue(mockBundleRD$([MockBitstream3]));
        (authorizationService.isAuthorized as jasmine.Spy).and.returnValue(observableOf(false));

        (metadataService as any).processRouteChange({
          data: {
            value: {
              dso: createSuccessfulRemoteDataObject(ItemMock),
            }
          }
        });
        tick();
        expect(meta.addTag).not.toHaveBeenCalledWith(jasmine.objectContaining({ name: 'citation_pdf_url' }));
      }));

    });

    describe('no primary Bitstream', () => {
      it('should link to first and only Bitstream regardless of format', fakeAsync(() => {
        (bundleDataService.findByItemAndName as jasmine.Spy).and.returnValue(mockBundleRD$([MockBitstream3]));

        (metadataService as any).processRouteChange({
          data: {
            value: {
              dso: createSuccessfulRemoteDataObject(ItemMock),
            }
          }
        });
        tick();
        expect(meta.addTag).toHaveBeenCalledWith({
          name: 'citation_pdf_url',
          content: 'https://request.org/bitstreams/4db100c1-e1f5-4055-9404-9bc3e2d15f29/download'
        });
      }));

      describe(`when there's a bitstream with an allowed format on the first page`, () => {
        let bitstreams;

        beforeEach(() => {
          bitstreams = [MockBitstream2, MockBitstream3, MockBitstream1];
          (bundleDataService.findByItemAndName as jasmine.Spy).and.returnValue(mockBundleRD$(bitstreams));
          (bitstreamDataService.findListByHref as jasmine.Spy).and.returnValues(
            ...mockBitstreamPages$(bitstreams).map(bp => createSuccessfulRemoteDataObject$(bp)),
          );
        });

        it('should link to first Bitstream with allowed format', fakeAsync(() => {
          (metadataService as any).processRouteChange({
            data: {
              value: {
                dso: createSuccessfulRemoteDataObject(ItemMock),
              }
            }
          });
          tick();
          expect(meta.addTag).toHaveBeenCalledWith({
            name: 'citation_pdf_url',
            content: 'https://request.org/bitstreams/99b00f3c-1cc6-4689-8158-91965bee6b28/download'
          });
        }));

      });

    });
  });

  describe(`when there's no bitstream with an allowed format on the first page`, () => {
    let bitstreams;

    beforeEach(() => {
      bitstreams = [MockBitstream1, MockBitstream3, MockBitstream2];
      (bundleDataService.findByItemAndName as jasmine.Spy).and.returnValue(mockBundleRD$(bitstreams));
      (bitstreamDataService.findListByHref as jasmine.Spy).and.returnValues(
        ...mockBitstreamPages$(bitstreams).map(bp => createSuccessfulRemoteDataObject$(bp)),
      );
    });

    it(`shouldn't add a citation_pdf_url meta tag`, fakeAsync(() => {
      (metadataService as any).processRouteChange({
        data: {
          value: {
            dso: createSuccessfulRemoteDataObject(ItemMock),
          }
        }
      });
      tick();
      expect(meta.addTag).not.toHaveBeenCalledWith({
        name: 'citation_pdf_url',
        content: 'https://request.org/bitstreams/99b00f3c-1cc6-4689-8158-91965bee6b28/download'
      });
    }));

  });


  describe('tagstore', () => {
    beforeEach(fakeAsync(() => {
      (metadataService as any).processRouteChange({
        data: {
          value: {
            dso: createSuccessfulRemoteDataObject(ItemMock),
          }
        }
      });
      tick();
    }));

    it('should remove previous tags on route change', fakeAsync(() => {
      expect(meta.removeTag).toHaveBeenCalledWith('name=\'title\'');
      expect(meta.removeTag).toHaveBeenCalledWith('name=\'description\'');
    }));

    it('should clear all tags and add new ones on route change', () => {
      expect(store.dispatch.calls.argsFor(0)).toEqual([new ClearMetaTagAction()]);
      expect(store.dispatch.calls.argsFor(1)).toEqual([new AddMetaTagAction('title')]);
      expect(store.dispatch.calls.argsFor(2)).toEqual([new AddMetaTagAction('description')]);
    });
  });

  const mockType = (mockItem: Item, type: string): Item => {
    const typedMockItem = Object.assign(new Item(), mockItem) as Item;
    typedMockItem.metadata['dc.type'] = [{ value: type }] as MetadataValue[];
    return typedMockItem;
  };

  const mockPublisher = (mockItem: Item): Item => {
    const publishedMockItem = Object.assign(new Item(), mockItem) as Item;
    publishedMockItem.metadata['dc.publisher'] = [
      {
        language: 'en_US',
        value: 'Mock Publisher'
      }
    ] as MetadataValue[];
    return publishedMockItem;
  };

  const mockUri = (mockItem: Item, uri?: string): Item => {
    const publishedMockItem = Object.assign(new Item(), mockItem) as Item;
    publishedMockItem.metadata['dc.identifier.uri'] = [{ value: uri }] as MetadataValue[];
    return publishedMockItem;
  };

  const mockBundleRD$ = (bitstreams: Bitstream[], primary?: Bitstream): Observable<RemoteData<Bundle>> => {
    return createSuccessfulRemoteDataObject$(
      Object.assign(new Bundle(), {
        name: 'ORIGINAL',
        bitstreams: createSuccessfulRemoteDataObject$(mockBitstreamPages$(bitstreams)[0]),
        primaryBitstream: createSuccessfulRemoteDataObject$(primary),
      })
    );
  };

  const mockBitstreamPages$ = (bitstreams: Bitstream[]): PaginatedList<Bitstream>[] => {
    return bitstreams.map((bitstream, index) => Object.assign(createPaginatedList([bitstream]), {
      pageInfo: {
        totalElements: bitstreams.length,       // announce multiple elements/pages
      },
      _links: index < bitstreams.length - 1
        ? { next: { href: 'not empty' }}        // fake link to the next bitstream page
        : { next: { href: undefined }},         // last page has no link
    }));
  };
});
