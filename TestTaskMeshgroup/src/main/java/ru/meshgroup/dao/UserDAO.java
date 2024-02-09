package ru.meshgroup.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ru.meshgroup.controller.bean.UserBean;

public interface UserDAO {

    public List<UserBean> getUserList(String name, LocalDate dateOfBirth, String email, String phone, int size, int offset);

    public void insertUser(UserBean userBean);

    public UserBean getUser(String name);

    public UserBean getUser(UserBean userBean);

    public void updateUser(UserBean userBean);

    public void transferMoney(Long userIdFrom, Long userIdTo, BigDecimal money);
}
