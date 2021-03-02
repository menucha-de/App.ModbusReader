package havis.custom.harting.modbus.reader;

public class ModbusReaderException extends Exception {

	private static final long serialVersionUID = 1L;

	public ModbusReaderException(String message) {
		super(message);
	}

	public ModbusReaderException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModbusReaderException(Throwable cause) {
		super(cause);
	}
}