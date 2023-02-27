/* eslint-disable max-classes-per-file */
import { Injectable } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { followLink, FollowLinkConfig } from '../../../shared/utils/follow-link-config.model';
import { HALLink } from '../../shared/hal-link.model';
import { HALResource } from '../../shared/hal-resource.model';
import { ResourceType } from '../../shared/resource-type';
import { LinkService } from './link.service';
import { LINK_DEFINITION_FACTORY, LINK_DEFINITION_MAP_FACTORY } from './build-decorators';
import { isEmpty } from 'rxjs/operators';
import { FindListOptions } from '../../data/find-list-options.model';
import { DATA_SERVICE_FACTORY } from '../../data/base/data-service.decorator';

const TEST_MODEL = new ResourceType('testmodel');
let result: any;

class TestModel implements HALResource {
  static type = TEST_MODEL;

  type = TEST_MODEL;

  value: string;

  _links: {
    self: HALLink;
    predecessor: HALLink;
    successor: HALLink;
  };

  predecessor?: TestModel;
  successor?: TestModel;
}

@Injectable()
class TestDataService {
  findListByHref(href: string, findListOptions: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<any>[]) {
    return 'findListByHref';
  }

  findByHref(href: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<any>[]) {
    return 'findByHref';
  }
}

let testDataService: TestDataService;

let testModel: TestModel;

describe('LinkService', () => {
  let service: LinkService;

  beforeEach(() => {
    testModel = Object.assign(new TestModel(), {
      value: 'a test value',
      _links: {
        self: {
          href: 'http://self.link'
        },
        predecessor: {
          href: 'http://predecessor.link'
        },
        successor: {
          href: 'http://successor.link'
        },
      }
    });
    testDataService = new TestDataService();
    spyOn(testDataService, 'findListByHref').and.callThrough();
    spyOn(testDataService, 'findByHref').and.callThrough();
    TestBed.configureTestingModule({
      providers: [LinkService, {
        provide: TestDataService,
        useValue: testDataService
      }, {
        provide: DATA_SERVICE_FACTORY,
        useValue: jasmine.createSpy('getDataServiceFor').and.returnValue(TestDataService),
      }, {
        provide: LINK_DEFINITION_FACTORY,
        useValue: jasmine.createSpy('getLinkDefinition').and.returnValue({
          resourceType: TEST_MODEL,
          linkName: 'predecessor',
          propertyName: 'predecessor'
        }),
      }, {
        provide: LINK_DEFINITION_MAP_FACTORY,
        useValue: jasmine.createSpy('getLinkDefinitions').and.returnValue([
          {
            resourceType: TEST_MODEL,
            linkName: 'predecessor',
            propertyName: 'predecessor',
          },
          {
            resourceType: TEST_MODEL,
            linkName: 'successor',
            propertyName: 'successor',
          }
        ]),
      }]
    });
    service = TestBed.inject(LinkService);
  });

  describe('resolveLink', () => {
    describe(`when the linkdefinition concerns a single object`, () => {
      beforeEach(() => {
        service.resolveLink(testModel, followLink('predecessor', {}, followLink('successor')));
      });
      it('should call dataservice.findByHref with the correct href and nested links', () => {
        expect(testDataService.findByHref).toHaveBeenCalledWith(testModel._links.predecessor.href, true, true, followLink('successor'));
      });
    });
    describe(`when the linkdefinition concerns a list`, () => {
      beforeEach(() => {
        ((service as any).getLinkDefinition as jasmine.Spy).and.returnValue({
          resourceType: TEST_MODEL,
          linkName: 'predecessor',
          propertyName: 'predecessor',
          isList: true
        });
        service.resolveLink(testModel, followLink('predecessor', { findListOptions: { some: 'options ' } as any }, followLink('successor')));
      });
      it('should call dataservice.findListByHref with the correct href, findListOptions,  and nested links', () => {
        expect(testDataService.findListByHref).toHaveBeenCalledWith(testModel._links.predecessor.href, { some: 'options ' } as any, true, true, followLink('successor'));
      });
    });
    describe('either way', () => {
      beforeEach(() => {
        result = service.resolveLink(testModel, followLink('predecessor', {}, followLink('successor')));
      });

      it('should call getLinkDefinition with the correct model and link', () => {
        expect((service as any).getLinkDefinition).toHaveBeenCalledWith(testModel.constructor as any, 'predecessor');
      });

      it('should call getDataServiceFor with the correct resource type', () => {
        expect((service as any).getDataServiceFor).toHaveBeenCalledWith(TEST_MODEL);
      });

      it('should return the model with the resolved link', () => {
        expect(result.type).toBe(TEST_MODEL);
        expect(result.value).toBe('a test value');
        expect(result._links.self.href).toBe('http://self.link');
        expect(result.predecessor).toBe('findByHref');
      });
    });

    describe(`when the specified link doesn't exist on the model's class`, () => {
      beforeEach(() => {
        ((service as any).getLinkDefinition as jasmine.Spy).and.returnValue(undefined);
      });
      it('should throw an error', () => {
        expect(() => {
          service.resolveLink(testModel, followLink('predecessor', {}, followLink('successor')));
        }).toThrow();
      });
    });

    describe(`when there is no dataservice for the resourcetype in the link`, () => {
      beforeEach(() => {
        ((service as any).getDataServiceFor as jasmine.Spy).and.returnValue(undefined);
      });
      it('should throw an error', () => {
        expect(() => {
          service.resolveLink(testModel, followLink('predecessor', {}, followLink('successor')));
        }).toThrow();
      });
    });
  });

  describe('resolveLinks', () => {
    beforeEach(() => {
      spyOn(service, 'resolveLink');
      result = service.resolveLinks(testModel, followLink('predecessor'), followLink('successor'));
    });

    it('should call resolveLink with the model for each of the provided links', () => {
      expect(service.resolveLink).toHaveBeenCalledWith(testModel, followLink('predecessor'));
      expect(service.resolveLink).toHaveBeenCalledWith(testModel, followLink('successor'));
    });

    it('should return the model', () => {
      expect(result.type).toBe(TEST_MODEL);
      expect(result.value).toBe('a test value');
      expect(result._links.self.href).toBe('http://self.link');
    });
  });

  describe('removeResolvedLinks', () => {
    beforeEach(() => {
      testModel.predecessor = 'predecessor value' as any;
      testModel.successor = 'successor value' as any;
    });

    it('should return a new version of the object without any resolved links', () => {
      result = service.removeResolvedLinks(testModel);
      expect(result.value).toBe(testModel.value);
      expect(result.type).toBe(testModel.type);
      expect(result._links).toBe(testModel._links);
      expect(result.predecessor).toBeUndefined();
      expect(result.successor).toBeUndefined();
    });

    it('should leave the original object untouched', () => {
      service.removeResolvedLinks(testModel);
      expect(testModel.predecessor as any).toBe('predecessor value');
      expect(testModel.successor as any).toBe('successor value');
    });
  });

  describe('when a link is missing', () => {
    beforeEach(() => {
      testModel = Object.assign(new TestModel(), {
        value: 'a test value',
        _links: {
          self: {
            href: 'http://self.link'
          },
          predecessor: {
            href: 'http://predecessor.link'
          }
        }
      });
    });

    describe('resolving the available link', () => {
      beforeEach(() => {
        result = service.resolveLinks(testModel, followLink('predecessor'));
      });

      it('should return the model with the resolved link', () => {
        expect(result.predecessor).toBe('findByHref');
      });
    });

    describe('resolving the missing link', () => {
      beforeEach(() => {
        ((service as any).getLinkDefinition as jasmine.Spy).and.returnValue({
          resourceType: TEST_MODEL,
          linkName: 'successor',
          propertyName: 'successor'
        });
        result = service.resolveLinks(testModel, followLink('successor'));
      });

      it('should resolve to an empty observable', (done) => {
        result.successor.pipe(isEmpty()).subscribe((v) => {
          expect(v).toEqual(true);
          done();
        });
      });
    });
  });

});
