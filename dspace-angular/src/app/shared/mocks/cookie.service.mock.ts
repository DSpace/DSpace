/**
 * Mock for [[CookieService]]
 */
export class CookieServiceMock {
  cookies: Map<string, string>;

  constructor(cookies: Map<string, string> = new Map()) {
    this.cookies = cookies;
  }

  set(name, value) {
    this.cookies.set(name, value);
  }

  get(name) {
    return this.cookies.get(name);
  }

  remove(name) {
    this.cookies.delete(name);
    return jasmine.createSpy('remove');
  }

  getAll() {
    return jasmine.createSpy('getAll');
  }
}
