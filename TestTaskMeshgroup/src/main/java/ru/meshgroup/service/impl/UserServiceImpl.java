package ru.meshgroup.service.impl;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.controller.exceptions.MoneyException;
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

    @Override
    public void updateUser(UserBean userBean) {
        userDAO.updateUser(userBean);
    }

    @Override
    public UserBean getUser(String name) {
        return userDAO.getUser(name);
    }

    @Override
    public void transferMoney(Long userIdFrom, Long userIdTo, BigDecimal money) throws MoneyException {
        userDAO.transferMoney(userIdFrom, userIdTo, money);
    }

    @Override
    public void updateAllAccounts(double k) {
        userDAO.updateAllAccounts(k);
    }
}
