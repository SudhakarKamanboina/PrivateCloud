package sjsu.controller;

import sjsu.model.VirtualMachine;
import sjsu.services.VirtualMachineService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.rmi.RemoteException;
import java.util.Arrays;

/**
 * Created by sudh on 4/5/2015.
 */

@Controller
@RequestMapping("/api/v1")
public class VirtualMachineController {

    @Autowired
    VirtualMachineService virtualMachineService;
        
    @RequestMapping(value = "/vms", method=RequestMethod.GET)
    @ResponseBody
    public List<VirtualMachine>  getVms(HttpServletRequest req,HttpServletResponse response) throws RemoteException {
        Cookie[] cookies = req.getCookies();
        String username=null;
        for (int i = 0; i < cookies.length; i++) {
            if("username".equals(cookies[i].getName())) {
                username = cookies[i].getValue();
            }
        }
        List<VirtualMachine> allVirtualMachines = virtualMachineService.getAllVirtualMachines(username);
        if(allVirtualMachines==null||allVirtualMachines.isEmpty()){
            throw new NoContentException("No Virtual Machines Found!");
        }
        return allVirtualMachines;
    }

    @ExceptionHandler(value={RemoteException.class,InterruptedException.class})
    public ResponseEntity<String> handleException(){
       HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json"));
        return new ResponseEntity<String>("{\"error\":\"Please contact service provider\"}",headers,HttpStatus.SERVICE_UNAVAILABLE);
    }


    @RequestMapping(value = "/vms/{vm-name}/on", method=RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String>  powerOnVM(@PathVariable("vm-name") String vmName) throws RemoteException, InterruptedException {

        virtualMachineService.powerOnVM(vmName);

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @RequestMapping(value = {"/vms/{vm-name}/off"}, method = RequestMethod.POST)
    public ResponseEntity<String> powerOffVM(@PathVariable("vm-name") String vmName) throws RemoteException, InterruptedException {
        virtualMachineService.powerOffVM(vmName);
        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @RequestMapping(value = {"/vms/create/{vm-type}/{vm-name}"}, method = RequestMethod.GET)
    public ResponseEntity<String> createVm(@PathVariable("vm-type") String vmType, @PathVariable("vm-name") String vmNameUI, HttpServletRequest req,HttpServletResponse response) throws RemoteException, InterruptedException {
       String vmName = vmNameUI;
        try {
            Cookie[] cookies = req.getCookies();
            String username=null;
            for (int i = 0; i < cookies.length; i++) {
                if("username".equals(cookies[i].getName())) {
                    username = cookies[i].getValue();
                }
            }
            virtualMachineService.deployVM(vmType,vmName, username);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @RequestMapping(value = {"/getStats/{vmName}"}, method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public JSONObject GetVMStats(@PathVariable("vmName") String vmName) throws RemoteException, InterruptedException {
            return virtualMachineService.getStats(vmName);
    }

}
