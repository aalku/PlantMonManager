package org.aalku.plantMonitor.manager;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

@Component
class PortManager extends Thread implements Closeable {

	private static final int TIMEOUT_RX_HELLO_SECONDS = 10;

	private final Logger log = LogManager.getLogger(PortManager.class);

	private final StringBuilder partialLine = new StringBuilder();

	private SerialPort port = null;
	
	private final AtomicBoolean reconnect = new AtomicBoolean(false);

	private String portName;
	
	{
		this.setDaemon(true);
	}

	private final AtomicBoolean connected = new AtomicBoolean(false);
	private final AtomicBoolean connecting = new AtomicBoolean(false);

	private Optional<Consumer<String>> scanSuccessHandler = Optional.empty();

	private boolean allowScan;

	private Optional<Consumer<String>> dataHandler = Optional.empty();

	@Override
	public void run() {
		while (!this.isInterrupted()) {
			handleConnect();
			if (isConnected()) {
				String read = readLine(0L, null);
				if (read != null) {
					log.debug("Received {}", read);
					dataHandler.ifPresent(h->h.accept(read));
				}
			} else {
				delay();
			}
		}
	}

	private void delay() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private String readLine(long timeout, TimeUnit timeUnit) {
		SerialPort _port = this.port;
		if (!isConnected() && !isConnecting()) {
			return null;
		} else if (_port == null) {
			return null;
		}
		boolean blocking = timeout > 0L;
		Instant timeoutInstant = blocking ? Instant.now().plus(timeUnit.toNanos(timeout), ChronoUnit.NANOS) : null;
		String res = null;
		while (true) {
			try {
				String read;
				while (null != (read = _port.readString())) {
					partialLine.append(read);
				}
			} catch (SerialPortException e) {
				internalDisconnect();
				e.printStackTrace();
				return null;
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
			
			int i;
			while (0 <= (i = findLf.getAsInt())) {
				if (i == 0) {
					partialLine.deleteCharAt(0);
				} else {
					res = partialLine.substring(0, i);
					partialLine.delete(0, i + 1);
				}
			}
			if (res != null || !blocking || Instant.now().isAfter(timeoutInstant)) {
				break;
			}
		}
		return res;
	}

	private boolean isConnecting() {
		return connecting.get();
	}

	private void handleConnect() {
		Consumer<String> tryPort = (_portName) -> {
			if (!isConnected()) {
				reconnect.set(true);
			}
			if (_portName != null && reconnect.getAndSet(false)) {
				internalDisconnect();
				if (connectRX(_portName)) {
					this.portName = _portName;
				}
			}
		};
		if (portName != null && !portName.isEmpty()) {
			tryPort.accept(portName);
		}
		if (allowScan && !isConnected()) {
			List<String> ports = Arrays.asList(SerialPortList.getPortNames());
			log.info("Scanning serial ports: {}", ports);
			for (String port: ports) {
				tryPort.accept(port);
				if (isConnected()) {
					scanSuccessHandler.ifPresent(h->h.accept(port));
					break;
				}
			}
		}
		if (!isConnected()) {
			reconnect.set(true);
		}

	}

	private boolean isConnected() {
		return connected.get() && port != null && port.isOpened();
	}

	public void close() throws IOException {
		this.interrupt();
		try {
			connected.set(false);
			if (port != null) {
				port.closePort();
				port = null;
			}
		} catch (SerialPortException e) {
			throw new IOException(e);
		}
	}
	
	private boolean connectRX(String portName) {
		log.debug("Connecting to RX at {} ...", portName);
		internalDisconnect();
		partialLine.setLength(0);
		port = new SerialPort(portName);		
		boolean ok = false;
		connecting.set(true);
		try {
			ok = port.openPort();
			if (ok) {
				port.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, false, true);
				ok = waitReceptorHello();
			}
			connected.set(ok);
			if (ok) {
				log.info("Connected to RX at {} !", portName);
			}
			return ok;
		} catch (SerialPortException e1) {
			e1.printStackTrace();
		} finally {
			if (!isConnected()) {
				internalDisconnect();
			}
			connecting.set(false);
		}
		return false;
	}

	private void internalDisconnect() {
		if (port != null) {
			try {
				port.closePort();
				port = null;
			} catch (SerialPortException e) {
			}
			connected.set(false);
		}
	}

	private boolean waitReceptorHello() {
		long t = TimeUnit.SECONDS.toNanos(TIMEOUT_RX_HELLO_SECONDS);
		long to = t + System.nanoTime();
		while (true) {
			long nanosTimeout = to - System.nanoTime();
			// log.debug("Waiting for RX at for {} seconds", TimeUnit.NANOSECONDS.toMillis(nanosTimeout)/1000d);
			String line = readLine(nanosTimeout, TimeUnit.NANOSECONDS);
			if (line != null && line.endsWith("RX")) {
				return true;
			} else if (nanosTimeout <= 0L) {
				break;
			}
		}
		return false;
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

	public void setScanSuccessHandler(Consumer<String> handler) {
		this.scanSuccessHandler = Optional.ofNullable(handler);
	}

	public void setAllowScan(boolean allowScan) {
		this.allowScan = allowScan;
		
	}

	public void setDataHandler(Consumer<String> handler) {
		this.dataHandler = Optional.ofNullable(handler);
	}

}