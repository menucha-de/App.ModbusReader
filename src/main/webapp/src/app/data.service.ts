import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RuntimeConfiguration, RuntimeConfigurationIntf } from './models/runtime-configuration';
import { BroadcasterService } from 'mica-shared';
import { RuntimeRegisterItem } from './models/runtime-register-item';
import { throwError, Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { DeviceInfo } from './models/deviceinfo';


@Injectable({
  providedIn: 'root'
})
export class DataService {

  private readonly baseUrl = 'rest/app/modbusreader/';
  constructor(
    private http: HttpClient,
    private broadcaster: BroadcasterService
  ) { }

  getDeviceInfo() {
    return this.http.get<DeviceInfo>(this.baseUrl + 'device/info').pipe(catchError(err => this.handleError(err)));
  }
  getRuntimeConfig(): Observable<RuntimeConfiguration> {
    return this.http.get<RuntimeConfigurationIntf>(this.baseUrl + 'runtime/configuration').pipe(
      map(conf => new RuntimeConfiguration(conf)),
      catchError(err => this.handleError(err))
    );
  }
  putRuntimeConfig(item: RuntimeConfigurationIntf) {
    const obj: RuntimeConfigurationIntf = {
      customOperationMaxLength: item.customOperationMaxLength,
      epcLength: item.epcLength,
      includeAccessPwd: item.includeAccessPwd,
      includeCRC: item.includeCRC,
      includeKillPwd: item.includeKillPwd,
      includePC: item.includePC,
      includeXPC: item.includeXPC,
      memorySelector: item.memorySelector,
      selectionMaskCount: item.selectionMaskCount,
      selectionMaskMaxLength: item.selectionMaskMaxLength,
      tagsInField: item.tagsInField,
      tidLength: item.tidLength,
      userLength: item.userLength
    };
    return this.http.put(this.baseUrl + 'runtime/configuration', obj)
    .pipe(catchError(err => this.handleError(err)));
  }
  getConfiguration() {
    return this.http.get<RuntimeRegisterItem[]>(this.baseUrl + 'runtime').pipe(catchError(err => this.handleError(err)));
  }
  exportRuntime() {
    return this.http.get(this.baseUrl + 'runtime/export', { responseType: 'text' })
    .pipe(catchError(err => this.handleError(err)));
  }

  private handleError(error: HttpErrorResponse) {
    this.broadcaster.broadcast('message' , new Map<string, string>([['messageType', 'error'], ['message', error.error]]));
    return throwError(error);
  }

}
