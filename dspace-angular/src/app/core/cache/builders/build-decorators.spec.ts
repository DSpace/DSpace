import { HALLink } from '../../shared/hal-link.model';
import { HALResource } from '../../shared/hal-resource.model';
import { ResourceType } from '../../shared/resource-type';
import { getLinkDefinition, link } from './build-decorators';

class TestHALResource implements HALResource {
  _links: {
    self: HALLink;
    foo: HALLink;
  };

  bar?: any;
}

let testType;

describe('build decorators', () => {
  beforeEach(() => {
    testType = new ResourceType('testType-' + new Date().getTime());
  });

  describe(`@link/getLinkDefinitions`, () => {
    it(`should register a link`, () => {
      const target = new TestHALResource();
      link(testType, true, 'foo')(target, 'bar');
      const result = getLinkDefinition(TestHALResource, 'foo');
      expect(result.resourceType).toBe(testType);
      expect(result.isList).toBe(true);
      expect(result.linkName).toBe('foo');
      expect(result.propertyName).toBe('bar');
    });

    describe(`when the linkname isn't specified`, () => {
      it(`should use the propertyname`, () => {
        const target = new TestHALResource();
        link(testType)(target, 'foo');
        const result = getLinkDefinition(TestHALResource, 'foo');
        expect(result.linkName).toBe('foo');
        expect(result.propertyName).toBe('foo');
      });
    });

    describe(`when there's no @link`, () => {
      it(`should return undefined`, () => {
        const result = getLinkDefinition(TestHALResource, 'self');
        expect(result).toBeUndefined();
      });
    });
  });
});
