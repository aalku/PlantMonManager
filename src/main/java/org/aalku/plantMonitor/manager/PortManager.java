package org.aalku.plantMonitor.manager;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import jssc.SerialPort;
import jssc.SerialPortException;

@Component
class PortManager extends Thread implements Closeable {

	private final Logger log = LogManager.getLogger(PlantMonitorManager.class);

	private final StringBuilder partialLine = new StringBuilder();

	private SerialPort port = null;
	
	private final AtomicBoolean reconnect = new AtomicBoolean(false);

	private String portName;
	
	{
		this.setDaemon(true);
		this.start();
	}

	@Override
	public void run() {
		while (!this.isInterrupted()) {
			handleConnect();
			if (isConnected()) {
				String read = readLine();
				if (read != null) {
					log.info("Received {}", read);
				}
			} else {
				reconnect.set(true);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private String readLine() {
		try {				
			String read;
			while (null != (read = port.readString())) {
				partialLine.append(read);
			}
		} catch (SerialPortException e) {
			reconnect.set(true);
			e.printStackTrace();
		}
		
		IntSupplier findLf = ()->{
			int ir = partialLine.indexOf("\r");
			int in = partialLine.indexOf("\n");
			int i = in;
			if ((ir >= 0) && (in < 0 || ir < in)) {
				i = ir;
			}
			return i;
		};
		
		String res = null;
		int i;
		while (0 <= (i = findLf.getAsInt())) {
			if (i == 0) {
				partialLine.deleteCharAt(0);
			} else {
				res = partialLine.substring(0, i);
				partialLine.delete(0, i + 1);
			}
		}
		return res;
	}

	private void handleConnect() {
		String _portName = getPortName();
		if (!isConnected()) {
			reconnect.set(true);
		}
		if (_portName != null && reconnect.getAndSet(false)) {
			log.info("Connecting to {}", _portName);
			partialLine.setLength(0);
			if (port != null) {
				try {
					port.closePort();
					port = null;
				} catch (SerialPortException e) {
					e.printStackTrace();
				}
			}
			port = openPort(_portName);
			if (port != null) {
				log.info("Connected to {}!", _portName);
			}
		}
	}

	private boolean isConnected() {
		return port != null && port.isOpened();
	}

	public void close() throws IOException {
		this.interrupt();
		try {
			port.closePort();
			port = null;
		} catch (SerialPortException e) {
			throw new IOException(e);
		}
		
	}
	
	public SerialPort openPort(String portName) {
		SerialPort port = new SerialPort(portName);		
		try {
			boolean ok = port.openPort();
			if (ok) {
				port.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, false, true);
			}
			return ok ? port : null;
		} catch (SerialPortException e1) {
			if (port != null) {
				try {
					port.closePort();
				} catch (SerialPortException e) {
				}
			}
			e1.printStackTrace();
		}
		return null;
	}

	public boolean testPort(String portName) {
		SerialPort port = openPort(portName);
		boolean ok = port != null;
		if (ok) {
			try {
				ok = false;
				port.closePort();
				ok = true;
			} catch (SerialPortException e) {
				e.printStackTrace();
			}
		}
		return ok;
	}

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public void reconnect() {
		reconnect.set(true);
	}

}