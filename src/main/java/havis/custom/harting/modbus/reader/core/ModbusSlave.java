package havis.custom.harting.modbus.reader.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import havis.custom.harting.modbus.reader.api.Module;
import havis.custom.harting.modbus.reader.common.FileHandler;
import havis.custom.harting.modbus.reader.common.NativeLibraryLoader;
import havis.custom.harting.modbus.reader.common.PathHandler;
import havis.util.modbus.IntArray;
import havis.util.modbus.ModbusBase;
import havis.util.modbus.ModbusMapping;
import havis.util.modbus.ModbusTcpPi;
import havis.util.modbus.UInt16Array;
import havis.util.modbus.UInt8Array;

public class ModbusSlave implements Slave {

	private static final Logger log = Logger.getLogger(ModbusSlave.class.getName());

	public enum Type {
		COILS, DISCRETE_INPUTS, HOLDING_REGISTERS, INPUT_REGISTERS
	}

	private final static String CONFIG_BASE_DIR = "havis-modbus-reader";
	private final static String STATE_BASE_DIR = "/var/lib/havis-modbus-reader";

	private String configBaseDir;
	private String stateBaseDir;
	private boolean createConfigCopy;
	private Module module;
	private String[] nativeLibraryNames;

	private int openCloseTimeout;
	private int maxConnectionCount;
	private SlaveProcessor slaveProcessor;
	private ModbusTcpPi ctx = null;
	private int serverSocket = -1;

	private Lock lock = new ReentrantLock();
	private Condition stopped = lock.newCondition();
	private int stopState;

	/**
	 * @param configBaseDir
	 *            Base path for configuration files (default:
	 *            havis-modbus-reader). A relative path starts at the class
	 *            path.
	 * @param stateBaseDir
	 *            Base path for state files (default:
	 *            /var/lib/havis-modbus-reader). A relative path starts at the
	 *            working directory.
	 * @param createConfigCopy
	 * @param module
	 * @param nativeLibraryNames
	 */
	public ModbusSlave(String configBaseDir, String stateBaseDir, boolean createConfigCopy, Module module,
			String[] nativeLibraryNames) {
		this.configBaseDir = (configBaseDir == null || configBaseDir.isEmpty()) ? CONFIG_BASE_DIR : configBaseDir;
		this.stateBaseDir = (stateBaseDir == null || stateBaseDir.isEmpty()) ? STATE_BASE_DIR : stateBaseDir;
		this.createConfigCopy = createConfigCopy;
		this.module = module;
		this.nativeLibraryNames = nativeLibraryNames;
	}

	public SlaveProcessor open() throws ModbusSlaveException {
		// check directories
		Path configBaseDirPath = new PathHandler().toAbsolutePath(Paths.get(configBaseDir));
		if (configBaseDirPath == null) {
			throw new ModbusSlaveException("Missing configuration directory: " + configBaseDirPath);
		}
		Path stateBaseDirPath = Paths.get(stateBaseDir).toAbsolutePath();
		if (!Files.isDirectory(stateBaseDirPath)) {
			throw new ModbusSlaveException("Missing directory for state files: " + stateBaseDirPath);
		}
		// load configuration properties
		Path configPropsFilePath = configBaseDirPath.resolve("config.properties");
		Path configCopyPropsFilePath = stateBaseDirPath.resolve("configCopy.properties");
		Path path = createConfigCopy && Files.isRegularFile(configCopyPropsFilePath) ? configCopyPropsFilePath
				: configPropsFilePath;
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Reading configuration values from " + path);
		}
		Properties configProps = new Properties();
		InputStream in = null;
		try {
			in = new FileHandler().newInputStream(path);
			configProps.load(in);
		} catch (Exception e) {
			throw new ModbusSlaveException("Cannot load configuration properties from " + path, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					throw new ModbusSlaveException("Cannot close configuration properties file " + path, e);
				}
			}
		}
		// if default config properties were loaded and a copy shall be created
		if (createConfigCopy && path.equals(configPropsFilePath)) {
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Writing configuration values to " + configCopyPropsFilePath);
			}
			OutputStream out = null;
			try {
				out = Files.newOutputStream(configCopyPropsFilePath);
				configProps.store(out, null /* comments */);
			} catch (Exception e) {
				throw new ModbusSlaveException("Cannot save configuration properties in " + configCopyPropsFilePath, e);
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
						throw new ModbusSlaveException(
								"Cannot close configuration properties file " + configCopyPropsFilePath, e);
					}
				}
			}
		}
		String port = configProps.getProperty("port");
		if (port == null || port.isEmpty()) {
			throw new ModbusSlaveException("Missing configuration property 'port'");
		}
		String openCloseTimeoutStr = configProps.getProperty("openCloseTimeout");
		if (openCloseTimeoutStr == null || openCloseTimeoutStr.isEmpty()) {
			throw new ModbusSlaveException("Missing configuration property 'openCloseTimeout'");
		}
		try {
			openCloseTimeout = Integer.parseInt(openCloseTimeoutStr);
		} catch (NumberFormatException e) {
			throw new ModbusSlaveException(
					"Cannot parse configuration property 'openCloseTimeout': " + openCloseTimeoutStr);
		}
		String maxConnectionCountStr = configProps.getProperty("maxConnectionCount");
		if (maxConnectionCountStr == null || maxConnectionCountStr.isEmpty()) {
			maxConnectionCountStr = "1";			
		}
		try {
			maxConnectionCount = Integer.parseInt(maxConnectionCountStr);
		} catch (NumberFormatException e) {
			throw new ModbusSlaveException(
					"Cannot parse configuration property 'maxConnectionCount': " + maxConnectionCountStr);
		}
		// create processor
		slaveProcessor = new ModbusSlaveProcessor(configBaseDirPath, stateBaseDirPath, openCloseTimeout, this, module);
		if (nativeLibraryNames != null) {
			NativeLibraryLoader loader = new NativeLibraryLoader();
			for (String nativeLibraryName : nativeLibraryNames) {
				loader.load(nativeLibraryName);
			}
		}
		ctx = new ModbusTcpPi();
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Opening server socket on port " + port);
		}
		if (ctx.newTcpPi("::0", port) < 0) {
			// delete class instance
			ctx.delete();
			ctx = null;
			slaveProcessor = null;
			throw new ModbusSlaveException("Unable to create a TCP context");
		}
		// set debug mode
		ctx.setDebug(log.isLoggable(Level.FINE));
		// open slave
		serverSocket = ctx.tcpPiListen(maxConnectionCount);
		if (serverSocket < 0) {
			String msg = "Unable to open slave: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo());
			// destroy context
			ctx.free();
			// delete class instance
			ctx.delete();
			ctx = null;
			slaveProcessor = null;
			throw new ModbusSlaveException(msg);
		}
		return slaveProcessor;
	}

	public void close() throws ModbusSlaveException {
		if (ctx == null) {
			return;
		}
		if (serverSocket >= 0) {
			lock.lock();
			try {
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Closing server socket");
				}
				stopState = 1;
				// close the network connection and socket (tcpPiAccept /
				// receive call is aborted)
				ctx.close();
				try {
					while (stopState != 2) {
						if (!stopped.await(openCloseTimeout, TimeUnit.MILLISECONDS)) {
							throw new ModbusSlaveException("Cannot close back end within " + openCloseTimeout + "ms");
						}
					}
				} catch (ModbusSlaveException e) {
					throw e;
				} catch (Exception e) {
					throw new ModbusSlaveException("Closing failed", e);
				}
				stopState = 0;
			} finally {
				lock.unlock();
			}
			serverSocket = -1;
		}
		// destroy context
		ctx.free();
		// delete class instance
		ctx.delete();
		ctx = null;
		slaveProcessor = null;
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Modbus slave closed");
		}
	}

	private boolean isClosing() {
		lock.lock();
		try {
			if (stopState == 1) {
				stopState = 2;
				stopped.signalAll();
				return true;
			}
			return false;
		} finally {
			lock.unlock();
		}
	}

	public void run() {
		int connectionCount = 0;
		boolean isSlaveProcessorConnected = false;
		UInt8Array request = new UInt8Array(ModbusTcpPi.MODBUS_TCP_MAX_ADU_LENGTH);
		IntArray readFds = new IntArray(1 /* serverSocket */ + maxConnectionCount);
		int readFdsCount = 0;
		try {
			while (true) {
				if (readFdsCount == 0) {
					boolean isIncomingConnection;
					do {
						if (log.isLoggable(Level.INFO)) {
							log.log(Level.INFO, "Waiting for data...");
						}
						do {
							readFdsCount = ctx.selectRead(readFds.cast());
							// if slave is being closed
							if (isClosing()) {
								if (isSlaveProcessorConnected) {
									// disconnect slave processor
									try {
										slaveProcessor.disconnect();
									} catch (ModbusSlaveException e) {
										log.log(Level.SEVERE, "Cannot clean up backend", e);
									}
								}
								return;
							}
							if (readFdsCount < 0) {
								System.err.println("Waiting for data failed: " + ctx.getErrNo() + " "
										+ ctx.strError(ctx.getErrNo()));
							}
						} while (readFdsCount <= 0);
						isIncomingConnection = readFds.getitem(0) == serverSocket;
						if (isIncomingConnection) {
							// accept the connection
							int clientSocket = ctx.tcpPiAccept(serverSocket);
							if (clientSocket < 0) {
								log.log(Level.SEVERE, "Unable to accept a connection: " + ctx.getErrNo() + " "
										+ ctx.strError(ctx.getErrNo()));
							} else {
								if (log.isLoggable(Level.INFO)) {
									log.log(Level.INFO, "Connection established: " + clientSocket);
								}
								connectionCount++;
								if (!isSlaveProcessorConnected) {
									try {
										// connect slave processor
										slaveProcessor.connect();
										isSlaveProcessorConnected = true;
									} catch (ModbusSlaveException e) {
										log.log(Level.SEVERE, "Cannot initialize backend", e);
									}
								}
							}
						}
					} while (isIncomingConnection);
				}
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Processing request from connection " + readFds.getitem(readFdsCount - 1));
				}
				// set client socket
				ctx.setSocket(readFds.getitem(readFdsCount - 1));
				readFdsCount--;
				// wait for a request
				int requestLength;
				Date timeStamp;
				do {
					requestLength = ctx.receive(request.cast());
					timeStamp = new Date();
					// filtered requests return 0
				} while (requestLength == 0);
				// if an error has occurred
				if (requestLength < 0) {
					// if "Connection reset by peer"
					if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
						if (log.isLoggable(Level.INFO)) {
							log.log(Level.INFO, "Failed to receive message: " + ctx.getErrNo() + " "
									+ ctx.strError(ctx.getErrNo()));
						}
						// close client
						ctx.close(ctx.getSocket());
						connectionCount--;
						if (connectionCount == 0 && isSlaveProcessorConnected) {
							// disconnect slave processor
							try {
								slaveProcessor.disconnect();
								isSlaveProcessorConnected = false;
							} catch (ModbusSlaveException e) {
								log.log(Level.SEVERE, "Cannot clean up backend", e);
							}
						}
					} else {
						log.log(Level.SEVERE,
								"Failed to receive message: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
					}
					// wait for next connection/request
					continue;
				}
				// if initialization of backend failed
				if (!isSlaveProcessorConnected) {
					log.log(Level.SEVERE, "Discarding request due to failed initialization of backend");
					// send exception response
					if (ctx.replyException(request.cast(), ModbusBase.MODBUS_EXCEPTION_SLAVE_OR_SERVER_FAILURE) < 0) {
						// if "Connection reset by peer"
						if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
							if (log.isLoggable(Level.INFO)) {
								log.log(Level.INFO, "Failed to receive message: " + ctx.getErrNo() + " "
										+ ctx.strError(ctx.getErrNo()));
							}
							// close client
							ctx.close(ctx.getSocket());
							connectionCount--;
						} else {
							log.log(Level.SEVERE, "Failed to send exception response: " + ctx.getErrNo() + " "
									+ ctx.strError(ctx.getErrNo()));
						}
					}
					// wait for next connection/request
					continue;
				}
				ModbusMapping mapping = null;
				try {
					int headerLength = ctx.getHeaderLength();
					// get function code
					short functionCode = request.getitem(headerLength);
					String requestDescr = null;
					if (log.isLoggable(Level.INFO)) {
						if (functionCode == ModbusBase.MODBUS_FC_READ_COILS) {
							requestDescr = "READ_COILS";
						} else if (functionCode == ModbusBase.MODBUS_FC_READ_DISCRETE_INPUTS) {
							requestDescr = "READ_DISCRETE_INPUTS";
						} else if (functionCode == ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS) {
							requestDescr = "READ_HOLDING_REGISTERS";
						} else if (functionCode == ModbusBase.MODBUS_FC_READ_INPUT_REGISTERS) {
							requestDescr = "READ_INPUT_REGISTERS";
						} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_COIL) {
							requestDescr = "WRITE_SINGLE_COIL";
						} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS) {
							requestDescr = "WRITE_MULTIPLE_COILS";
						} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_REGISTER) {
							requestDescr = "WRITE_SINGLE_REGISTER";
						} else if (functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_REGISTERS) {
							requestDescr = "WRITE_MULTIPLE_REGISTERS";
						}
						log.log(Level.INFO, "Received " + requestDescr);
					}
					boolean isRead = functionCode == ModbusBase.MODBUS_FC_READ_COILS
							|| functionCode == ModbusBase.MODBUS_FC_READ_DISCRETE_INPUTS
							|| functionCode == ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS
							|| functionCode == ModbusBase.MODBUS_FC_READ_INPUT_REGISTERS;
					boolean isWrite = functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_COIL
							|| functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS
							|| functionCode == ModbusBase.MODBUS_FC_WRITE_SINGLE_REGISTER
							|| functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_REGISTERS;
					// get address
					int address = ctx.getInt16FromInt8(request.cast(), headerLength + 1);
					// get quantity
					int addressQuantity = 1;
					if (isRead || functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_COILS
							|| functionCode == ModbusBase.MODBUS_FC_WRITE_MULTIPLE_REGISTERS) {
						addressQuantity = ctx.getInt16FromInt8(request.cast(), headerLength + 3);
					}
					mapping = slaveProcessor.acquireMapping();
					// if data shall be read
					if (isRead) {
						if (log.isLoggable(Level.FINE)) {
							log.log(Level.FINE, "Reading data for functionCode=" + functionCode + ", address=" + address
									+ ", addressQuantity=" + addressQuantity);
						}
						// update data
						try {
							slaveProcessor.read(functionCode, address, addressQuantity, mapping);
						} catch (ModbusSlaveException e) {
							log.log(Level.SEVERE, "Cannot read data for functionCode=" + functionCode + ",address="
									+ address + ",quantity=" + addressQuantity, e);
							// send exception response
							if (ctx.replyException(request.cast(),
									ModbusBase.MODBUS_EXCEPTION_SLAVE_OR_SERVER_FAILURE) < 0) {
								// if "Connection reset by peer"
								if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
									if (log.isLoggable(Level.INFO)) {
										log.log(Level.INFO, "Failed to send exception response: " + ctx.getErrNo() + " "
												+ ctx.strError(ctx.getErrNo()));
									}
									// close client
									ctx.close(ctx.getSocket());
									connectionCount--;
									if (connectionCount == 0) {
										// disconnect slave processor
										try {
											slaveProcessor.disconnect();
											isSlaveProcessorConnected = false;
										} catch (ModbusSlaveException e1) {
											log.log(Level.SEVERE, "Cannot clean up backend", e1);
										}
									}
								} else {
									log.log(Level.SEVERE, "Failed to send exception response: " + ctx.getErrNo() + " "
											+ ctx.strError(ctx.getErrNo()));
								}
							}
							// wait for next connection/request
							continue;
						}
					}
					// send response
					if (ctx.reply(request.cast(), requestLength, mapping) < 0) {
						// if "Connection reset by peer"
						if (ctx.getErrNo() == ModbusBase.ERRNO_ECONNRESET) {
							if (log.isLoggable(Level.INFO)) {
								log.log(Level.INFO, "Failed to send response: " + ctx.getErrNo() + " "
										+ ctx.strError(ctx.getErrNo()));
							}
							// close client
							ctx.close(ctx.getSocket());
							connectionCount--;
							if (connectionCount == 0) {
								// disconnect slave processor
								try {
									slaveProcessor.disconnect();
									isSlaveProcessorConnected = false;
								} catch (ModbusSlaveException e) {
									log.log(Level.SEVERE, "Cannot clean up backend", e);
								}
							}
						} else {
							log.log(Level.SEVERE,
									"Failed to send response: " + ctx.getErrNo() + " " + ctx.strError(ctx.getErrNo()));
						}
						// wait for next connection/request
						continue;
					}
					if (log.isLoggable(Level.INFO)) {
						log.log(Level.INFO, "Sent " + requestDescr);
					}
					// if data has been written
					if (isWrite) {
						if (log.isLoggable(Level.FINE)) {
							log.log(Level.FINE, "Writing data for functionCode=" + functionCode + ", address=" + address
									+ ", addressQuantity=" + addressQuantity);
						}
						// update data
						try {
							slaveProcessor.write(functionCode, address, addressQuantity, timeStamp, mapping);
						} catch (ModbusSlaveException e) {
							log.log(Level.SEVERE, "Cannot write data for functionCode=" + functionCode + ",address="
									+ address + ",quantity=" + addressQuantity, e);
						}
					}
				} finally {
					if (mapping != null) {
						slaveProcessor.releaseMapping();
					}
				}
			}
		} finally {
			// destroy request structure
			request.delete();
			// destroy structure for read fds
			readFds.delete();
		}
	}

	@Override
	public ModbusMapping createMapping(int coils, int discreteInputs, int holdingRegisters, int inputRegisters) {
		return ctx.mappingNew(coils, discreteInputs, holdingRegisters, inputRegisters);
	}

	@Override
	public void destroyMapping(ModbusMapping mapping) {
		// destroy mapping structure
		ctx.mappingFree(mapping);
		// destroy class instance
		mapping.delete();
	}

	@Override
	public void setFloat(float value, UInt16Array destRegisters) {
		ctx.setFloat(value, destRegisters.cast());
	}

	@Override
	public float getFloat(UInt16Array destRegisters) {
		return ctx.getFloat(destRegisters.cast());
	}
}
