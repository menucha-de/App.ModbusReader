package havis.app.modbus.reader.rest.provider;

import havis.app.modbus.reader.ModbusReaderException;
import havis.net.rest.shared.data.SerializableValue;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ModbusReaderExceptionMapper implements ExceptionMapper<ModbusReaderException> {

	@Override
	public Response toResponse(ModbusReaderException e) {
		return Response.status(Response.Status.BAD_REQUEST).entity(new SerializableValue<String>(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
	}
}