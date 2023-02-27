import { DOCUMENT } from '@angular/common';
import { Renderer2, RendererFactory2 } from '@angular/core';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { MockProvider } from 'ng-mocks';
import { LinkHeadService } from './link-head.service';

describe('LinkHeadService', () => {

    let service: LinkHeadService;

    const renderer2: Renderer2 = {
        createRenderer: jasmine.createSpy('createRenderer'),
        createElement: jasmine.createSpy('createElement'),
        setAttribute: jasmine.createSpy('setAttribute'),
        appendChild: jasmine.createSpy('appendChild')
    } as unknown as Renderer2;

    beforeEach(waitForAsync(() => {
        return TestBed.configureTestingModule({
          providers: [
            MockProvider(RendererFactory2, {
                createRenderer: () => renderer2
            }),
            { provide: Document, useExisting: DOCUMENT },
          ]
        });
    }));

    beforeEach(() => {
        service = new LinkHeadService(TestBed.inject(RendererFactory2), TestBed.inject(DOCUMENT));
    });

    describe('link', () => {
        it('should create a link tag', () => {
            const link = service.addTag({
                href: 'test',
                type: 'application/atom+xml',
                rel: 'alternate',
                title: 'Sitewide Atom feed'
              });
            expect(link).not.toBeUndefined();
        });
    });

});
