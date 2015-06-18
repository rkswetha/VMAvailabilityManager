package cmpe283;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

/**
*
* @author Swetha RK
* CMPE283 Project 1- Disaster Recovery Manager  
*/
//This class performs the operation of maintaining snapshots of all the live virtual machines and the vHosts. 
public class SnapshotThread extends Thread{

    ServiceInstance si = null;
    int sleepTime = 0;
    int count = 0;
    
    private PingVMThread waitPing;
    
    //Constructor that takes serviceInstance and snapshot period as parameter
    SnapshotThread(ServiceInstance si, int sleepTime, PingVMThread p) {
        this.si = si;
        this.sleepTime = sleepTime;
        this.waitPing = p;
    }
    
    @Override
    public void run() {

    	System.out.println("SnapshotThread: Starting Snapshot thread..");
    	
    	while(true){
    		
	    	try {
	    			hostSnapShot();
			    	
		    		virtualMachineSnapShot();
		    		
		    		if(count == 0){
		        		synchronized(waitPing){
		    				System.out.println("SnapshotThread: Notifying ping thread..");
		    				waitPing.notify();
		    				count++;
		        		}
	    			}
		    		
	    			System.out.println("SnapshotThread: Continue after notification..");
	    			
		    	    //Sleep for sleepTime(milliseconds)
	                Thread.sleep(this.sleepTime);
	                
	                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	         	   	Date date = new Date();
	         	   
	                System.out.println("SnapshotThread: Current time:"+ dateFormat.format(date));
	                
		          //Exception Handling  
	            } catch (Exception e) {
	                System.out.println("SnapshotThread: Could not interrupt the thread to sleep" + e.getMessage());
	            }
    	}
    }
    
    public void virtualMachineSnapShot()throws IOException
    {
    	System.out.println("SnapshotThread:---------------virtualMachineSnapshot method called--------------------");
    	
    	try{
    		AppProperties prop = new AppProperties();
    		String vCenterUrl = prop.getVCenterURL();
    		String vCenterLogin = prop.getVCenterUsername();		
    		String vCenterPassword = prop.getVCenterPassword();
    		
    		System.out.println("SnapshotThread: Logging into VCenter: "+vCenterUrl +" as login: "+vCenterLogin +vCenterPassword);
    		
	    	 //Creating serviceinstance for admin VCenter
	        ServiceInstance si = new ServiceInstance(new URL(vCenterUrl), vCenterLogin, vCenterPassword, true);
	        Folder rootFolder = si.getRootFolder();
	        ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
	        if (mes == null || mes.length == 0) {
	            System.out.println("SnapshotThread: No Hosted VM's found in your Vcenter...");
	            return;
	        }
	        System.out.println("SnapshotThread: Num of Hosted VM's found in your Vcenter:" + mes.length);
	        
	        for (ManagedEntity me : mes) {
	            VirtualMachine vm = (VirtualMachine) me;
	        
	            System.out.println("SnapshotThread:VM name:" + vm.getName());
	            
	            if(vm.getSummary().getConfig().template == true)
	            	System.out.println("SnapshotThread: Not creating snapshots for template: " +vm.getName());
	            else
	            {
	            	//Searching for the hosts based on name and status
		            //Removing all old snapshots from the host
		            Task task = vm.removeAllSnapshots_Task();
		            if (task.waitForMe().equals(Task.SUCCESS)) {
		                    System.out.println("SnapshotThread:All Snapshots removed!!");
		            }
		            //Creating new snapshot for the host
		            task = vm.createSnapshot_Task("latest", "Latest snapshot of VM", false, false);
		            if (task.waitForMe().equals(Task.SUCCESS)) {
		                 System.out.println("SnapshotThread:Snapshot was created for VM: " + vm.getName());
		            } else {
		                 System.out.println("SnapshotThread:Snapshot creation interrupted for Host " + vm.getName());
		            }
	            }
	       }
            //Closing connection to admin VCenter
	        si.getServerConnection().logout();
	    } catch (RemoteException | MalformedURLException e) {
            System.out.println("SnapshotThread: Exception occured " + Arrays.toString(e.getStackTrace()));
        }
    	System.out.println("SnapshotThread:---------------virtualMachineSnapshot method end--------------------");    	
    }
    
    // Create a snapshot for vHost under T14-vHost
    public void hostSnapShot() throws IOException
    {
    	System.out.println("SnapshotThread:-------------------hostSnapShot method called--------------------------");
    	
    	try{
    		AppProperties prop = new AppProperties();
    		String vCenterAdminUrl = prop.getVCenterAdminURL();
    		String vCenterAdminLogin = prop.getVCenterAdminUsername();		
    		String vCenterAdminPassword = prop.getVCenterAdminPassword();
    		String hostNameForSnapshot = prop.getHostNameForSnapshot();
    		
    		System.out.println("SnapshotThread:Logging into Admin VCenter: "+vCenterAdminUrl +" as login: "+vCenterAdminLogin +vCenterAdminPassword);
    		System.out.println("SnapshotThread: Taking Snapshot of VHost : " +hostNameForSnapshot);
    		
	    	//Creating serviceinstance for admin VCenter
	        ServiceInstance si = new ServiceInstance(new URL(vCenterAdminUrl), vCenterAdminLogin, vCenterAdminPassword, true);
	        Folder rootFolder = si.getRootFolder();
	        ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
	        if (mes == null || mes.length == 0) {
	            System.out.println("SnapshotThread: No Hosted VM's found in your Vcenter...");
	            return;
	        }
	
	        for (ManagedEntity me : mes) {
	            VirtualMachine vm = (VirtualMachine) me;
	            
	            //Searching for the hosts based on name 
	            //if(vm.getSummary().runtime.powerState.toString().equals("poweredOn") && vm.getName().contains(hostNameForSnapshot)){
	            if(vm.getName().contains("T14-vHost")){ // Take snapshot of all Team 14 Vhost
	            	
                    System.out.println("SnapshotThread: Initiate snapshot for VHost: "+vm.getName());

	            	//Removing all old snapshots from the host
	                Task task = vm.removeAllSnapshots_Task();
	                if (task.waitForMe().equals(Task.SUCCESS)) {
	
	                    System.out.println("Snapshot thread: All Snapshots removed!!");
	                }
                    
	                //Creating new snapshot for the host
	                task = vm.createSnapshot_Task("latest", "Latest snapshot of vHost", false, false);
	                if (task.waitForMe().equals(Task.SUCCESS)) {
	                    System.out.println("SnapshotThread: Snapshot was created for Host " + vm.getName());
	                } else {
	                    System.out.println("SnapshotThread: Snapshot creation interrupted for Host " + vm.getName());
	                }
	            }
	        }
	        //Closing connection to admin VCenter
	        si.getServerConnection().logout();
	    } catch (RemoteException | MalformedURLException e) {
            System.out.println("SnapshotThread: Exception occured " + Arrays.toString(e.getStackTrace()));
        }
    	
        System.out.println("SnapshotThread:-------------------hostSnapShot method end--------------------------");
    	
    }
 }
