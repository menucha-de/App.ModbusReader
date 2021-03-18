package havis.app.modbus.reader.core;

public class ModbusSlaveException extends Exception {

    private static final long serialVersionUID = -7616644322343256345L;

    public ModbusSlaveException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModbusSlaveException(String message) {
        super(message);
    }
}