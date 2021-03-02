import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { MainComponent } from './main/main.component';

import { MicaSharedModule } from 'mica-shared';
import { DeviceInfoComponent } from './device-info/device-info.component';
import { RuntimeConfigComponent } from './runtime-config/runtime-config.component';
import { RuntimeRegisterComponent } from './runtime-register/runtime-register.component';
import { RuntimeConfigRowComponent } from './runtime-config-row/runtime-config-row.component';
import { IncludeRowComponent } from './include-row/include-row.component';

@NgModule({
  declarations: [
    AppComponent,
    MainComponent,
    DeviceInfoComponent,
    RuntimeConfigComponent,
    RuntimeRegisterComponent,
    RuntimeConfigRowComponent,
    IncludeRowComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    AppRoutingModule,
    MicaSharedModule
  ],
  providers: [
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
