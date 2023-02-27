import { animate, state, transition, trigger, style } from '@angular/animations';

export const focusShadow = trigger('focusShadow', [

  state('focus', style({ boxShadow: 'rgba(119, 119, 119, 0.6) 0px 0px 6px' })),

  state('blur', style({ boxShadow: 'none' })),

  transition('focus <=> blur', [animate('250ms')])
]);

export const focusBackground = trigger('focusBackground', [

  state('focus', style({ backgroundColor: 'rgba(119, 119, 119, 0.1)' })),

  state('blur', style({ backgroundColor: 'transparent' })),

  transition('focus <=> blur', [animate('250ms')])
]);
