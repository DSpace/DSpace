import { Inject, InjectionToken, Pipe, PipeTransform, SecurityContext } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { environment } from '../../../environments/environment';

const markdownItLoader = async () => (await import('markdown-it')).default;
type LazyMarkdownIt = ReturnType<typeof markdownItLoader>;
const MARKDOWN_IT = new InjectionToken<LazyMarkdownIt>(
  'Lazily loaded MarkdownIt',
  { providedIn: 'root', factory: markdownItLoader }
);

const mathjaxLoader = async () => (await import('markdown-it-mathjax3')).default;
type Mathjax = ReturnType<typeof mathjaxLoader>;
const MATHJAX = new InjectionToken<Mathjax>(
  'Lazily loaded mathjax',
  { providedIn: 'root', factory: mathjaxLoader }
);

const sanitizeHtmlLoader = async () => (await import('sanitize-html') as any).default;
type SanitizeHtml = ReturnType<typeof sanitizeHtmlLoader>;
const SANITIZE_HTML = new InjectionToken<SanitizeHtml>(
  'Lazily loaded sanitize-html',
  { providedIn: 'root', factory: sanitizeHtmlLoader }
);

/**
 * Pipe for rendering markdown and mathjax.
 * - markdown will only be rendered if {@link MarkdownConfig#enabled} is true
 * - mathjax will only be rendered if both {@link MarkdownConfig#enabled} and {@link MarkdownConfig#mathjax} are true
 *
 * This pipe should be used on the 'innerHTML' attribute of a component, in combination with an async pipe.
 * Example usage:
 *   <span class="example" [innerHTML]="'# title' | dsMarkdown | async"></span>
 * Result:
 *   <span class="example">
 *     <h1>title</h1>
 *   </span>
 */
@Pipe({
  name: 'dsMarkdown'
})
export class MarkdownPipe implements PipeTransform {

  constructor(
    protected sanitizer: DomSanitizer,
    @Inject(MARKDOWN_IT) private markdownIt: LazyMarkdownIt,
    @Inject(MATHJAX) private mathjax: Mathjax,
    @Inject(SANITIZE_HTML) private sanitizeHtml: SanitizeHtml,
  ) {
  }

  async transform(value: string): Promise<SafeHtml> {
    if (!environment.markdown.enabled) {
      return value;
    }
    const MarkdownIt = await this.markdownIt;
    const md = new MarkdownIt({
      html: true,
      linkify: true,
    });

    let html: string;
    if (environment.markdown.mathjax) {
      md.use(await this.mathjax);
      const sanitizeHtml = await this.sanitizeHtml;
      html = sanitizeHtml(md.render(value), {
        // sanitize-html doesn't let through SVG by default, so we extend its allowlists to cover MathJax SVG
        allowedTags: [
          ...sanitizeHtml.defaults.allowedTags,
          'mjx-container', 'svg', 'g', 'path', 'rect', 'text'
        ],
        allowedAttributes: {
          ...sanitizeHtml.defaults.allowedAttributes,
          'mjx-container': [
            'class', 'style', 'jax'
          ],
          svg: [
            'xmlns', 'viewBox', 'style', 'width', 'height', 'role', 'focusable', 'alt', 'aria-label'
          ],
          g: [
            'data-mml-node', 'style', 'stroke', 'fill', 'stroke-width', 'transform'
          ],
          path: [
            'd', 'style', 'transform'
          ],
          rect: [
            'width', 'height', 'x', 'y', 'transform', 'style'
          ],
          text: [
            'transform', 'font-size'
          ]
        },
        parser: {
          lowerCaseAttributeNames: false,
        },
      });
    } else {
      html = this.sanitizer.sanitize(SecurityContext.HTML, md.render(value));
    }

    return this.sanitizer.bypassSecurityTrustHtml(html);
  }
}
