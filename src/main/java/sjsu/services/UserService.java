package sjsu.services;

import sjsu.model.User;
import sjsu.repository.SignUpException;
import sjsu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class UserService {
    @Autowired
    UserRepository userRepository;
    public void signUpUser(User user) throws SignUpException {
        final String time = new Long(new Date().getTime()).toString();
        user.setId(Integer.valueOf(time.substring(time.length()-6,time.length())));
        userRepository.save(user);
    }

    public User find(String username,String password) {
        return userRepository.findByUsername(username);
    }
}
