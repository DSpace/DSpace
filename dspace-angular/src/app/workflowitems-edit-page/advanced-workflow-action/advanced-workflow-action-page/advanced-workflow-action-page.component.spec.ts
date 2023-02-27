import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdvancedWorkflowActionPageComponent } from './advanced-workflow-action-page.component';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

describe('AdvancedWorkflowActionPageComponent', () => {
  let component: AdvancedWorkflowActionPageComponent;
  let fixture: ComponentFixture<AdvancedWorkflowActionPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
      ],
      declarations: [
        AdvancedWorkflowActionPageComponent,
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: {
                workflow: 'testaction',
              },
            },
          },
        },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AdvancedWorkflowActionPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
