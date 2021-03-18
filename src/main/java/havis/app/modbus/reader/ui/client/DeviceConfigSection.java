package havis.app.modbus.reader.ui.client;

import havis.net.ui.shared.client.ConfigurationSection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Widget;

public class DeviceConfigSection extends ConfigurationSection {

	private static DeviceConfigSectionUiBinder uiBinder = GWT
			.create(DeviceConfigSectionUiBinder.class);

	interface DeviceConfigSectionUiBinder extends
			UiBinder<Widget, DeviceConfigSection> {
	}
	
	@UiConstructor
	public DeviceConfigSection(String name) {
		super(name);
		initWidget(uiBinder.createAndBindUi(this));
	}

}
