import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { of as observableOf } from 'rxjs';
import { EditCommunityPageComponent } from './edit-community-page.component';
import { CommunityDataService } from '../../core/data/community-data.service';

describe('EditCommunityPageComponent', () => {
  let comp: EditCommunityPageComponent;
  let fixture: ComponentFixture<EditCommunityPageComponent>;

  const routeStub = {
    data: observableOf({
      dso: { payload: {} }
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

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), SharedModule, CommonModule, RouterTestingModule],
      declarations: [EditCommunityPageComponent],
      providers: [
        { provide: CommunityDataService, useValue: {} },
        { provide: ActivatedRoute, useValue: routeStub },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EditCommunityPageComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('type', () => {
    it('should have the right type set', () => {
      expect((comp as any).type).toEqual('community');
    });
  });
});
