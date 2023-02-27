import { HttpXsrfTokenExtractor } from '@angular/common/http';

/**
 * A Mock TokenExtractor which just returns whatever token it is initialized with.
 * This mock object is injected into our XsrfInterceptor, so that it always finds
 * the same fake XSRF token.
 */
export class HttpXsrfTokenExtractorMock extends HttpXsrfTokenExtractor {
    constructor(private token: string | null) { super(); }

    getToken(): string | null { return this.token; }
}
