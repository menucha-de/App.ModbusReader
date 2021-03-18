package havis.app.modbus.reader.ui.client;

import havis.app.modbus.reader.rest.async.ModbusReaderServiceAsync;
import havis.app.modbus.reader.rest.data.DeviceInfo;
import havis.app.modbus.reader.rest.data.RuntimeConfiguration;
import havis.app.modbus.reader.rest.data.RuntimeRegisterItem;
import havis.app.modbus.reader.ui.res.AppResources;
import havis.app.modbus.reader.ui.res.cons.AppConstants;
import havis.net.ui.shared.client.event.MessageEvent.MessageType;
import havis.net.ui.shared.client.widgets.CustomMessageWidget;
import havis.net.ui.shared.resourcebundle.ResourceBundle;

import java.util.List;

import org.fusesource.restygwt.client.Defaults;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class WebUI extends Composite implements EntryPoint {

	@UiField
	FlowPanel container;

	@UiField
	DeviceInfoSection deviceInfo;

	@UiField
	RuntimeConfigSection runtimeConfig;

	@UiField
	RuntimeRegisterSection runtimeReg;
	
	private ResourceBundle shared = ResourceBundle.INSTANCE;
	private AppResources res = AppResources.INSTANCE;
	private AppConstants con = AppConstants.INSTANCE;
	
	// Runtime Config
	interface RuntimeConfigDriver extends SimpleBeanEditorDriver<RuntimeConfiguration, RuntimeConfigSection> {}
	RuntimeConfigDriver runtimeConfigDriver = GWT.create(RuntimeConfigDriver.class);

	// DeviceInfo
	interface DeviceInfoDriver extends SimpleBeanEditorDriver<DeviceInfo, DeviceInfoSection> {}
	DeviceInfoDriver deviceInfoDriver = GWT.create(DeviceInfoDriver.class);
	
	ModbusReaderServiceAsync service = GWT.create(ModbusReaderServiceAsync.class);

	private RuntimeConfiguration runtimeConfiguration;

	private static WebUIUiBinder uiBinder = GWT.create(WebUIUiBinder.class);

	@UiTemplate("WebUI.ui.xml")
	interface WebUIUiBinder extends UiBinder<Widget, WebUI> {
	}

	public WebUI() {
		initWidget(uiBinder.createAndBindUi(this));
		Defaults.setDateFormat(null);
		deviceInfoDriver.initialize(deviceInfo);
		runtimeConfigDriver.initialize(runtimeConfig);
		ensureInjection();
	}

	@Override
	public void onModuleLoad() {
		RootLayoutPanel.get().add(this);
	}

	private void ensureInjection() {
		shared.css().ensureInjected();
		res.css().ensureInjected();
	}

	@UiHandler("runtimeConfig")
	void onToggleRuntimeConfig(ValueChangeEvent<Boolean> event) {
		if (event.getValue()) {
			service.getRuntimeConfiguration(new MethodCallback<RuntimeConfiguration>() {

				@Override
				public void onSuccess(Method method, RuntimeConfiguration response) {
					runtimeConfiguration = response;
					runtimeConfigDriver.edit(runtimeConfiguration);
				}

				@Override
				public void onFailure(Method method, Throwable exception) {
					CustomMessageWidget.show(con.failRuntimeConfig(), MessageType.ERROR);
					runtimeConfig.setOpen(false);
				}
			});
		}
	}
	
	@UiHandler("deviceInfo")
	void onToggleDeviceInfo(ValueChangeEvent<Boolean> event) {
		if (event.getValue()) {
			service.getDeviceInfo(new MethodCallback<DeviceInfo>() {
				
				@Override
				public void onSuccess(Method method, DeviceInfo response) {
					response.setHardwareRevision("2.0");
					deviceInfoDriver.edit(response);
				}
				
				@Override
				public void onFailure(Method method, Throwable exception) {
					CustomMessageWidget.show(con.failDeviceInfo(), MessageType.ERROR);
					deviceInfo.setOpen(false);
				}
			});
		}
	}
	
	@UiHandler("runtimeReg")
	void onToggleRuntimeRegister(ValueChangeEvent<Boolean> event) {
		if(event.getValue()) {
			service.getRuntime(new MethodCallback<List<RuntimeRegisterItem>>() {
				@Override
				public void onSuccess(Method method, List<RuntimeRegisterItem> response) {
					runtimeReg.initializeTable();
					for(RuntimeRegisterItem i : response) {
						RegisterTableRow row = new RegisterTableRow(i);
						runtimeReg.addTableRow(row);
					}
				}

				@Override
				public void onFailure(Method method, Throwable exception) {
					CustomMessageWidget.show(con.failRuntimeRegister(), MessageType.ERROR);
					runtimeReg.setOpen(false);
				}
			});
		}
	}
	
	@UiHandler("runtimeConfig")
	void onConfigChangedEvent(ConfigChangeEvent event) {
		runtimeConfigDriver.flush();
		service.setRuntimeConfiguration(runtimeConfiguration, new MethodCallback<Void>() {
			
			@Override
			public void onSuccess(Method method, Void response) {
			}
			
			@Override
			public void onFailure(Method method, Throwable exception) {
				CustomMessageWidget.show(con.failRuntimeChange(), MessageType.ERROR);
			}
		});
	}
}