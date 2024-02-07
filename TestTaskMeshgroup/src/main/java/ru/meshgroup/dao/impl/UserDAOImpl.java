package ru.meshgroup.dao.impl;

import java.sql.PreparedStatement;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.dao.UserDAO;

@Repository
public class UserDAOImpl implements UserDAO {

    @Autowired
    @Qualifier("meshDataSource")
    NamedParameterJdbcTemplate jdbc;

    @Transactional
    public void insertUser(UserBean userBean) {
        jdbc.update("insert into user (id, name, date_of_birth, password) values(:id,:name,:date,:password)", Map.of("id", userBean.getId(), "name", userBean.getName(), "date", userBean.getDateOfBirth(), "password", userBean.getPassword()));
    }
}
