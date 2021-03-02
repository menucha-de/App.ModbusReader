import { Component, OnInit } from '@angular/core';
import { DeviceInfo } from '../models/deviceinfo';
import { DataService } from '../data.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-device-info',
  templateUrl: './device-info.component.html',
  styleUrls: ['./device-info.component.scss']
})
export class DeviceInfoComponent implements OnInit {

  constructor(public data: DataService) { }
  info$: Observable<DeviceInfo>;
  ngOnInit() {
    this.info$ = this.data.getDeviceInfo();
  }
}
