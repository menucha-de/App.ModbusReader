import { Component, OnInit } from '@angular/core';
import { RuntimeConfiguration } from '../models/runtime-configuration';
import { DataService } from '../data.service';
import { BroadcasterService } from 'mica-shared';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-runtime-config',
  templateUrl: './runtime-config.component.html',
  styleUrls: ['./runtime-config.component.scss']
})
export class RuntimeConfigComponent implements OnInit {

  constructor(private data: DataService, private broadcaster: BroadcasterService) { }
  item$: Observable<RuntimeConfiguration>;
  tagsInField = Array(256).fill(1).map((x, i) => i + 1);
  qty = Array(4).fill(1).map((x, i) => i + 1);
  ngOnInit() {
    this.item$ = this.data.getRuntimeConfig();
  }
  update(item: RuntimeConfiguration) {
    this.data.putRuntimeConfig(item).subscribe();
  }

  log(item: RuntimeConfiguration) {
    console.log(item);
  }
}
