package sjsu.controller;

import sjsu.model.User;
import sjsu.repository.SignUpException;
import sjsu.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by sudh on 4/5/2015.
 */

@Controller
public class ProfileController {

    @Autowired
    UserService signupService;

    @RequestMapping(value = {"/profile"},method = RequestMethod.POST)
    public ModelAndView createProfile(ModelAndView model,@ModelAttribute User user,BindingResult result){
        if(result.hasErrors()){
            model.setViewName("error");
            return model;
        }
        try {
            signupService.signUpUser(user);
        } catch (SignUpException ex) {
        	ex.printStackTrace();
            model.setViewName("signup");
            model.addObject("error",ex.getMessage());
            return model;
        }
        model.setViewName("login");
        return model;
    }
}
