import { Component } from '@angular/core';
import { FooterComponent as BaseComponent } from '../../../../app/footer/footer.component';

@Component({
  selector: 'ds-footer',
  // styleUrls: ['footer.component.scss'],
  styleUrls: ['../../../../app/footer/footer.component.scss'],
  // templateUrl: 'footer.component.html'
  templateUrl: '../../../../app/footer/footer.component.html'
})
export class FooterComponent extends BaseComponent {
}
