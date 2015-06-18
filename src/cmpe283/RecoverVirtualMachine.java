package cmpe283;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.HostVMotionCompatibility;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
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
// This thread class performs the VM/VHost recovery operation using the snapshot revert operation. 
public class RecoverVirtualMachine extends Thread{

	ServiceInstance si;
    String vmName = null;
    String VMType = null;
    int VMRetryCount = 0;
	
	RecoverVirtualMachine(ServiceInstance si, String vmName, String VMType) {
        this.si = si; // If VMType == VM, then si= myVcenter. If VMType == Host, then Si = master vcenter
        this.vmName = vmName; // If VMType == VM, then vmName will be VM name. If VMType == Host, then vmName will be hostIp
        this.VMType = VMType;
    }
	
	@Override
    public void run() {
		
		try{
			if(VMType == "VM")
				recoverVM();
			else if (VMType == "VHOST")
				recoverVHost();
		} catch (IOException e) {
            System.out.println("Exception occured in Recovery thread" + e.getMessage());
        }
	}
	
	public void recoverVM()
	{
		System.out.println(" RecoverVirtualMachine: Recovering VM " + vmName);
		
		try {
            Folder rootFolder = si.getRootFolder();
            //Searching the VirtualMachine by the given name
            VirtualMachine vm = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmName);

            if (vm != null) {

            	System.out.println(" RecoverVirtualMachine: Found VM in rootFolder");
                //Reverting VM from the current snapshot    
                Task task = vm.revertToCurrentSnapshot_Task(null);
                if (task.waitForMe().equals(Task.SUCCESS)) {
                    
                	System.out.println("RecoverVirtualMachine: "+vm.getName() + " reverted to latest working snapshot.");
   
                	 if (vm.getSummary().runtime.powerState.toString().equals("poweredOff")){
	                
                		 //Powering on the VM after reverting
		                task = vm.powerOnVM_Task(null);
		                if (task.waitForMe().equals(Task.SUCCESS)) {
		                    System.out.println("RecoverVirtualMachine: VM: " + vm.getName() + " powered on.");
		                    return;
		                }
	                }
	           }

            } else {
                //Output when the given VM is not found    
                System.out.println("RecoverVirtualMachine: The given VM not found.");
                return;
            }
            
        } catch (Exception e) {
            //Exception handler
            System.out.println("RecoverVirtualMachine: Exception occured during recoverVM " + e.getMessage());
        }
	}
	
	// This function is called when Ping to vHost fails and need to recover vHost.
	public void recoverVHost() throws IOException
	{
		System.out.println(" RecoverVirtualMachine: Recovering Host" + vmName);
		
		try {
            //Using the substring method on string hostIp
            String sub = vmName.substring(7);
            System.out.println(" RecoverVirtualMachine: Recovering Host: sub" + sub);
            
            AppProperties prop = new AppProperties();
    		String vCenterAdminUrl = prop.getVCenterAdminURL();
    		String vCenterAdminLogin = prop.getVCenterAdminUsername();		
    		String vCenterAdminPassword = prop.getVCenterAdminPassword();
    		String hostNameForSnapshot = prop.getHostNameForSnapshot();
    		
    		System.out.println("RecoverVirtualMachine: Logging into Admin VCenter: "+vCenterAdminUrl +" as login: "+vCenterAdminLogin +vCenterAdminPassword);
    
    		//Creating serviceinstance for admin VCenter
            ServiceInstance sih = new ServiceInstance(new URL(vCenterAdminUrl), vCenterAdminLogin, vCenterAdminPassword, true);
            Folder rootFolder = sih.getRootFolder();
            ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");

            for (ManagedEntity me : mes) {

                VirtualMachine vm = (VirtualMachine) me;

                //Finding host that contains "sub" in its name
                if (vm.getName().contains(sub)) {

                    //Checking if the host is powered on
                	// If its ON, then revert to latest snapshot and bring up all the VM under that vHost
                	if (vm.getSummary().runtime.powerState.toString().equals("poweredOn")) {

                		System.out.println("RecoverVirtualMachine: Trying to recover VHost " + vm.getName());
                		
                        //Reverting host from the current snapshot
                        Task task = vm.revertToCurrentSnapshot_Task(null);
                        if (task.waitForMe().equals(Task.SUCCESS)) {
                            System.out.println("RecoverVirtualMachine: Host " + vm.getName() + " reverted to snapshot " + vm.getCurrentSnapShot());
                            
                            if (vm.getSummary().runtime.powerState.toString().equals("poweredOff")) {
	                            //Powering on the host after reverting and bring up all VMs
		                        task = vm.powerOnVM_Task(null);
		                        if (task.waitForMe().equals(Task.SUCCESS)) {
		                            System.out.println("RecoverVirtualMachine: Host:" + vm.getName() + " powered on.");
		                            //Thread.sleep(1000);
		                            
		                            //Calling function by passing hostIp
			                        turnVmOn(vmName);
			                        
			                        return;
		                        }
		                        else // Retry again after sometime.
		                        {
		                        	if(VMRetryCount < 3)
		                        	{	
				                        try {
				                            Thread.sleep(180000);
				                        } catch (Exception e) {
				                            System.out.println("RecoverVirtualMachine: Could not interrupt the thread to sleep" + e.getMessage());
				
				                        }
				                        VMRetryCount++;
		                        	}
		                        	else
		                        	{
		                        		System.out.println("RecoverVirtualMachine: Migrating "+vmName +" to another host " +hostNameForSnapshot);
		                        		migrateToAnotherHost(vmName, hostNameForSnapshot);		                        	
		                        	}
		                        }
	                        }
                        }
                    	
                    } else {
                        
                    	System.out.println("RecoverVirtualMachine: Power on Host:" + vm.getName() + " and all VMs.");
                    	
                        //Powering on the host if powered off and bringup all VMs
                        Task task = vm.powerOnVM_Task(null);
                        if (task.waitForMe().equals(Task.SUCCESS)) {
                        	
                            System.out.println("Host:" + vm.getName() + " powered on.");
                            //Calling function by passing hostIp
                            //Thread.sleep(1000);
                            turnVmOn(vmName);
                            return;
                        }
                    }
                }
            }
            
            sih.getServerConnection().logout();
            
        } catch (Exception e) {
            System.out.println("RecoverVirtualMachine: Exception occured during recoverVHost" + e.getMessage());
        }
}

public void turnVmOn(String hostName) throws RemoteException, MalformedURLException {

	System.out.println("RecoverVirtualMachine: turnVmOn: All VMs under " + hostName);
	
	try{
		AppProperties prop = new AppProperties();
		String vCenterUrl = prop.getVCenterURL();
		String vCenterlogin = prop.getVCenterUsername();		
		String vCenterpassword = prop.getVCenterPassword();
		String hostNameForColdMigration = prop.getHostNameForSnapshot();
		
		//Creating serviceinstance for 119 VCenter
		ServiceInstance si = new ServiceInstance(new URL(vCenterUrl), vCenterlogin, vCenterpassword, true);
		    
	    Folder rootFolder = si.getRootFolder();
	    HostSystem host = (HostSystem) new InventoryNavigator(rootFolder).searchManagedEntity("HostSystem", hostName);
	    
	    HostConnectSpec hSpec = new HostConnectSpec();
			hSpec.setHostName("130.65.133.22");
			hSpec.setUserName("root");
			hSpec.setPassword("12!@qwQW");
			hSpec.setForce(true);
			//hSpec.setSslThumbprint("B0:26:73:C4:11:9D:4C:5D:9A:CD:13:BE:2A:A0:93:36:0C:0E:97:EA");
		
		//Task reconnectTask = host.reconnectHost_Task(hSpec);
	
		System.out.println("RecoverVirtualMachine: Trying to reconnect Host");
	
		//String reconnectStatus = reconnectTask.waitForMe();
		String reconnectStatus = "FAILED";
		if(reconnectStatus==Task.SUCCESS)
		{
        	System.out.println("RecoverVirtualMachine: Host reconnect success");

        	ManagedEntity[] mes = host.getVms();
    	    
		    for (ManagedEntity me : mes) {
		
		    	System.out.println("RecoverVirtualMachine: Looping into each VM");
		    	
		        VirtualMachine vm = (VirtualMachine) me;
		        System.out.println("RecoverVirtualMachine: Looping into each VM");
		    	
		        if (vm.getSummary().runtime.powerState.toString().equals("poweredOff")) {
		
		        	//Powering on the VMs in the recovered host
		            Task task = vm.powerOnVM_Task(null);
		            if (task.waitForMe().equals(Task.SUCCESS)) {
		                System.out.println("RecoverVirtualMachine: VM: " + vm.getName() + " powered on.");
		
		            }
		        } else {
		            //Output if no VMs are found powered off
		            System.out.println("RecoverVirtualMachine: Desired VM is already powered on!!!");
		        }
		    }
	    }
		else{
			
				System.out.println("RecoverVirtualMachine: Host reconnect fails");
				System.out.println("RecoverVirtualMachine: Migrating VM from "+vmName +" to another host " +hostNameForColdMigration);
				
				ManagedEntity[] mes = host.getVms();
	    	    
			    for (ManagedEntity me : mes) {
			
			    	System.out.println("RecoverVirtualMachine: Looping into each VM");
			    	
			        VirtualMachine vm = (VirtualMachine) me;
			        
			        migrateToAnotherHost(vm.getName(), hostNameForColdMigration);
			        
			        System.out.println("RecoverVirtualMachine: Cold Migration complete!");
			    }
	        
			}
	}catch(Exception e){
		System.out.println("RecoverVirtualMachine: turnVmOn: Exception occured during turnVmOn" + e.getMessage());
	}
    
 }

//method migrate to another host
 public boolean migrateToAnotherHost(String vmname, String newHostName) {
	
	 try {
		Folder rootFolder = si.getRootFolder();
	
		VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmname);
		
		
		//Start: To get another host IP
		ManagedEntity[] hosts = new InventoryNavigator(rootFolder).searchManagedEntities(
				new String[][] { {"HostSystem", "name" }, }, true);
		for(int i=0; i<hosts.length; i++)
		{
			System.out.println("host["+i+"]=" + hosts[i].getName());
		}
		
		//newHostName = hosts[0].getName();
		String newHostUrl= "https://"+newHostName+"/sdk";
		
		ServiceInstance sitemp = new ServiceInstance(new URL(newHostUrl), "root", "12!@qwQW", true);
		Folder rf = sitemp.getRootFolder();
		ManagedEntity[] vms = new InventoryNavigator(rf).searchManagedEntities(
				new String[][] { {"VirtualMachine", "name" }, }, true);
		for(int i=0; i<vms.length; i++)
		{
			if(vms[i].getName().equalsIgnoreCase(vmname))
			{
				newHostName=hosts[1].getName();
				break;
			}
		}
		if(newHostName.equals(null)||newHostName.equalsIgnoreCase(""))
		{
			System.out.println("New Host is invalid OR Null");
			System.exit(0);
		}
		
		HostSystem newHost = (HostSystem) new InventoryNavigator(rootFolder).searchManagedEntity("HostSystem",newHostName);
		ComputeResource cr = (ComputeResource) newHost.getParent();
		
		String[] checks = new String[] { "cpu", "software" };
		HostVMotionCompatibility[] vmcs = si.queryVMotionCompatibility(vm,
				new HostSystem[] { newHost }, checks);

		String[] comps = vmcs[0].getCompatibility();
		if (checks.length != comps.length) {
			System.out.println("CPU/software NOT compatible. Exit.");
			//si.getServerConnection().logout();
			return false;
		}

		Task task = vm.migrateVM_Task(cr.getResourcePool(),newHost,
				VirtualMachineMovePriority.highPriority,
				VirtualMachinePowerState.poweredOff);
		
		if (task.waitForTask() == Task.SUCCESS) {
			System.out.println("VMotioned Migrated..!");
			/*
			//Before rename delete previous one
			VirtualMachine oldvm = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmname);
			
			// delete the old VM
			Task removeOld = oldvm.destroy_Task();
			if(removeOld.waitForTask()==Task.SUCCESS)
			{
				System.out.println(" The Old VM is deleted successfully");
			}
		
			Task task1 = vm.rename_Task(vmname);
			if (task1.waitForTask() == Task.SUCCESS) {
		
				Task task2 = vm.powerOnVM_Task(newHost);
				if (task2.waitForTask() == Task.SUCCESS) {
					System.out.println("VM On! ");
					//valueSet = true;
					return true;
			} 
			} 
			else {
				System.out.println("VM has not renamed. Do not power on");
			}*/
			return true;
		}
		else 
		{
				System.out.println("VMotion has failed!");
				TaskInfo info = task.getTaskInfo();
				System.out.println(info.getError().getFault());
		}
    } catch (InvalidProperty e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (RuntimeFault e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (RemoteException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (MalformedURLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return false;
}

	
}
