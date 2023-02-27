import { of as observableOf } from 'rxjs';
import { followLink } from '../../shared/utils/follow-link-config.model';
import { ObjectCacheService } from '../cache/object-cache.service';
import { RelationshipType } from '../shared/item-relationships/relationship-type.model';
import { Relationship } from '../shared/item-relationships/relationship.model';
import { Item } from '../shared/item.model';
import { PageInfo } from '../shared/page-info.model';
import { buildPaginatedList } from './paginated-list.model';
import { DeleteRequest } from './request.models';
import { RelationshipDataService } from './relationship-data.service';
import { RequestService } from './request.service';
import { HALEndpointServiceStub } from '../../shared/testing/hal-endpoint-service.stub';
import {
  createFailedRemoteDataObject$,
  createSuccessfulRemoteDataObject,
  createSuccessfulRemoteDataObject$
} from '../../shared/remote-data.utils';
import { getMockRemoteDataBuildServiceHrefMap } from '../../shared/mocks/remote-data-build.service.mock';
import { getMockRequestService } from '../../shared/mocks/request.service.mock';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { RequestEntry } from './request-entry.model';
import { FindListOptions } from './find-list-options.model';
import { testSearchDataImplementation } from './base/search-data.spec';
import { MetadataValue } from '../shared/metadata.models';
import { MetadataRepresentationType } from '../shared/metadata-representation/metadata-representation.model';

describe('RelationshipDataService', () => {
  let service: RelationshipDataService;
  let requestService: RequestService;

  const restEndpointURL = 'https://rest.api/core';
  const relationshipsEndpointURL = `${restEndpointURL}/relationships`;
  const halService: any = new HALEndpointServiceStub(restEndpointURL);

  const relationshipType = Object.assign(new RelationshipType(), {
    id: '1',
    uuid: '1',
    leftwardType: 'isAuthorOfPublication',
    rightwardType: 'isPublicationOfAuthor'
  });

  const ri1SelfLink = restEndpointURL + '/author1';
  const ri2SelfLink = restEndpointURL + '/author2';
  const itemSelfLink = restEndpointURL + '/publication';

  const relationship1 = Object.assign(new Relationship(), {
    _links: {
      self: {
        href: relationshipsEndpointURL + '/2'
      },
      leftItem: {
        href: ri1SelfLink
      },
      rightItem: {
        href: itemSelfLink
      }
    },
    id: '2',
    uuid: '2',
    relationshipType: createSuccessfulRemoteDataObject$(relationshipType)
  });
  const relationship2 = Object.assign(new Relationship(), {
    _links: {
      self: {
        href: relationshipsEndpointURL + '/3'
      },
      leftItem: {
        href: ri2SelfLink
      },
      rightItem: {
        href: itemSelfLink
      },
    },
    id: '3',
    uuid: '3',
    relationshipType: createSuccessfulRemoteDataObject$(relationshipType)
  });

  const relationships = [relationship1, relationship2];
  const item = Object.assign(new Item(), {
    id: 'publication',
    uuid: 'publication',
    relationships: createSuccessfulRemoteDataObject$(createPaginatedList(relationships)),
    _links: {
      relationships: { href: restEndpointURL + '/publication/relationships' },
      self: { href: itemSelfLink }
    }
  });

  const relatedItem1 = Object.assign(new Item(), {
    id: 'author1',
    uuid: 'author1',
    _links: {
      self: { href: ri1SelfLink }
    }
  });
  const relatedItem2 = Object.assign(new Item(), {
    id: 'author2',
    uuid: 'author2',
    _links: {
      self: { href: ri2SelfLink }
    }
  });

  relationship1.leftItem = createSuccessfulRemoteDataObject$(relatedItem1);
  relationship1.rightItem = createSuccessfulRemoteDataObject$(item);
  relationship2.leftItem = createSuccessfulRemoteDataObject$(relatedItem2);
  relationship2.rightItem = createSuccessfulRemoteDataObject$(item);
  const relatedItems = [relatedItem1, relatedItem2];

  const buildList$ = createSuccessfulRemoteDataObject$(createPaginatedList(relatedItems));
  const relationships$ = createSuccessfulRemoteDataObject$(createPaginatedList(relationships));
  const rdbService = getMockRemoteDataBuildServiceHrefMap(undefined, {
    'href': buildList$,
    'https://rest.api/core/publication/relationships': relationships$
  });
  const objectCache = Object.assign({
    /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
    remove: () => {
    },
    hasBySelfLinkObservable: () => observableOf(false),
    hasByHref$: () => observableOf(false)
    /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
  }) as ObjectCacheService;

  const itemService = jasmine.createSpyObj('itemService', {
    findById: (uuid) => createSuccessfulRemoteDataObject(relatedItems.find((relatedItem) => relatedItem.id === uuid)),
    findByHref: createSuccessfulRemoteDataObject$(relatedItems[0])
  });

  function initTestService() {
    return new RelationshipDataService(
      requestService,
      rdbService,
      halService,
      objectCache,
      itemService,
      null,
      jasmine.createSpy('paginatedRelationsToItems').and.returnValue((v) => v),
    );
  }

  const getRequestEntry$ = (successful: boolean) => {
    return observableOf({
      response: { isSuccessful: successful, payload: relationships } as any
    } as RequestEntry);
  };

  beforeEach(() => {
    requestService = getMockRequestService(getRequestEntry$(true));
    service = initTestService();
  });

  describe('composition', () => {
    const initService = () => new RelationshipDataService(null, null, null, null, null, null, null);

    testSearchDataImplementation(initService);
  });

  describe('deleteRelationship', () => {
    beforeEach(() => {
      spyOn(service, 'findById').and.returnValue(createSuccessfulRemoteDataObject$(relationship1));
      spyOn(objectCache, 'remove');
      service.deleteRelationship(relationships[0].uuid, 'right').subscribe();
    });

    it('should send a DeleteRequest', () => {
      const expected = new DeleteRequest(requestService.generateRequestId(), relationshipsEndpointURL + '/' + relationship1.uuid + '?copyVirtualMetadata=right');
      expect(requestService.send).toHaveBeenCalledWith(expected);
    });

    it('should clear the cache of the related items', () => {
      expect(objectCache.remove).toHaveBeenCalledWith(relatedItem1._links.self.href);
      expect(objectCache.remove).toHaveBeenCalledWith(item._links.self.href);
      expect(requestService.removeByHrefSubstring).toHaveBeenCalledWith(relatedItem1.uuid);
      expect(requestService.removeByHrefSubstring).toHaveBeenCalledWith(item.uuid);
    });
  });

  describe('getItemRelationshipsArray', () => {
    it('should return the item\'s relationships in the form of an array', (done) => {
      service.getItemRelationshipsArray(item).subscribe((result) => {
        expect(result).toEqual(relationships);
        done();
      });
    });
  });

  describe('getRelatedItemsByLabel', () => {
    let relationsList;
    let mockItem;
    let mockLabel;
    let mockOptions;

    beforeEach(() => {
      relationsList = buildPaginatedList(new PageInfo({
        elementsPerPage: relationships.length,
        totalElements: relationships.length,
        currentPage: 1,
        totalPages: 1
      }), relationships);
      mockItem = { uuid: 'someid' } as Item;
      mockLabel = 'label';
      mockOptions = { label: 'options' } as FindListOptions;

      const rd$ = createSuccessfulRemoteDataObject$(relationsList);
      spyOn(service, 'getItemRelationshipsByLabel').and.returnValue(rd$);
    });

    it('should call getItemRelationshipsByLabel with the correct params', (done) => {
      mockOptions = Object.assign(mockOptions, { fetchThumbnail: true });
      service.getRelatedItemsByLabel(
        mockItem,
        mockLabel,
        mockOptions
      ).subscribe((result) => {
        expect(service.getItemRelationshipsByLabel).toHaveBeenCalledWith(
          mockItem,
          mockLabel,
          mockOptions,
          true,
          true,
          followLink('leftItem',{}, followLink('thumbnail')),
          followLink('rightItem',{}, followLink('thumbnail')),
          followLink('relationshipType')
        );
        done();
      });
    });

    it('should use the paginatedRelationsToItems operator', (done) => {
      service.getRelatedItemsByLabel(
        mockItem,
        mockLabel,
        mockOptions
      ).subscribe((result) => {
        expect((service as any).paginatedRelationsToItems).toHaveBeenCalledWith(mockItem.uuid);
        done();
      });
    });
  });

  describe('resolveMetadataRepresentation', () => {
    const parentItem: Item = Object.assign(new Item(), {
      id: 'parent-item',
      metadata: {
        'dc.contributor.author': [
          Object.assign(new MetadataValue(), {
            language: null,
            value: 'Related Author with authority',
            authority: 'virtual::related-author',
            place: 2
          }),
          Object.assign(new MetadataValue(), {
            language: null,
            value: 'Author without authority',
            place: 1
          }),
        ],
        'dc.creator': [
          Object.assign(new MetadataValue(), {
            language: null,
            value: 'Related Creator with authority',
            authority: 'virtual::related-creator',
            place: 3,
          }),
            Object.assign(new MetadataValue(), {
            language: null,
            value: 'Related Creator with authority - unauthorized',
            authority: 'virtual::related-creator-unauthorized',
            place: 4,
          }),
        ],
        'dc.title': [
          Object.assign(new MetadataValue(), {
            language: null,
            value: 'Parent Item'
          }),
        ]
      }
    });
    const relatedAuthor: Item = Object.assign(new Item(), {
      id: 'related-author',
      metadata: {
        'dc.title': [
          Object.assign(new MetadataValue(), {
            language: null,
            value: 'Related Author'
          }),
        ]
      }
    });
    const relatedCreator: Item = Object.assign(new Item(), {
      id: 'related-creator',
      metadata: {
        'dc.title': [
          Object.assign(new MetadataValue(), {
            language: null,
            value: 'Related Creator'
          }),
        ],
        'dspace.entity.type': 'Person',
      }
    });
    const authorRelation: Relationship = Object.assign(new Relationship(), {
      leftItem: createSuccessfulRemoteDataObject$(parentItem),
      rightItem: createSuccessfulRemoteDataObject$(relatedAuthor)
    });
    const creatorRelation: Relationship = Object.assign(new Relationship(), {
      leftItem: createSuccessfulRemoteDataObject$(parentItem),
      rightItem: createSuccessfulRemoteDataObject$(relatedCreator),
    });
    const creatorRelationUnauthorized: Relationship = Object.assign(new Relationship(), {
      leftItem: createSuccessfulRemoteDataObject$(parentItem),
      rightItem: createFailedRemoteDataObject$('Unauthorized', 401),
    });

    let metadatum: MetadataValue;

    beforeEach(() => {
      service.findById = (id: string) => {
        if (id === 'related-author') {
          return createSuccessfulRemoteDataObject$(authorRelation);
        }
        if (id === 'related-creator') {
          return createSuccessfulRemoteDataObject$(creatorRelation);
        }
        if (id === 'related-creator-unauthorized') {
          return createSuccessfulRemoteDataObject$(creatorRelationUnauthorized);
        }
      };
    });

    describe('when the metadata isn\'t virtual', () => {
      beforeEach(() => {
        metadatum = parentItem.metadata['dc.contributor.author'][1];
      });

      it('should return a plain text MetadatumRepresentation', (done) => {
        service.resolveMetadataRepresentation(metadatum, parentItem, 'Person').subscribe((result) => {
          expect(result.representationType).toEqual(MetadataRepresentationType.PlainText);
          done();
        });
      });
    });

    describe('when the metadata is a virtual author', () => {
      beforeEach(() => {
        metadatum = parentItem.metadata['dc.contributor.author'][0];
      });

      it('should return a ItemMetadataRepresentation with the correct value', (done) => {
        service.resolveMetadataRepresentation(metadatum, parentItem, 'Person').subscribe((result) => {
          expect(result.representationType).toEqual(MetadataRepresentationType.Item);
          expect(result.getValue()).toEqual(metadatum.value);
          expect((result as any).id).toEqual(relatedAuthor.id);
          done();
        });
      });
    });

    describe('when the metadata is a virtual creator', () => {
      beforeEach(() => {
        metadatum = parentItem.metadata['dc.creator'][0];
      });

      it('should return a ItemMetadataRepresentation with the correct value', (done) => {
        service.resolveMetadataRepresentation(metadatum, parentItem, 'Person').subscribe((result) => {
          expect(result.representationType).toEqual(MetadataRepresentationType.Item);
          expect(result.getValue()).toEqual(metadatum.value);
          expect((result as any).id).toEqual(relatedCreator.id);
          done();
        });
      });
    });

    describe('when the metadata refers to a relationship leading to an error response', () => {
      beforeEach(() => {
        metadatum = parentItem.metadata['dc.creator'][1];
      });

      it('should return an authority controlled MetadatumRepresentation', (done) => {
        service.resolveMetadataRepresentation(metadatum, parentItem, 'Person').subscribe((result) => {
          expect(result.representationType).toEqual(MetadataRepresentationType.AuthorityControlled);
          done();
        });
      });
    });
  });
});
