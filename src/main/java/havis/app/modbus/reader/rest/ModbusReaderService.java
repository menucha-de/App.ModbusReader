package havis.app.modbus.reader.rest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import havis.app.modbus.reader.ModbusReaderConfiguration;
import havis.app.modbus.reader.ModbusReaderException;
import havis.app.modbus.reader.core.SlaveProcessor;
import havis.app.modbus.reader.rest.data.DeviceInfo;
import havis.app.modbus.reader.rest.data.RuntimeConfiguration;
import havis.app.modbus.reader.rest.data.RuntimeRegisterItem;

@Path("app/modbusreader")
public class ModbusReaderService {

	private ModbusReaderConfiguration config;

	public ModbusReaderService(SlaveProcessor slaveProcessor) {
		config = new ModbusReaderConfiguration(slaveProcessor);
	}

	@PermitAll
	@GET
	@Path("device/info")
	@Produces({ MediaType.APPLICATION_JSON })
	public DeviceInfo getDeviceInfo() throws ModbusReaderException {
		return config.getDeviceInfo();
	}

	@PermitAll
	@GET
	@Path("runtime/configuration")
	@Produces({ MediaType.APPLICATION_JSON })
	public RuntimeConfiguration getRuntimeConfiguration() throws ModbusReaderException {
		return config.getRuntimeConfiguration();
	}

	@RolesAllowed("admin")
	@PUT
	@Path("runtime/configuration")
	@Produces({ MediaType.APPLICATION_JSON })
	public void setRuntimeConfiguration(RuntimeConfiguration rc) throws ModbusReaderException {
		config.setRuntimeConfiguration(rc);
	}

	@PermitAll
	@GET
	@Path("runtime")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<RuntimeRegisterItem> getRuntime() throws ModbusReaderException {
		return config.getRuntime();
	}
	
	@PermitAll
	@GET
	@Path("runtime/export")
	@Produces({ MediaType.APPLICATION_OCTET_STREAM })
	public Response exportRuntime() throws ModbusReaderException {
		String result = config.getRuntimeExport();
		String filename = String.format("RuntimeRegister_%s.txt", new SimpleDateFormat("yyyyMMdd").format(new Date()));
		byte[] data = result.getBytes();
		return Response.ok(result, MediaType.APPLICATION_OCTET_STREAM)
				.header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
				.header("Content-Type", "text/plain; charset=utf-8").header("Content-Length", data.length).build();
	}
	
	
}