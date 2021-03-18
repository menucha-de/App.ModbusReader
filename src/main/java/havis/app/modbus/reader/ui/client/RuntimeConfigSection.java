package havis.app.modbus.reader.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.user.client.ui.Widget;

import havis.app.modbus.reader.rest.data.RuntimeConfiguration;
import havis.net.ui.shared.client.ConfigurationSection;

public class RuntimeConfigSection extends ConfigurationSection
		implements ValueAwareEditor<RuntimeConfiguration>, ConfigChangeEvent.HasHandlers {

	private static RuntimeConfigSectionUiBinder uiBinder = GWT.create(RuntimeConfigSectionUiBinder.class);

	interface RuntimeConfigSectionUiBinder extends UiBinder<Widget, RuntimeConfigSection> {
	}

	@UiField(provided = true)
	ValueListBox<Integer> tagsInField = new ValueListBox<>();

	@UiField
	ToggleButton includePC;

	@UiField
	ToggleButton includeCRC;

	@UiField
	ToggleButton includeAccessPwd;

	@UiField
	ToggleButton includeKillPwd;

	@UiField
	RuntimeConfigRow epcLength;

	@UiField
	RuntimeConfigRow tidLength;

	@UiField
	RuntimeConfigRowLarge userLength;

	@UiField
	RuntimeConfigRow customOperationMaxLength;
	
	@UiField
	@Path("")
	RuntimeConfigRowQty selMasks;

	@UiConstructor
	public RuntimeConfigSection(String name) {
		super(name);
		initWidget(uiBinder.createAndBindUi(this));
	}

	@UiHandler({ "includePC", "includeCRC", "includeAccessPwd", "includeKillPwd", "epcLength", "tidLength",
			"userLength", "customOperationMaxLength", "selMasks" })
	void onConfigChanged(ValueChangeEvent<Boolean> event) {
		fireEvent(new ConfigChangeEvent());
	}
	
	@UiHandler("tagsInField")
	void onTagsInFieldChanged(ValueChangeEvent<Integer> event) {
		fireEvent(new ConfigChangeEvent());
	}

	private void fillListBoxes() {
		for (int i = 1; i < 256; i++) {
			tagsInField.setValue(i);
		}
	}

	@Override
	protected void onOpenSection() {
		super.onOpenSection();
	}

	@Override
	public HandlerRegistration addConfigChangeEventHandler(ConfigChangeEvent.Handler handler) {
		return addHandler(handler, ConfigChangeEvent.getType());
	}

	@Override
	public void setDelegate(EditorDelegate<RuntimeConfiguration> delegate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPropertyChange(String... paths) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setValue(RuntimeConfiguration value) {
		fillListBoxes();
	}
}
