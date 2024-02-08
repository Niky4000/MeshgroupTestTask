package ru.meshgroup.auth.service;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.dao.UserDAO;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserDAO userDAO;
    private static final String ADMIN_USER = "meshgroup_user";
    private static final String ADMIN_PASSWORD = "password";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (ADMIN_USER.equals(username)) {
            return new User(ADMIN_USER, passwordEncoder.encode(ADMIN_PASSWORD), new ArrayList<>());
        } else {
            UserBean user = userDAO.getUser(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found with username: " + username);
            } else {
                return new User(username, passwordEncoder.encode(user.getPassword()), new ArrayList<>());
            }
        }
    }
}
