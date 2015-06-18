package cmpe283;

import java.rmi.RemoteException;

import com.vmware.vim25.ComputeResourceConfigSpec;
import com.vmware.vim25.Description;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.Permission;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;

/**
*
* @author Swetha RK
* CMPE283 Project 1- Disaster Recovery Manager  
*/
// Implements the interfaces needed for operation related to vHost
public class VHostInfo {

	ServiceInstance si;
    private String vHostName;
    
    //Constructor that takes ServiceInstance as parameter
    VHostInfo(ServiceInstance si) {
        this.si = si;
    }

    //Get function for vHostName
    public String getVHostName() {
        return vHostName;
    }

    //Set function for vHostName
    public void setVHostName(String vHostName) {
        this.vHostName = vHostName;
    }

    public void addNewHost(String vHostName)
    {
    	HostConnectSpec hcs = new HostConnectSpec();
		
		hcs.setHostName(vHostName);
		hcs.setUserName("administrator");
		hcs.setPassword("12!@qwQW");
		
		ComputeResourceConfigSpec crcs = new ComputeResourceConfigSpec();
		Task task = null;
		Folder rootFolder = this.si.getRootFolder();
		try {
			ManagedEntity[] dcs = new InventoryNavigator(rootFolder).searchManagedEntities("Datacenter");
			
			Permission permission = new Permission();
			permission.setPropagate(true);
			permission.setEntity(this.si.getMOR());
			
			task = ((Datacenter)dcs[0]).getDatastoreFolder().addStandaloneHost_Task(hcs, crcs, true);
			
		} catch (InvalidProperty e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RuntimeFault e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
			try {
				if(task.waitForMe() == Task.SUCCESS){
					System.out.println("Host Created Succesfully");
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
			}
      }
    
    
      static VirtualDeviceConfigSpec createScsiSpec(int cKey)
      {
        VirtualDeviceConfigSpec scsiSpec = 
          new VirtualDeviceConfigSpec();
        scsiSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
        VirtualLsiLogicController scsiCtrl = 
            new VirtualLsiLogicController();
        scsiCtrl.setKey(cKey);
        scsiCtrl.setBusNumber(0);
        scsiCtrl.setSharedBus(VirtualSCSISharing.noSharing);
        scsiSpec.setDevice(scsiCtrl);
        return scsiSpec;
      }
      
      static VirtualDeviceConfigSpec createDiskSpec(String dsName, 
          int cKey, long diskSizeKB, String diskMode)
      {
        VirtualDeviceConfigSpec diskSpec = 
            new VirtualDeviceConfigSpec();
        diskSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
        diskSpec.setFileOperation(
            VirtualDeviceConfigSpecFileOperation.create);
        
        VirtualDisk vd = new VirtualDisk();
        vd.setCapacityInKB(diskSizeKB);
        diskSpec.setDevice(vd);
        vd.setKey(0);
        vd.setUnitNumber(0);
        vd.setControllerKey(cKey);

        VirtualDiskFlatVer2BackingInfo diskfileBacking = 
            new VirtualDiskFlatVer2BackingInfo();
        String fileName = "["+ dsName +"]";
        diskfileBacking.setFileName(fileName);
        diskfileBacking.setDiskMode(diskMode);
        diskfileBacking.setThinProvisioned(true);
        vd.setBacking(diskfileBacking);
        return diskSpec;
      }
      
      static VirtualDeviceConfigSpec createNicSpec(String netName, 
          String nicName) throws Exception
      {
        VirtualDeviceConfigSpec nicSpec = 
            new VirtualDeviceConfigSpec();
        nicSpec.setOperation(VirtualDeviceConfigSpecOperation.add);

        VirtualEthernetCard nic =  new VirtualPCNet32();
        VirtualEthernetCardNetworkBackingInfo nicBacking = 
            new VirtualEthernetCardNetworkBackingInfo();
        nicBacking.setDeviceName(netName);

        Description info = new Description();
        info.setLabel(nicName);
        info.setSummary(netName);
        nic.setDeviceInfo(info);
        
        // type: "generated", "manual", "assigned" by VC
        nic.setAddressType("generated");
        nic.setBacking(nicBacking);
        nic.setKey(0);
       
        nicSpec.setDevice(nic);
        return nicSpec;
      }
    
     public void recoverVMToLatestSnapshot()
 	 {
 		// Starting thread for reverting host
         RecoverVirtualMachine pthread2 = new RecoverVirtualMachine(this.si, this.vHostName, "VM");    	
         pthread2.start();
    	 
 	 }
      
     public void recoverVHost()
     {
    	try{
	    	 // Starting thread for reverting host
	         RecoverVirtualMachine pthread3 = new RecoverVirtualMachine(this.si, this.vHostName, "VHOST");    	
	         pthread3.start();
	         
    	}catch(Exception e){
    		
	    }
     }
     
}
