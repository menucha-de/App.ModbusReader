package havis.custom.harting.modbus.reader.api;

public class ModuleException extends Exception {

	private static final long serialVersionUID = -7616644322343256345L;

	public ModuleException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModuleException(String message) {
		super(message);
	}
}