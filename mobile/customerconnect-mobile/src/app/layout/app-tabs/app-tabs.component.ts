import { ChangeDetectionStrategy, Component } from '@angular/core';
import { addIcons } from 'ionicons';
import {
  homeOutline,
  peopleOutline,
  briefcaseOutline,
  sparklesOutline,
  personCircleOutline,
} from 'ionicons/icons';
import {
  IonIcon,
  IonLabel,
  IonRouterOutlet,
  IonTabBar,
  IonTabButton,
  IonTabs,
} from '@ionic/angular/standalone';

@Component({
  selector: 'app-tabs',
  imports: [IonTabs, IonRouterOutlet, IonTabBar, IonTabButton, IonIcon, IonLabel],
  templateUrl: './app-tabs.component.html',
  styleUrl: './app-tabs.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppTabsComponent {
  constructor() {
    addIcons({
      homeOutline,
      peopleOutline,
      briefcaseOutline,
      sparklesOutline,
      personCircleOutline,
    });
  }
}
