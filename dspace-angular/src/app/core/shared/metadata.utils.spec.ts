import { isUndefined } from '../../shared/empty.util';
import { v4 as uuidv4 } from 'uuid';
import { MetadataMap, MetadataValue, MetadataValueFilter, MetadatumViewModel } from './metadata.models';
import { Metadata } from './metadata.utils';

const mdValue = (value: string, language?: string, authority?: string): MetadataValue => {
  return Object.assign(new MetadataValue(), {
    uuid: uuidv4(),
    value: value,
    language: isUndefined(language) ? null : language,
    place: 0,
    authority: isUndefined(authority) ? null : authority,
    confidence: undefined
  });
};

const dcDescription = mdValue('Some description');
const dcAbstract = mdValue('Some abstract');
const dcTitle0 = mdValue('Title 0');
const dcTitle1 = mdValue('Title 1');
const dcTitle2 = mdValue('Title 2', 'en_US');
const bar = mdValue('Bar');
const test = mdValue('Test');

const singleMap = { 'dc.title': [dcTitle0] };

const multiMap = {
  'dc.description': [dcDescription],
  'dc.description.abstract': [dcAbstract],
  'dc.title': [dcTitle1, dcTitle2],
  'foo': [bar]
};

const regexTestMap = {
  'foolbar.baz': [test],
  'foo.bard': [test],
};

const multiViewModelList = [
  { key: 'dc.description', ...dcDescription, order: 0 },
  { key: 'dc.description.abstract', ...dcAbstract, order: 0 },
  { key: 'dc.title', ...dcTitle1, order: 0 },
  { key: 'dc.title', ...dcTitle2, order: 1 },
  { key: 'foo', ...bar, order: 0 }
];

const testMethod = (fn, resultKind, mapOrMaps, keyOrKeys, expected, filter?) => {
  const keys = keyOrKeys instanceof Array ? keyOrKeys : [keyOrKeys];
  describe('and key' + (keys.length === 1 ? (' ' + keys[0]) : ('s ' + JSON.stringify(keys)))
    + ' with ' + (isUndefined(filter) ? 'no filter' : 'filter ' + JSON.stringify(filter)), () => {
    const result = fn(mapOrMaps, keys, filter);
    let shouldReturn;
    if (resultKind === 'boolean') {
      shouldReturn = expected;
    } else if (isUndefined(expected)) {
      shouldReturn = 'undefined';
    } else if (expected instanceof Array) {
      shouldReturn = 'an array with ' + expected.length + ' ' + (expected.length > 1 ? 'ordered ' : '')
        + resultKind + (expected.length !== 1 ? 's' : '');
    } else {
      shouldReturn = 'a ' + resultKind;
    }
    it('should return ' + shouldReturn, () => {
      expect(result).toEqual(expected);
    });
  });
};

describe('Metadata', () => {

  describe('all method', () => {

    const testAll = (mapOrMaps, keyOrKeys, expected, filter?: MetadataValueFilter) =>
      testMethod(Metadata.all, 'value', mapOrMaps, keyOrKeys, expected, filter);

    describe('with emptyMap', () => {
      testAll({}, 'foo', []);
      testAll({}, '*', []);
    });
    describe('with singleMap', () => {
      testAll(singleMap, 'foo', []);
      testAll(singleMap, '*', [dcTitle0]);
      testAll(singleMap, '*', [], { value: 'baz' });
      testAll(singleMap, 'dc.title', [dcTitle0]);
      testAll(singleMap, 'dc.*', [dcTitle0]);
    });
    describe('with multiMap', () => {
      testAll(multiMap, 'foo', [bar]);
      testAll(multiMap, '*', [dcDescription, dcAbstract, dcTitle1, dcTitle2, bar]);
      testAll(multiMap, 'dc.title', [dcTitle1, dcTitle2]);
      testAll(multiMap, 'dc.*', [dcDescription, dcAbstract, dcTitle1, dcTitle2]);
      testAll(multiMap, ['dc.title', 'dc.*'], [dcTitle1, dcTitle2, dcDescription, dcAbstract]);
    });
    describe('with [ singleMap, multiMap ]', () => {
      testAll([singleMap, multiMap], 'foo', [bar]);
      testAll([singleMap, multiMap], '*', [dcTitle0]);
      testAll([singleMap, multiMap], 'dc.title', [dcTitle0]);
      testAll([singleMap, multiMap], 'dc.*', [dcTitle0]);
    });
    describe('with [ multiMap, singleMap ]', () => {
      testAll([multiMap, singleMap], 'foo', [bar]);
      testAll([multiMap, singleMap], '*', [dcDescription, dcAbstract, dcTitle1, dcTitle2, bar]);
      testAll([multiMap, singleMap], 'dc.title', [dcTitle1, dcTitle2]);
      testAll([multiMap, singleMap], 'dc.*', [dcDescription, dcAbstract, dcTitle1, dcTitle2]);
      testAll([multiMap, singleMap], ['dc.title', 'dc.*'], [dcTitle1, dcTitle2, dcDescription, dcAbstract]);
    });
    describe('with regexTestMap', () => {
      testAll(regexTestMap, 'foo.bar.*', []);
    });
  });

  describe('allValues method', () => {

    const testAllValues = (mapOrMaps, keyOrKeys, expected) =>
      testMethod(Metadata.allValues, 'string', mapOrMaps, keyOrKeys, expected);

    describe('with emptyMap', () => {
      testAllValues({}, '*', []);
    });
    describe('with singleMap', () => {
      testAllValues([singleMap, multiMap], '*', [dcTitle0.value]);
    });
    describe('with [ multiMap, singleMap ]', () => {
      testAllValues([multiMap, singleMap], '*', [dcDescription.value, dcAbstract.value, dcTitle1.value, dcTitle2.value, bar.value]);
    });
  });

  describe('first method', () => {

    const testFirst = (mapOrMaps, keyOrKeys, expected) =>
      testMethod(Metadata.first, 'value', mapOrMaps, keyOrKeys, expected);

    describe('with emptyMap', () => {
      testFirst({}, '*', undefined);
    });
    describe('with singleMap', () => {
      testFirst(singleMap, '*', dcTitle0);
    });
    describe('with [ multiMap, singleMap ]', () => {
      testFirst([multiMap, singleMap], '*', dcDescription);
    });
  });

  describe('firstValue method', () => {

    const testFirstValue = (mapOrMaps, keyOrKeys, expected) =>
      testMethod(Metadata.firstValue, 'value', mapOrMaps, keyOrKeys, expected);

    describe('with emptyMap', () => {
      testFirstValue({}, '*', undefined);
    });
    describe('with singleMap', () => {
      testFirstValue(singleMap, '*', dcTitle0.value);
    });
    describe('with [ multiMap, singleMap ]', () => {
      testFirstValue([multiMap, singleMap], '*', dcDescription.value);
    });
  });

  describe('has method', () => {

    const testHas = (mapOrMaps, keyOrKeys, expected, filter?: MetadataValueFilter) =>
      testMethod(Metadata.has, 'boolean', mapOrMaps, keyOrKeys, expected, filter);

    describe('with emptyMap', () => {
      testHas({}, '*', false);
    });
    describe('with singleMap', () => {
      testHas(singleMap, '*', true);
      testHas(singleMap, '*', false, { value: 'baz' });
    });
    describe('with [ multiMap, singleMap ]', () => {
      testHas([multiMap, singleMap], '*', true);
    });
  });

  describe('valueMatches method', () => {

    const testValueMatches = (value: MetadataValue, expected: boolean, filter?: MetadataValueFilter) => {
      describe('with value ' + JSON.stringify(value) + ' and filter '
        + (isUndefined(filter) ? 'undefined' : JSON.stringify(filter)), () => {
        const result = Metadata.valueMatches(value, filter);
        it('should return ' + expected, () => {
          expect(result).toEqual(expected);
        });
      });
    };

    testValueMatches(mdValue('a'), true);
    testValueMatches(mdValue('a'), true, { value: 'a' });
    testValueMatches(mdValue('a'), false, { value: 'A' });
    testValueMatches(mdValue('a'), true, { value: 'A', ignoreCase: true });
    testValueMatches(mdValue('ab'), false, { value: 'b' });
    testValueMatches(mdValue('ab'), true, { value: 'b', substring: true });
    testValueMatches(mdValue('a'), true, { language: null });
    testValueMatches(mdValue('a'), false, { language: 'en_US' });
    testValueMatches(mdValue('a', 'en_US'), true, { language: 'en_US' });
    testValueMatches(mdValue('a', undefined, '4321'), true, { authority: '4321' });
    testValueMatches(mdValue('a', undefined, '4321'), false, { authority: '1234' });
  });

  describe('toViewModelList method', () => {

    const testToViewModelList = (map: MetadataMap, expected: MetadatumViewModel[]) => {
      describe('with map ' + JSON.stringify(map), () => {
        const result = Metadata.toViewModelList(map);
        it('should return ' + JSON.stringify(expected), () => {
          expect(result).toEqual(expected);
        });
      });
    };

    testToViewModelList(multiMap, multiViewModelList);
  });

  describe('toMetadataMap method', () => {

    const testToMetadataMap = (metadatumList: MetadatumViewModel[], expected: MetadataMap) => {
      describe('with metadatum list ' + JSON.stringify(metadatumList), () => {
        const result = Metadata.toMetadataMap(metadatumList);
        it('should return ' + JSON.stringify(expected), () => {
          expect(result).toEqual(expected);
        });
      });
    };

    testToMetadataMap(multiViewModelList, multiMap);
  });

  describe('setFirstValue method', () => {

    const metadataMap = {
      'dc.description': [mdValue('Test description')],
      'dc.title': [mdValue('Test title 1'), mdValue('Test title 2')]
    };

    const testSetFirstValue = (map: MetadataMap, key: string, value: string) => {
      describe(`with field ${key} and value ${value}`, () => {
        Metadata.setFirstValue(map, key, value);
        it(`should set first value of ${key} to ${value}`, () => {
          expect(map[key][0].value).toEqual(value);
        });
      });
    };

    testSetFirstValue(metadataMap, 'dc.description', 'New Description');
    testSetFirstValue(metadataMap, 'dc.title', 'New Title');
    testSetFirstValue(metadataMap, 'dc.format', 'Completely new field and value');

  });

});
