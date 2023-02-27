import { MarkdownPipe } from './markdown.pipe';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '../../../config/app-config.interface';
import { environment } from '../../../environments/environment';

describe('Markdown Pipe', () => {

  let markdownPipe: MarkdownPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MarkdownPipe,
        {
          provide: APP_CONFIG,
          useValue: Object.assign(environment, {
            markdown: {
              enabled: true,
              mathjax: true,
            }
          })
        },
      ],
    }).compileComponents();

    markdownPipe = TestBed.inject(MarkdownPipe);
  });

  it('should render markdown', async () => {
    await testTransform(
      '# Header',
      '<h1>Header</h1>'
    );
  });

  it('should render mathjax', async () => {
    await testTransform(
      '$\\sqrt{2}^2$',
      '<svg.*?>.*</svg>'
    );
  });

  it('should render regular links', async () => {
    await testTransform(
      '<a href="https://www.dspace.com">DSpace</a>',
      '<a href="https://www.dspace.com">DSpace</a>'
    );
  });

  it('should not render javascript links', async () => {
    await testTransform(
      '<a href="javascript:window.alert(\'bingo!\');">exploit</a>',
      '<a>exploit</a>'
    );
  });

  async function testTransform(input: string, output: string) {
    expect(
      await markdownPipe.transform(input)
    ).toMatch(
      new RegExp('.*' + output + '.*')
    );
  }
});
