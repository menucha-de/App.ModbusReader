import { Component, OnInit, Inject } from '@angular/core';
@Component({
  selector: 'app-main',
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.css']
})
export class MainComponent implements OnInit {
  constructor(
    @Inject('SHOW_FRAME') private _showFrame: boolean,
    @Inject('API_ENDPOINT') public apiEndpoint: string) { }

  ngOnInit() {
  }

  get showFrame() {
    return this._showFrame;
  }
}
