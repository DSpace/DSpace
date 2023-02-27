import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { Community } from '../../../../core/shared/community.model';
import { SharedModule } from '../../../shared.module';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { DSpaceObject } from '../../../../core/shared/dspace-object.model';
import { EditComColPageComponent } from './edit-comcol-page.component';

describe('EditComColPageComponent', () => {
  let comp: EditComColPageComponent<DSpaceObject>;
  let fixture: ComponentFixture<EditComColPageComponent<DSpaceObject>>;
  let router: Router;

  let community;
  let routerStub;
  let routeStub;

  function initializeVars() {
    community = Object.assign(new Community(), {
      uuid: 'a20da287-e174-466a-9926-f66b9300d347',
      metadata: [{
        key: 'dc.title',
        value: 'test community'
      }]
    });

    routerStub = {
      navigate: (commands) => commands,
      events: observableOf({}),
      url: 'mockUrl'
    };

    routeStub = {
      data: observableOf({
        dso: community
      }),
      routeConfig: {
        children: [
          {
            path: 'mockUrl',
            data: {
              hideReturnButton: false
            }
          }
        ]
      },
      snapshot: {
        firstChild: {
          routeConfig: {
            path: 'mockUrl'
          }
        }
      }
    };

  }

  beforeEach(waitForAsync(() => {
    initializeVars();
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), SharedModule, CommonModule, RouterTestingModule],
      providers: [
        { provide: Router, useValue: routerStub },
        { provide: ActivatedRoute, useValue: routeStub },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EditComColPageComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
    router = (comp as any).router;
  });

  describe('getPageUrl', () => {
    let url;
    beforeEach(() => {
      url = comp.getPageUrl(community);
    });
    it('should return the current url as a fallback', () => {
      expect(url).toEqual(routerStub.url);
    });
  });
});
