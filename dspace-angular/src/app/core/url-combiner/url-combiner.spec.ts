import { URLCombiner } from './url-combiner';

describe('URLCombiner', () => {

  it('should return a valid URL when created with a valid set of url parts', () => {
    const url = new URLCombiner('http://foo.com', 'bar', 'id', '5').toString();
    expect(url).toBe('http://foo.com/bar/id/5');
  });

  it('should return a URL with the protocol followed by two slashes', () => {
    const url = new URLCombiner('http:/foo.com').toString();
    expect(url).toBe('http://foo.com');
  });

  it('should return a URL with a single slash between each part', () => {
    const url = new URLCombiner('http://foo.com/', '/bar/', '//id', '///5').toString();
    expect(url).toBe('http://foo.com/bar/id/5');
  });

  it('should return a URL without a trailing slash before its parameters', () => {
    const url1 = new URLCombiner('http://foo.com/', '?bar=25').toString();
    const url2 = new URLCombiner('http://foo.com/', '#bar').toString();

    expect(url1).toBe('http://foo.com?bar=25');
    expect(url2).toBe('http://foo.com#bar');
  });

  it('should return an empty string when created without url parts', () => {
    const url = new URLCombiner().toString();
    expect(url).toBe('');
  });

});
