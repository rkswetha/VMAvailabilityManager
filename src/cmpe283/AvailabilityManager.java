package cmpe283;
import java.net.URL;

import com.vmware.vim25.mo.ServiceInstance;

/**
*
* @author Swetha RK
* CMPE283 Project 1- Disaster Recovery Manager  
*/
public class AvailabilityManager {
	
	public static boolean flag;
	
	public static void main(String[] args) throws Exception
	{
		System.out.println("Launch Availablity Manager");
		AppProperties prop = new AppProperties();
		String vCenterUrl = prop.getVCenterURL();
		String vCenterlogin = prop.getVCenterUsername();		
		String vCenterpassword = prop.getVCenterPassword();
		Integer snapshotSleeptime = prop.getSnapshotInterval();
		
		System.out.println("AvailablityManager: Logging into VCenter: "+vCenterUrl +" as login: "+vCenterlogin +vCenterpassword);
		
		URL url = new URL(vCenterUrl);
		ServiceInstance si = new ServiceInstance(url, vCenterlogin, vCenterpassword, true);
		

		PingVMThread pingThread = new PingVMThread(si);
		SnapshotThread snapThread = new SnapshotThread(si, snapshotSleeptime, pingThread);
			
		//Thread for taking snapshots every 10 mins
	    snapThread.start();
	    pingThread.start();

	}

}

