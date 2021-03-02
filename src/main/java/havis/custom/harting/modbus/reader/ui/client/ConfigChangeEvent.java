package havis.custom.harting.modbus.reader.ui.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

public class ConfigChangeEvent extends GwtEvent<ConfigChangeEvent.Handler> {
	public interface Handler extends EventHandler {
		void onConfigChangeEvent(ConfigChangeEvent event);
	}

	public interface HasHandlers {
		HandlerRegistration addConfigChangeEventHandler(ConfigChangeEvent.Handler handler);
	}

	private static final Type<ConfigChangeEvent.Handler> TYPE = new Type<>();

	public ConfigChangeEvent() {
	}
	
	@Override
	public Type<Handler> getAssociatedType() {
		return TYPE;
	}

	public static Type<Handler> getType() {
		return TYPE;
	}

	@Override
	protected void dispatch(Handler handler) {
		handler.onConfigChangeEvent(this);
	}

}
