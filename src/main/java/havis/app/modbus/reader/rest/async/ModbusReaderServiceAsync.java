package havis.app.modbus.reader.rest.async;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;

import havis.app.modbus.reader.rest.data.DeviceInfo;
import havis.app.modbus.reader.rest.data.RuntimeConfiguration;
import havis.app.modbus.reader.rest.data.RuntimeRegisterItem;

@Path("../rest/app/modbusreader")
public interface ModbusReaderServiceAsync extends RestService {

	@GET
	@Path("device/info")
	void getDeviceInfo(MethodCallback<DeviceInfo> callback);
	
	@GET
	@Path("runtime/configuration")
	void getRuntimeConfiguration(MethodCallback<RuntimeConfiguration> callback);
	
	@PUT
	@Path("runtime/configuration")
	void setRuntimeConfiguration(RuntimeConfiguration runtimeConfiguration, MethodCallback<Void> callback);
	
	@GET
	@Path("runtime")
	void getRuntime(MethodCallback<List<RuntimeRegisterItem>> callback);
}