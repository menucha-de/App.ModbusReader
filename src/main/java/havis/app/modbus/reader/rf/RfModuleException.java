package havis.app.modbus.reader.rf;

import havis.app.modbus.reader.api.ModuleException;
import havis.app.modbus.reader.rf.RfConstants.RfErrorCode;

public class RfModuleException extends ModuleException {

	private static final long serialVersionUID = -7749286833455203876L;
	private RfErrorCode errorCode;

	public RfModuleException(String message, RfConstants.RfErrorCode errorCode, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public RfModuleException(String message, RfConstants.RfErrorCode errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public RfErrorCode getErrorCode() {
		return errorCode;
	}

	@Override
	public String toString() {
		return "RfModuleException [errorCode=" + errorCode + ", super=" + super.toString() + "]";
	}
}