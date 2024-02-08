package ru.meshgroup.dao;

import ru.meshgroup.controller.bean.UserBean;

public interface UserDAO {

    public void insertUser(UserBean userBean);

    public UserBean getUser(String name);

    public void updateUser(UserBean userBean);
}
