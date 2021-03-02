import { Component, OnInit } from '@angular/core';
import { RuntimeRegisterItem } from '../models/runtime-register-item';
import { DataService } from '../data.service';
import { BroadcasterService } from 'mica-shared';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-runtime-register',
  templateUrl: './runtime-register.component.html',
  styleUrls: ['./runtime-register.component.scss']
})
export class RuntimeRegisterComponent implements OnInit {
  items$: Observable<RuntimeRegisterItem[]>;
  constructor(private data: DataService, private broadcaster: BroadcasterService) { }

  ngOnInit() {
    this.items$ = this.data.getConfiguration();
  }
}
