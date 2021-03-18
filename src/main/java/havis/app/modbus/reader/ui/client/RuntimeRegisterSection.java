package havis.app.modbus.reader.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import havis.net.ui.shared.client.ConfigurationSection;

public class RuntimeRegisterSection extends ConfigurationSection {

	private static final String EXPORT_PATH = "rest/app/modbusreader/runtime/export";

	private static RuntimeRegisterSectionUiBinder uiBinder = GWT
			.create(RuntimeRegisterSectionUiBinder.class);

	interface RuntimeRegisterSectionUiBinder extends
			UiBinder<Widget, RuntimeRegisterSection> {
	}
	
	@UiField
	FlexTable regTable;
	
	@UiField
	Button export;
	
	@UiConstructor
	public RuntimeRegisterSection(String name) {
		super(name);
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	@UiHandler("export")
	public void onExport(ClickEvent e) {
		Window.Location.assign(GWT.getHostPageBaseURL() + EXPORT_PATH);
	}
	
	public void initializeTable() {
		regTable.removeAllRows();
		addTableHeader();
	}
	
	private void addTableHeader() {
		RegisterTableRow row = new RegisterTableRow();
		Widget[] widgets = row.getWidgets();
		for(int i = 0; i < widgets.length; i++) {
			regTable.setWidget(0, i, widgets[i]);
		}
	}
	
	public void addTableRow(RegisterTableRow row) {
		Widget[] widgets = row.getWidgets();
		int index = regTable.getRowCount();
		for(int i = 0; i < widgets.length; i++) {
			regTable.setWidget(index, i, widgets[i]);
		}
	}
}
