import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { Community } from '../../../core/shared/community.model';
import { TranslateModule } from '@ngx-translate/core';
import { SearchResultsComponent } from './search-results.component';
import { QueryParamsDirectiveStub } from '../../testing/query-params-directive.stub';
import { createFailedRemoteDataObject } from '../../remote-data.utils';

describe('SearchResultsComponent', () => {
  let comp: SearchResultsComponent;
  let fixture: ComponentFixture<SearchResultsComponent>;
  let heading: DebugElement;
  let title: DebugElement;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
      declarations: [
        SearchResultsComponent,
        QueryParamsDirectiveStub],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchResultsComponent);
    comp = fixture.componentInstance; // SearchFormComponent test instance
    heading = fixture.debugElement.query(By.css('heading'));
    title = fixture.debugElement.query(By.css('h2'));
  });

  it('should display results when results are not empty', () => {
    (comp as any).searchResults = { hasSucceeded: true, isLoading: false, payload: { page: { length: 2 } } };
    (comp as any).searchConfig = {};
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('ds-viewable-collection'))).not.toBeNull();
  });

  it('should not display link when results are not empty', () => {
    (comp as any).searchResults = { hasSucceeded: true, isLoading: false, payload: { page: { length: 2 } } };
    (comp as any).searchConfig = {};
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('a'))).toBeNull();
  });

  it('should display error message if error is 500', () => {
    (comp as any).searchResults = createFailedRemoteDataObject('Error', 500);
    fixture.detectChanges();
    expect(comp.showError()).toBeTrue();
    expect(comp.errorMessageLabel()).toBe('error.search-results');
    expect(fixture.debugElement.query(By.css('ds-error'))).not.toBeNull();
  });

  it('should display error message if error is 422', () => {
    (comp as any).searchResults = createFailedRemoteDataObject('Error', 422);
    fixture.detectChanges();
    expect(comp.showError()).toBeTrue();
    expect(comp.errorMessageLabel()).toBe('error.invalid-search-query');
    expect(fixture.debugElement.query(By.css('ds-error'))).not.toBeNull();
  });

  it('should display link with new search where query is quoted if search return a error 400', () => {
    (comp as any).searchResults = createFailedRemoteDataObject('Error', 400);
    (comp as any).searchConfig = { query: 'foobar' };
    fixture.detectChanges();

    const linkDes = fixture.debugElement.queryAll(By.directive(QueryParamsDirectiveStub));

    // get attached link directive instances
    // using each DebugElement's injector
    const routerLinkQuery = linkDes.map((de) => de.injector.get(QueryParamsDirectiveStub));

    expect(routerLinkQuery.length).toBe(1, 'should have 1 router link with query params');
    expect(routerLinkQuery[0].queryParams.query).toBe('"foobar"', 'query params should be "foobar"');
  });

  it('should display link with new search where query is quoted if search result is empty', () => {
    (comp as any).searchResults = { payload: { page: { length: 0 } } };
    (comp as any).searchConfig = { query: 'foobar' };
    fixture.detectChanges();

    const linkDes = fixture.debugElement.queryAll(By.directive(QueryParamsDirectiveStub));

    // get attached link directive instances
    // using each DebugElement's injector
    const routerLinkQuery = linkDes.map((de) => de.injector.get(QueryParamsDirectiveStub));

    expect(routerLinkQuery.length).toBe(1, 'should have 1 router link with query params');
    expect(routerLinkQuery[0].queryParams.query).toBe('"foobar"', 'query params should be "foobar"');
  });

  it('should add quotes around the given string', () => {
    expect(comp.surroundStringWithQuotes('teststring')).toEqual('"teststring"');
  });

  it('should not add quotes around the given string if they are already there', () => {
    expect(comp.surroundStringWithQuotes('"teststring"')).toEqual('"teststring"');
  });

  it('should not add quotes around a given empty string', () => {
    expect(comp.surroundStringWithQuotes('')).toEqual('');
  });
});

export const objects = [
  Object.assign(new Community(), {
    logo: {
      self: {
        _isScalar: true,
        value: 'https://dspace7.4science.it/dspace-spring-rest/api/core/bitstreams/10b636d0-7890-4968-bcd6-0d83bf4e2b42',
        scheduler: null
      }
    },
    collections: {
      self: {
        _isScalar: true,
        value: '1506937433727',
        scheduler: null
      }
    },
    _links: {
      self: {
        href: 'https://dspace7.4science.it/dspace-spring-rest/api/core/communities/7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
      },
    },
    id: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
    uuid: '7669c72a-3f2a-451f-a3b9-9210e7a4c02f',
    type: Community.type,
    metadata: {
      'dc.description': [
        {
          language: null,
          value: ''
        }
      ],
      'dc.description.abstract': [
        {
          language: null,
          value: 'This is a test community to hold content for the OR2017 demostration'
        }
      ],
      'dc.description.tableofcontents': [
        {
          language: null,
          value: ''
        }
      ],
      'dc.rights': [
        {
          language: null,
          value: ''
        }
      ],
      'dc.title': [
        {
          language: null,
          value: 'OR2017 - Demonstration'
        }
      ],
      'dc.identifier.uri': [
        {
          language: null,
          value: 'http://localhost:4000/handle/10673/11'
        }
      ]
    }
  }),
  Object.assign(new Community(),
    {
      logo: {
        self: {
          _isScalar: true,
          value: 'https://dspace7.4science.it/dspace-spring-rest/api/core/bitstreams/f446c17d-6d51-45ea-a610-d58a73642d40',
          scheduler: null
        }
      },
      collections: {
        self: {
          _isScalar: true,
          value: '1506937433727',
          scheduler: null
        }
      },
      _links: {
        self: {
          href: 'https://dspace7.4science.it/dspace-spring-rest/api/core/communities/9076bd16-e69a-48d6-9e41-0238cb40d863',
        },
      },
      id: '9076bd16-e69a-48d6-9e41-0238cb40d863',
      uuid: '9076bd16-e69a-48d6-9e41-0238cb40d863',
      type: Community.type,
      metadata: {
        'dc.description': [
          {
            language: null,
            value: '<p>This is the introductory text for the <em>Sample Community</em> on the DSpace Demonstration Site. It is editable by System or Community Administrators (of this Community).</p>\r\n<p><strong>DSpace Communities may contain one or more Sub-Communities or Collections (of Items).</strong></p>\r\n<p>This particular Community has its own logo (the <a href=\'http://www.duraspace.org/\'>DuraSpace</a> logo).</p>'
          }
        ],
        'dc.description.abstract': [
          {
            language: null,
            value: 'This is a sample top-level community'
          }
        ],
        'dc.description.tableofcontents': [
          {
            language: null,
            value: '<p>This is the <em>news section</em> for this <em>Sample Community</em>. System or Community Administrators (of this Community) can edit this News field.</p>'
          }
        ],
        'dc.rights': [
          {
            language: null,
            value: '<p><em>If this Community had special copyright text to display, it would be displayed here.</em></p>'
          }
        ],
        'dc.title': [
          {
            language: null,
            value: 'Sample Community'
          }
        ],
        'dc.identifier.uri': [
          {
            language: null,
            value: 'http://localhost:4000/handle/10673/1'
          }
        ]
      }
    }
  )
];
