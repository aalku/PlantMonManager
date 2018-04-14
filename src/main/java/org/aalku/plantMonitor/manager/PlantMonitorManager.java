package org.aalku.plantMonitor.manager;

import java.util.Arrays;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jssc.SerialPortList;

@Controller
@SpringBootApplication
public class PlantMonitorManager {

	Logger log = LogManager.getLogger(PlantMonitorManager.class);

	@Resource
	PortManager port;

	@RequestMapping("/")
	@ResponseBody
	String home() {
		return "Serial port names: " + Arrays.asList(SerialPortList.getPortNames());
	}


	@ResponseBody
	@RequestMapping("/connect")
	public String connect(@RequestParam("portName") String portName) {
		try {
			if (!portName.equals(port.getPortName())) {
				if (port.testPort(portName)) {
					port.setPortName(portName);
					return "Port changed: " + portName;
				} else {
					return "Can't open port: " + portName;
				}
			} else {
				return "Reconnecting same port: " + portName;
			}
		} finally {
			port.reconnect();
		}
	}
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(PlantMonitorManager.class, args);
	}

}