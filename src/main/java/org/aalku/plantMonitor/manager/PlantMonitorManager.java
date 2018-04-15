package org.aalku.plantMonitor.manager;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.aalku.plantMonitor.manager.repository.ConfigRepository;
import org.aalku.plantMonitor.manager.vo.PersistedConfiguration;
import org.aalku.plantMonitor.manager.vo.PlantData;
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

	private static Logger log = LogManager.getLogger(PlantMonitorManager.class);

	@Resource
	private PortManager port;
	
	@Resource
	private ConfigRepository configRepo;
	
	@Resource
	private PlantDataService plantDataService;
	
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
				port.setPortName(portName);
				config.setPortName(portName);
				configRepo.save(config);
				return "Port changed: " + portName;
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
		
		port.setPortName(portName);
		port.setAllowScan(true);
		port.setScanSuccessHandler((pn) -> {
			config.setPortName(pn);
			configRepo.save(config);
		});
		// Received: 25, AD50;35;3.30;5.18;611;52
		Pattern pattern = Pattern.compile("^Received: ([0-9]+), ([0-9A-F]+);([0-9A-F]+);([0-9.]+);([0-9.]+);([0-9]+);([0-9]+)$");
		port.setDataHandler(line->{
			Matcher m;
			if ((m = pattern.matcher(line)).matches()) {
				int repLength = Integer.parseInt(m.group(1));
				int meaLength = m.end(0) - m.start(2);
				if (repLength == meaLength && repLength == 32 || repLength == meaLength + 1 && repLength <= 32) {
					PlantData d = new PlantData();
					d.setVcc(Float.parseFloat(m.group(4)));
					d.setVbat(Float.parseFloat(m.group(5)));
					d.setSense(Integer.parseInt(m.group(6)));
					String addr = m.group(2);
					String seq = m.group(3);
					Integer perc = Integer.valueOf(m.group(7));
					log.info("Received: addr={}, seq={}, data={}, eperc={}", addr, seq, d, perc);
					plantDataService.store(addr, d);
				} else {
					log.warn("Cropped message: {}", line);
				}
			} else {
				log.warn("Noise: {}", line);
			}
		});
		port.start();
	}

}