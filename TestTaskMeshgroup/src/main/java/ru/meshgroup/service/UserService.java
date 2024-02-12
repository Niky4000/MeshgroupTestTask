package ru.meshgroup.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.controller.exceptions.MoneyException;

public interface UserService {

    public List<UserBean> getUserList(String name, LocalDate dateOfBirth, String email, String phone, int size, int offset);
    
    public void insertUser(UserBean userBean);

    public UserBean getUser(String name);

    public void updateUser(UserBean userBean);

    public void transferMoney(Long userIdFrom, Long userIdTo, BigDecimal money) throws MoneyException;

    public void updateAllAccounts(double k);
}
