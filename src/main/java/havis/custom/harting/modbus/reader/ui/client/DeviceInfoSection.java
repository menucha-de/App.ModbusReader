package havis.custom.harting.modbus.reader.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import havis.custom.harting.modbus.reader.rest.data.DeviceInfo;
import havis.net.ui.shared.client.ConfigurationSection;

public class DeviceInfoSection extends ConfigurationSection implements Editor<DeviceInfo> {

	private static DeviceInfoSectionUiBinder uiBinder = GWT
			.create(DeviceInfoSectionUiBinder.class);

	interface DeviceInfoSectionUiBinder extends
			UiBinder<Widget, DeviceInfoSection> {
	}
	
	@UiField
	TextBox vendorName;
	
	@UiField
	TextBox productCode;
	
	@UiField
	TextBox majorMinorRevision;
	
	@UiField
	TextBox serialNumber;
	
	@UiField
	TextBox hardwareRevision;
	
	@UiField
	TextBox baseFirmware;

	@UiConstructor
	public DeviceInfoSection(String name) {
		super(name);
		initWidget(uiBinder.createAndBindUi(this));
	}
}
