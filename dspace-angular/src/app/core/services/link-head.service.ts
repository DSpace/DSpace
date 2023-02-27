import { Injectable, RendererFactory2, ViewEncapsulation, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';

/**
 * LinkHead Service injects <link> tag into the head element during runtime.
 */
@Injectable()
export class LinkHeadService {
  constructor(
    private rendererFactory: RendererFactory2,
    @Inject(DOCUMENT) private document
  ) {

  }

  /**
   * Method to create a Link tag in the HEAD of the html.
   * @param tag LinkDefition is the paramaters to define a link tag.
   * @returns Link tag that was created
   */
  addTag(tag: LinkDefinition) {

    try {
      const renderer = this.rendererFactory.createRenderer(this.document, {
        id: '-1',
        encapsulation: ViewEncapsulation.None,
        styles: [],
        data: {}
      });

      const link = renderer.createElement('link');
      const head = this.document.head;

      if (head === null) {
        throw new Error('<head> not found within DOCUMENT.');
      }

      Object.keys(tag).forEach((prop: string) => {
        return renderer.setAttribute(link, prop, tag[prop]);
      });

      renderer.appendChild(head, link);
      return renderer;
    } catch (e) {
      console.error('Error within linkService : ', e);
    }
  }

  /**
   * Removes a link tag in header based on the given attrSelector.
   * @param attrSelector The attr assigned to a link tag which will be used to determine what link to remove.
   */
  removeTag(attrSelector: string) {
    if (attrSelector) {
      try {
        const renderer = this.rendererFactory.createRenderer(this.document, {
          id: '-1',
          encapsulation: ViewEncapsulation.None,
          styles: [],
          data: {}
        });
        const head = this.document.head;
        if (head === null) {
          throw new Error('<head> not found within DOCUMENT.');
        }
        const linkTags = this.document.querySelectorAll('link[' + attrSelector + ']');
        for (const link of linkTags) {
          renderer.removeChild(head, link);
        }
      } catch (e) {
        console.log('Error while removing tag ' + e.message);
      }
    }
  }
}

export declare type LinkDefinition = {
  charset?: string;
  crossorigin?: string;
  href?: string;
  hreflang?: string;
  media?: string;
  rel?: string;
  rev?: string;
  sizes?: string;
  target?: string;
  type?: string;
} & {
    [prop: string]: string;
  };
