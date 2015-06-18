package cmpe283;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
*
* @author Swetha RK
* CMPE283 Project 1- Disaster Recovery Manager  
*/
// Class to read the properties used by the app: vCenterUrl, username, password, ping interval, snapshot interval
public class AppProperties {

	Properties prop = new Properties();
	String propFileName = "config.properties";
	InputStream inputStream;
	
	public Properties readProperty() throws IOException{
		
		inputStream =  getClass().getClassLoader().getResourceAsStream(propFileName);
		prop.load(inputStream);
		
		if (inputStream == null) {
			throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
		}
		
		// Closing the stream
		inputStream.close();
		
		return prop;
	}
	

	public String getVCenterURL() throws IOException {
		
		Properties p = readProperty();
		return p.getProperty("vCenterURL");
	}
	
	
	public String getVCenterUsername() throws IOException {
		
		Properties p = readProperty();
		return p.getProperty("vCenterUsername");
	}
	
    public String getVCenterPassword() throws IOException {

    	Properties p = readProperty();
		return p.getProperty("vCenterPassword");
	}

    public String getVCenterAdminURL() throws IOException {
		
		Properties p = readProperty();
		return p.getProperty("vCenterAdmin");
	}
	
	
	public String getVCenterAdminUsername() throws IOException {
		
		Properties p = readProperty();
		return p.getProperty("vCenterAdminUsername");
	}
	
    public String getVCenterAdminPassword() throws IOException {

    	Properties p = readProperty();
		return p.getProperty("vCenterAdminPassword");
	}

    public String getHostNameForSnapshot() throws IOException {

    	Properties p = readProperty();
		return p.getProperty("HostNameForSnapshot");
	}
    
    public Integer getPingSleepTime() throws IOException {
		
    	Properties p = readProperty();
		return Integer.parseInt(p.getProperty("PingSleepTime"));
    }

    public Integer getSnapshotInterval() throws IOException {
	
    	Properties p = readProperty();
    	return Integer.parseInt(p.getProperty("SnapshotInterval"));
    }	
	

}

