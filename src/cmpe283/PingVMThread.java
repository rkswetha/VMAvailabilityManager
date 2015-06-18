package cmpe283;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.vmware.vim25.AlarmSetting;
import com.vmware.vim25.AlarmSpec;
import com.vmware.vim25.AlarmState;
import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.StateAlarmExpression;
import com.vmware.vim25.StateAlarmOperator;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.mo.Alarm;
import com.vmware.vim25.mo.AlarmManager;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

/**
*
* @author Swetha RK
* CMPE283 Project 1- Disaster Recovery Manager  
*/
public class PingVMThread extends Thread {

    ServiceInstance si = null;
    ArrayList<VHostInfo> vList = new ArrayList<VHostInfo>();
    
    int sleepCount = 0;
    
    //Constructor that accepts serviceinstance as parameter
    PingVMThread(ServiceInstance si) throws RuntimeFault, RemoteException {
        this.si = si;
    }

    // Retrieve each VM under each Host. Ping each VM. If Ping successful, return.
    // If ping not successful, then check if VM is manually powered off (by alarm).
    // If manually powered off(alarm raised),then just return.
    // If no alarm raised, then ping VHost.
    // If ping to vHost is successful, then recover VM. If ping fails, then recover vHost.
    // Note: If there's no ip assigned for vHost, then do a ping using hostname.
    public void run() {

        String ip;
        while(true){
	        try {
	            	// Wait for the snapshot thread to notify on completion on taking snapshot.
		        	if(sleepCount == 0){
			        	synchronized(this) {
			                try {
			                	System.out.println("PingVMThread: Waiting for snapshot thread to notify...");
			                    wait();
			                } catch(InterruptedException e) {
			                  throw new RuntimeException(e);
			                }
			              }
		        	}
		        	
		        	System.out.println("PingVMThread: Got notification from snapshot thread...");
		        	
		        	Folder rootFolder = si.getRootFolder();
	
	                //Getting list of all hostsystems in Vcenter
	                ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
	                if (mes == null || mes.length == 0) {
	                    System.out.println("PingVMThread: No Hosted VM's found in your Vcenter...");
	                    return;
	                }
	                
	                //Looping all hostsystems
	                for (ManagedEntity me : mes) {
	
	                	System.out.println("==============================================");
	                    HostSystem host = (HostSystem) me;
	                	System.out.println("PingVMThread: Retrieving list of all VMs for vHost: "+host.getName());
	                    
	                    //Getting list of all VMs within a hostsystem
	                    ManagedEntity[] mes1 = host.getVms();
	
	                    //Looping all VMs within a host
	                    for (ManagedEntity me1 : mes1) {
	
	                        VirtualMachine vm = (VirtualMachine) me1;
	                    	
	                        displayVMDetailsInv(si, vm, host);
	                    	
	                        //Creating new alarm for VM
	                        createAlarm(si, vm);
	                        ip = vm.getSummary().getGuest().getIpAddress();
	                        
	                        try {
	                            if (ip != null) {
	                            	monitorVM(vm, host);
	                            } 
	                            else {
	                            	monitorVHost(vm, host);
	                            }
	                        } catch (Exception e) {
	                            System.out.println("PingVMThread: Exception occured "
	                                    + e.getMessage());
	
	                        }
	                    }
	                    System.out.println("==============================================");                    
	                }
	            } catch (Exception e) {
	                System.out.println("PingVMThread: Exception occured " + Arrays.toString(e.getStackTrace()));
	
	            }
	            try {
	            	
	            	System.out.println("PingVMThread: ====SleepCount = "+sleepCount);
	            	Thread.sleep(300000); // Ping every 5mins
	            	sleepCount++;
	            	
	            	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	         	   	Date date = new Date();
	         	   
	                System.out.println("PingVMThread: Current time:"+ dateFormat.format(date));
	                
	            	
	            } catch (Exception e) {
	                System.out.println("PingVMThread: Could not interrupt the thread to sleep" + e.getMessage());
	            }
        }
    }

    private void monitorVM(VirtualMachine vm, HostSystem host) throws Exception
    {
    	String ip = vm.getSummary().getGuest().getIpAddress();
    	System.out.println("PingVMThread: ...........Monitor VM scenario for VM ..........."+vm.getName()); 

    	if (verifyPing(ip)) {
    		System.out.println("PingVMThread:" + vm.getName() + " pinged successfully!!!");
	     } 
    	else {
    		System.out.println("PingVMThread:" + vm.getName() + " not pinged successfully!!!");
    		monitorVHost(vm, host);
	     }
    }
    
    private void monitorVHost(VirtualMachine vm,HostSystem host)throws Exception
    {
    	System.out.println("PingVMThread: ...........Monitor VHost scenario for VHost..........."+vm.getName()); 
        
    	// Get alarm status for a given VM
    	boolean alarmState = checkAlarm(vm);
        System.out.println("PingVMThread: alarmState for VM " + vm.getName()+ " is: "+ alarmState);
        
        if (alarmState) {
           System.out.println("PingVMThread: "+ vm.getName() + " not pinged successfully!!!");
           System.out.println("PingVMThread: Triggered alarm found for " + vm.getName() + " No Action Required.");
        } else {
            System.out.println("PingVMThread:" + vm.getName() + " not pinged successfully!!!");
            System.out.println("PingVMThread: Checking for respective host.");
            
            if (verifyPing(host.getName())) {

                System.out.println("PingVMThread: Host for " + vm.getName() + " is pinging.");
                System.out.println("PingVMThread: Recovering VM : " + vm.getName());
                           		
                VHostInfo vObj = new VHostInfo(this.si);
                vObj.setVHostName(vm.getName());
                vList.add(vObj);
                vObj.recoverVMToLatestSnapshot();
                
                Thread.sleep(10000); 
                
            } else {
                System.out.println("PingVMThread: Host " + host.getName() + " is also not pinging");
                System.out.println("PingVMThread: Recovering Host : " + host.getName());
               
                VHostInfo vObj = new VHostInfo(this.si);
                vObj.setVHostName(host.getName());
                vList.add(vObj);
                vObj.recoverVHost();
                
                Thread.sleep(30000);
                
            }            
        }
    }
    
    private void displayVMDetailsInv(ServiceInstance si, VirtualMachine vm, HostSystem host) 
    {
    	String macAddress="";
    	    
    		for(VirtualDevice vd:vm.getConfig().getHardware().getDevice()){
    	    try {
	    	    	VirtualEthernetCard vEth=(VirtualEthernetCard)vd;
	    	    	macAddress=vEth.macAddress;
	    	    }
	    	    catch(Exception e){}
	    	}
	    	System.out.println("------------ Name : "+vm.getName() +" --------------");
	    	System.out.println("------------ VM Guest OS ip address: "+vm.getSummary().getGuest().getIpAddress()+" --------------");
	    	System.out.println("------------ VM wayer version is ..from inventory.. "+ vm.getConfig().version +" --------------");
	    	System.out.println("------------ Guest os uuid "+vm.getSummary().getConfig().uuid +" --------------");
	    	System.out.println("------------ Guest mac Address of guest  "+macAddress +" --------------");
	    	System.out.println("------------ Host ipaddress is "+host.getName() +" --------------");
	    	
	}
    
    //Function to create alarm for a VM
    public void createAlarm(ServiceInstance si, VirtualMachine vm) throws DuplicateName, RuntimeFault, RemoteException {

    	// Remove alarm for a given VM if it already exist
    	Alarm[] alarm = si.getAlarmManager().getAlarm(vm);
        for (Alarm alarm1 : alarm) {

            alarm1.removeAlarm();
        }

        AlarmManager alarmMgr = si.getAlarmManager();

        //Initializing class to give alarm specifications
        AlarmSpec spec = new AlarmSpec();

        StateAlarmExpression expression
                = createStateAlarmExpression();

        //Defining alarm specifications
        spec.setExpression(expression);
        spec.setName("VmPowerOffAlarm-"+ vm.getName());
        spec.setDescription("Monitor VM state and trigger alarm if VM powers off");
        spec.setEnabled(true);

        //Defining alarm settings
        AlarmSetting as = new AlarmSetting();
        as.setReportingFrequency(0); 
        as.setToleranceRange(0);

        spec.setSetting(as);

        //Actual alarm creation on VM
        alarmMgr.createAlarm(vm, spec);

    }

    //Function to check status of alarm for  a VM
    public boolean checkAlarm(VirtualMachine vm) throws RemoteException {
         AlarmState[] alarmStates=vm.getTriggeredAlarmState();
            if (alarmStates != null){
                return true;
                
            }
            return false;
    }

    static StateAlarmExpression createStateAlarmExpression() {
        StateAlarmExpression expression
                = new StateAlarmExpression();
        expression.setType("VirtualMachine");
        expression.setStatePath("runtime.powerState");
        expression.setOperator(StateAlarmOperator.isEqual);
        expression.setRed("poweredOff");
        return expression;
    }
    
    //Function to ping an IP
    public synchronized boolean verifyPing(String ip) throws Exception {

    	
    	System.out.println("Ping to: "+ip);
    	String inputLine = null;
        String pingResult = null;
        ProcessBuilder pb = new ProcessBuilder("ping", ip);
        Process process = pb.start();

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                process.getInputStream()));

        while ((inputLine = stdInput.readLine()) != null) {
            pingResult += inputLine;
        }
        stdInput.close();

        System.out.println("Ping output for ip "+ip+":" + pingResult);
        
        //Return status based on pinging result
        if ((pingResult.contains("Reply from")) && (pingResult.contains("Average"))) {
            return true;
        } else {
            return false;
        }
    }
}
