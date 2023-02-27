import { getEmbedSizeParams, getUrlWithoutEmbedParams } from './index.selectors';


describe(`index selectors`, () => {

  describe(`getUrlWithoutEmbedParams`, () => {

    it(`should return a url without its embed params`, () => {
      const source = 'https://rest.api/resource?a=1&embed=2&b=3&embed=4/5&c=6&embed=7/8/9';
      const result = getUrlWithoutEmbedParams(source);
      expect(result).toBe('https://rest.api/resource?a=1&b=3&c=6');
    });

    it(`should return a url without embed params unmodified`, () => {
      const source = 'https://rest.api/resource?a=1&b=3&c=6';
      const result = getUrlWithoutEmbedParams(source);
      expect(result).toBe(source);
    });

    it(`should return a string that isn't a url unmodified`, () => {
      const source = 'a=1&embed=2&b=3&embed=4/5&c=6&embed=7/8/9';
      const result = getUrlWithoutEmbedParams(source);
      expect(result).toBe(source);
    });

    it(`should return undefined or null unmodified`, () => {
      expect(getUrlWithoutEmbedParams(undefined)).toBe(undefined);
      expect(getUrlWithoutEmbedParams(null)).toBe(null);
    });

  });

  describe(`getEmbedSizeParams`, () => {

    it(`url with single embed size param => should return list with ['subcommunities' - size]`, () => {
      const source = 'https://rest.api/core/communities/search/top?page=0&size=50&sort=dc.title,ASC&embed.size=subcommunities=5&embed=subcommunities';
      const result = getEmbedSizeParams(source);
      expect(result).toHaveSize(1);
      expect(result[0]).toEqual({name: 'subcommunities', size: 5});
    });

    it(`url with multiple embed size param => should return list with {name, size}`, () => {
      const source = 'https://rest.api/core/communities/search/top?page=0&size=50&sort=dc.title,ASC&embed.size=subcommunities=5&embed=subcommunities&embed.size=collections=1&embed=collections';
      const result = getEmbedSizeParams(source);
      expect(result).toHaveSize(2);
      expect(result[0]).toEqual({name: 'subcommunities', size: 5});
      expect(result[1]).toEqual({name: 'collections', size: 1});
    });

    it(`url without params => should return empty list`, () => {
      const source = 'https://rest.api/core/collections/uuid';
      expect(getEmbedSizeParams(source)).toHaveSize(0);
    });

    it(`url without embed size params => should return empty list`, () => {
      const source = 'https://rest.api/core/collections/uuid?page=0&size=50';
      expect(getEmbedSizeParams(source)).toHaveSize(0);
    });

    it(`undefined or null url => should return empty list`, () => {
      expect(getEmbedSizeParams(undefined)).toHaveSize(0);
      expect(getEmbedSizeParams(null)).toHaveSize(0);
    });

  });

});
