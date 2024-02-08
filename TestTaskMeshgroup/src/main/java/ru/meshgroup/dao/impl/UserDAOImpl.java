package ru.meshgroup.dao.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.meshgroup.controller.bean.AccountBean;
import ru.meshgroup.controller.bean.LinkedBean;
import ru.meshgroup.controller.bean.MailBean;
import ru.meshgroup.controller.bean.PhoneBean;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.dao.UserDAO;
import static ru.meshgroup.utils.DateUtils.toLocalDateFromSql;
import ru.meshgroup.utils.SecurityUtils;
import static ru.meshgroup.utils.SecurityUtils.getUserName;

@Repository
public class UserDAOImpl implements UserDAO {

    @Autowired
    @Qualifier("meshJdbc")
    NamedParameterJdbcTemplate meshJdbcTemplate;

    @Override
    @Transactional
    public void insertUser(UserBean userBean) {
        meshJdbcTemplate.update("insert into users (id, name, date_of_birth, password) values(:id,:name,:date,:password)", Map.of("id", userBean.getId(), "name", userBean.getName(), "date", userBean.getDateOfBirth(), "password", userBean.getPassword()));
        userBean.getAccountBeanList().forEach(accountBean -> meshJdbcTemplate.update("insert into account (id, user_id, balance) values(:id,:userId,:balance)", Map.of("id", accountBean.getId(), "userId", accountBean.getUserId(), "balance", accountBean.getBalance())));
        userBean.getMailBeanList().forEach(mail -> meshJdbcTemplate.update("insert into email_data (id, user_id, email) values(:id,:userId,:email)", Map.of("id", mail.getId(), "userId", mail.getUserId(), "email", mail.getEmail())));
        userBean.getPhoneBeanList().forEach(phone -> meshJdbcTemplate.update("insert into phone_data (id, user_id, phone) values(:id,:userId,:phone)", Map.of("id", phone.getId(), "userId", phone.getUserId(), "phone", phone.getPhone())));
    }

    @Override
    public UserBean getUser(String name) {
        UserBean user = Optional.ofNullable(meshJdbcTemplate.query("select id,date_of_birth,password from users where name=:name", Map.of("name", name), (rs, rowNum) -> {
            return new UserBean(rs.getLong("id"), name, toLocalDateFromSql(rs.getDate("date_of_birth")), rs.getString("password"));
        }).stream().collect(Collectors.toList())).filter(l -> l.size() == 1).map(l -> l.get(0)).orElse(null);
        return user;
    }

    public UserBean getUser(UserBean userBean) {
        userBean.setAccountBeanList(getAccountBeanList(userBean.getId()));
        userBean.setMailBeanList(getMailBeanList(userBean.getId()));
        userBean.setPhoneBeanList(getPhoneBeanList(userBean.getId()));
        return userBean;
    }

    @Override
    @Transactional
    public void updateUser(UserBean userBean) {
        int update = meshJdbcTemplate.update("update users set name=:name,date_of_birth=:date,password=:password where id=:id and name=:userName", Map.of("id", userBean.getId(), "name", userBean.getName(), "date", userBean.getDateOfBirth(), "password", userBean.getPassword(), "userName", getUserName()));
        if (update == 0) {
            throw new RuntimeException("User was not updated!");
        }
        if (userBean.getMailBeanList() != null && !userBean.getMailBeanList().isEmpty()) {
            List<MailBean> mailBeanList = getMailBeanList(userBean.getId());
            updateInsertOrDelete("email_data", "email", userBean.getMailBeanList(), mailBeanList.stream().collect(Collectors.toMap(MailBean::getId, o -> o)));
        }
        if (userBean.getPhoneBeanList() != null && !userBean.getPhoneBeanList().isEmpty()) {
            List<PhoneBean> phoneBeanList = getPhoneBeanList(userBean.getId());
            updateInsertOrDelete("phone_data", "phone", userBean.getPhoneBeanList(), phoneBeanList.stream().collect(Collectors.toMap(PhoneBean::getId, o -> o)));
        }
    }

    String getUserName() {
        return SecurityUtils.getUserName();
    }

    private <T extends LinkedBean> void updateInsertOrDelete(String tableName, String fieldName, List<T> beanList, Map<Long, T> beanListFromDb) {
        for (LinkedBean bean : beanList) {
            LinkedBean dbBean = beanListFromDb.get(bean.getId());
            if (dbBean != null) {
                if (!dbBean.getValue().equals(bean.getValue())) {
                    update(tableName, fieldName, new TreeMap<>(Map.of("userId", bean.getUserId(), "id", bean.getId(), "value", bean.getValue())));
                }
                beanListFromDb.remove(bean.getId());
            } else {
                insert(tableName, new TreeMap<>(Map.of("user_id", bean.getUserId(), "id", bean.getId(), fieldName, bean.getValue())));
            }
        }
        for (LinkedBean bean : beanListFromDb.values()) {
            delete(tableName, new TreeMap<>(Map.of("userId", bean.getUserId(), "id", bean.getId())));
        }
    }

    private List<AccountBean> getAccountBeanList(Long userId) {
        List<AccountBean> mailBeanList = meshJdbcTemplate.query("select id,balance from account where user_id=:userId", Map.of("userId", userId), (rs, rowNum) -> {
            return new AccountBean(rs.getLong("id"), userId, rs.getBigDecimal("balance"));
        }).stream().collect(Collectors.toList());
        return mailBeanList;
    }

    private List<MailBean> getMailBeanList(Long userId) {
        List<MailBean> mailBeanList = meshJdbcTemplate.query("select id,email from email_data where user_id=:userId", Map.of("userId", userId), (rs, rowNum) -> {
            return new MailBean(rs.getLong("id"), userId, rs.getString("email"));
        }).stream().collect(Collectors.toList());
        return mailBeanList;
    }

    private List<PhoneBean> getPhoneBeanList(Long userId) {
        List<PhoneBean> phoneBeanList = meshJdbcTemplate.query("select id,phone from phone_data where user_id=:userId", Map.of("userId", userId), (rs, rowNum) -> {
            return new PhoneBean(rs.getLong("id"), userId, rs.getString("phone"));
        }).stream().collect(Collectors.toList());
        return phoneBeanList;
    }

    private int insert(String tableName, TreeMap<String, Object> parameters) {
        return meshJdbcTemplate.update(new StringBuilder("insert into ").append(tableName).append(" (").append(parameters.keySet().stream().reduce((p1, p2) -> p1 + "," + p2).get()).append(")").append(" values(").append(":").append(parameters.keySet().stream().reduce((p1, p2) -> p1 + ",:" + p2).get()).append(")").toString(), parameters);
    }

    private int update(String tableName, String fieldName, Map<String, Object> parameters) {
        return meshJdbcTemplate.update(new StringBuilder("update ").append(tableName).append(" set ").append(fieldName).append("=:value where id=:id and user_id=:userId").toString(), parameters);
    }

    private int delete(String tableName, Map<String, Object> parameters) {
        return meshJdbcTemplate.update(new StringBuilder("delete from ").append(tableName).append(" where id=:id and user_id=:userId").toString(), parameters);
    }
}
