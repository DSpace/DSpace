import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { LinkMenuItemComponent } from './link-menu-item.component';
import { RouterLinkDirectiveStub } from '../../testing/router-link-directive.stub';
import { QueryParamsDirectiveStub } from '../../testing/query-params-directive.stub';
import { RouterStub } from '../../testing/router.stub';
import { Router } from '@angular/router';

describe('LinkMenuItemComponent', () => {
  let component: LinkMenuItemComponent;
  let fixture: ComponentFixture<LinkMenuItemComponent>;
  let debugElement: DebugElement;
  let text;
  let link;
  let queryParams;

  function init() {
    text = 'HELLO';
    link = '/world/hello';
    queryParams = {params: true};
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [LinkMenuItemComponent, RouterLinkDirectiveStub, QueryParamsDirectiveStub],
      providers: [
        { provide: 'itemModelProvider', useValue: { text: text, link: link, queryParams: queryParams } },
        { provide: Router, useValue: RouterStub },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LinkMenuItemComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should contain the correct text', () => {
    const textContent = debugElement.query(By.css('a')).nativeElement.textContent;
    expect(textContent).toEqual(text);
  });

  it('should have the right routerLink attribute', () => {
    const linkDes = fixture.debugElement.queryAll(By.directive(RouterLinkDirectiveStub));
    const routerLinkQuery = linkDes.map((de) => de.injector.get(RouterLinkDirectiveStub));

    expect(routerLinkQuery.length).toBe(1);
    expect(routerLinkQuery[0].routerLink).toBe(link);
  });

  it('should have the right queryParams attribute', () => {
    const queryDes = fixture.debugElement.queryAll(By.directive(QueryParamsDirectiveStub));
    const routerParamsQuery = queryDes.map((de) => de.injector.get(QueryParamsDirectiveStub));

    expect(routerParamsQuery.length).toBe(1);
    expect(routerParamsQuery[0].queryParams).toBe(queryParams);
  });
});
