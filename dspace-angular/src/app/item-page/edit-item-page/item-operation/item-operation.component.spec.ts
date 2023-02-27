import { ItemOperation } from './itemOperation.model';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { ItemOperationComponent } from './item-operation.component';
import { TranslateModule } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

describe('ItemOperationComponent', () => {
  let itemOperation: ItemOperation;

  let fixture;
  let comp;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      declarations: [ItemOperationComponent]
    }).compileComponents();
  }));

  beforeEach(() => {
    itemOperation = new ItemOperation('key1', 'url1');

    fixture = TestBed.createComponent(ItemOperationComponent);
    comp = fixture.componentInstance;
    comp.operation = itemOperation;
    fixture.detectChanges();
  });

  it('should render operation row', () => {
    const span = fixture.debugElement.query(By.css('.action-label span')).nativeElement;
    expect(span.textContent).toContain('item.edit.tabs.status.buttons.key1.label');
    const button = fixture.debugElement.query(By.css('button')).nativeElement;
    expect(button.textContent).toContain('item.edit.tabs.status.buttons.key1.button');
  });
  it('should render disabled operation row', () => {
    itemOperation.setDisabled(true);
    fixture.detectChanges();

    const span = fixture.debugElement.query(By.css('.action-label span')).nativeElement;
    expect(span.textContent).toContain('item.edit.tabs.status.buttons.key1.label');
    const button = fixture.debugElement.query(By.css('button')).nativeElement;
    expect(button.disabled).toBeTrue();
    expect(button.textContent).toContain('item.edit.tabs.status.buttons.key1.button');
  });
});
