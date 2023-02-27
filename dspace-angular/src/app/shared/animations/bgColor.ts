import {
  animate,
  animateChild,
  group, query,
  state,
  style,
  transition,
  trigger
} from '@angular/animations';

const startStyle = style({ backgroundColor: '{{ startColor }}' });
const endStyle = style({ backgroundColor: '{{ endColor }}' });

export const bgColor = trigger('bgColor',
  [
    state('startBackground', startStyle, { params: { startColor: '*' } }),
    state('endBackground', endStyle, { params: { endColor: '*' } }),
    transition('startBackground <=> endBackground',
      group(
        [
          query('@*', animateChild()),
          animate('200ms'),

        ]
      ))
  ]);
