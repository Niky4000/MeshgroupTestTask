package ru.meshgroup.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.dao.UserDAO;
import ru.meshgroup.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserDAO userDAO;

    @Override
    public void insertUser(UserBean userBean) {
        userDAO.insertUser(userBean);
    }

}
