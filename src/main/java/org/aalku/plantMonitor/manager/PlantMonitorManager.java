package org.aalku.plantMonitor.manager;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.aalku.plantMonitor.manager.repository.ConfigRepository;
import org.aalku.plantMonitor.manager.vo.PersistedConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jssc.SerialPortList;

@Controller
@SpringBootApplication
public class PlantMonitorManager implements InitializingBean {

	private Logger log = LogManager.getLogger(PlantMonitorManager.class);

	@Resource
	private PortManager port;
	
	@Resource
	private ConfigRepository configRepo;
	
	private PersistedConfiguration config;

	@RequestMapping("/serialPorts")
	@ResponseBody
	List<String> serialPorts() {
		return Arrays.asList(SerialPortList.getPortNames());
	}

	@RequestMapping("/currentSerialPort")
	@ResponseBody
	String currentPort() {
		return port == null ? null : port.getPortName();
	}

	@ResponseBody
	@RequestMapping("/connectSerialPort")
	public String connect(@RequestParam("portName") String portName) {
		try {
			if (!portName.equals(port.getPortName())) {
				if (port.testPort(portName)) {
					port.setPortName(portName);
					config.setPortName(portName);
					configRepo.save(config);
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


	@Override
	public void afterPropertiesSet() throws Exception {
		config = configRepo.load().orElseGet(()->{
			PersistedConfiguration cfg = new PersistedConfiguration();
			cfg.setId(0L);
			return cfg;
		});
		String portName = config.getPortName();
		if (portName != null && !portName.trim().isEmpty()) {
			this.connect(portName);
		}
		configRepo.save(config);
	}

}