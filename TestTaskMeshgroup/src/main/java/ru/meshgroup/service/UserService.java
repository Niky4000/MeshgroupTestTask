package ru.meshgroup.service;

import ru.meshgroup.controller.bean.UserBean;

public interface UserService {

    public void insertUser(UserBean userBean);

    public void updateUser(UserBean userBean);
}
