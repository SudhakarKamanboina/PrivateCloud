package sjsu.services;

import sjsu.model.VirtualMachine;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.json.simple.JSONObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


import static sjsu.model.VirtualMachine.instance;


@Component
public class VirtualMachineService {

    @Autowired
    JdbcTemplate template;

    @Autowired
    ServiceInstance serviceInstance;

    @Value("${linux.template}")
    String linuxTemplate;

    @Value("${windows.template}")
    String windowsTemplate;


    public List<VirtualMachine> getAllVirtualMachines(String UIusername ) throws RemoteException {
        String sql= "Select vmname From privatecloud.usrvm Where username ='" + UIusername + "'";
        List<String> vmList = template.queryForList(sql, String.class);
        Folder rootFolder = serviceInstance.getRootFolder();
        ManagedEntity[] vms = new InventoryNavigator(rootFolder).searchManagedEntities(new String[][] { {"VirtualMachine", "name" }, }, true);

        List<com.vmware.vim25.mo.VirtualMachine> newLst = new ArrayList<com.vmware.vim25.mo.VirtualMachine>();
        for(String vmName: vmList)
        {
            for (int i = 0; i < vms.length; i++) {
                com.vmware.vim25.mo.VirtualMachine vm = (com.vmware.vim25.mo.VirtualMachine) vms[i];
                if(vmName.equals(vm.getName()))
                    newLst.add(vm);
            }
        }

        //return Arrays.asList(newLst).stream().map(vm-> instance(vm)).collect(Collectors.toList());
        List<VirtualMachine> finalList = new ArrayList<VirtualMachine>();
        for(com.vmware.vim25.mo.VirtualMachine vm: newLst)
        {
           finalList.add(VirtualMachine.instance(vm));
        }
        return finalList;
    }

    public void powerOnVM(String vmName) throws RemoteException, InterruptedException {
        Folder rootFolder = serviceInstance.getRootFolder();

        InventoryNavigator inv = new InventoryNavigator(
                serviceInstance.getRootFolder());
        com.vmware.vim25.mo.VirtualMachine vm = null;

        vm = (com.vmware.vim25.mo.VirtualMachine) inv.searchManagedEntity("VirtualMachine", vmName);


        if (vm == null) {
            serviceInstance.getServerConnection().logout();
            throw new VMNotFoundException("VM With the name does not exists.");
        }
        VirtualMachineRuntimeInfo vmri = (VirtualMachineRuntimeInfo) vm.getRuntime();
        if (vmri.getPowerState() == VirtualMachinePowerState.poweredOff) {
            Task task = vm.powerOnVM_Task(null);

            task.waitForTask();

            System.out.println("vm:" + vm.getName() + " powered on.");
        }
    }
    public void powerOffVM(String vmName) throws RemoteException, InterruptedException {
        Folder rootFolder = serviceInstance.getRootFolder();
        com.vmware.vim25.mo.VirtualMachine virtualMachine = (com.vmware.vim25.mo.VirtualMachine)new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmName);
        if(virtualMachine!=null){
            VirtualMachinePowerState powerState = virtualMachine.getRuntime().getPowerState();
            if(powerState.equals(VirtualMachinePowerState.poweredOn)){
                virtualMachine.powerOffVM_Task().waitForTask();
            }
        }else{
            throw new VMNotFoundException(String.format("VM with the name %s doesn't exist",vmName));
        }
    }

    public void deployVM(String type, String vmName, String UIusername) throws Exception{
        String templateName = type.equals("linux")?linuxTemplate:windowsTemplate;
        //String templateName = "VM-CLI-01";
        System.out.println("Deploying VM with Template: "+templateName);
        Folder rootFolder = serviceInstance.getRootFolder();
        com.vmware.vim25.mo.VirtualMachine vm = (com.vmware.vim25.mo.VirtualMachine) new InventoryNavigator(
                rootFolder).searchManagedEntity(
                "VirtualMachine", templateName);
        if(vm==null)
        {
            System.out.println("No template " + templateName + " found");
            return;
        }
        else{
            System.out.println("NAME: "+vm.getName());
        }
        //DC and Resouce pool config
        VirtualMachineRelocateSpec locationSpec = new VirtualMachineRelocateSpec();
        Datacenter dc = (Datacenter) new InventoryNavigator(rootFolder).searchManagedEntity("Datacenter", "T14-DC");
        Datastore[] dStore = dc.getDatastores();
        System.out.println("Datastore: "+dStore[0].getSummary().name);
        ResourcePool rp = (ResourcePool) new InventoryNavigator(
                dc).searchManagedEntities("ResourcePool")[0];
        locationSpec.setPool(rp.getMOR());
        locationSpec.setDatastore(dStore[0].getMOR());
        //Clone config
        VirtualMachineCloneSpec cloneSpec =
                new VirtualMachineCloneSpec();
        cloneSpec.setLocation(locationSpec);
        cloneSpec.setPowerOn(true);
        cloneSpec.setTemplate(false);

        Task task = vm.cloneVM_Task((Folder) vm.getParent(),
                vmName, cloneSpec);
        System.out.println("Launching the VM clone task. " +
                "Please wait ...");
        String status = task.waitForMe();
        if(task.waitForMe()==Task.SUCCESS){
            System.out.println("VM got cloned successfully.");
            String sql = String.format("insert into privatecloud.usrvm(username, vmname) VALUES('%s','%s')",UIusername, vmName);
            template.execute(sql);
        }
        else {
            System.out.println("Failure -: VM cannot be cloned");
        }

    }
    
    public JSONObject getStats(String vmName) throws RemoteException, InterruptedException {
        JSONObject json= new JSONObject();

        String sql1 = "SELECT overall_cpu_usage FROM privatecloud.stats WHERE vm_name ='" + vmName + "' ORDER BY id DESC LIMIT 12";
        List<String> cpu_usage = template.queryForList(sql1,String.class);
        json.put("cpu_usage", cpu_usage);

        String sql2 = "SELECT guest_memory_usage FROM privatecloud.stats WHERE vm_name ='" + vmName + "' ORDER BY id LIMIT 12";
        List<String> guest_memory_usage = template.queryForList(sql2, String.class);
        json.put("guest_memory_usage", guest_memory_usage);

        String sql3 = "SELECT host_memory_usage FROM privatecloud.stats WHERE vm_name ='" + vmName + "' ORDER BY id LIMIT 12";
        List<String> host_memory_usage = template.queryForList(sql3, String.class);
        json.put("host_memory_usage", host_memory_usage);

        return json;
    }
}
