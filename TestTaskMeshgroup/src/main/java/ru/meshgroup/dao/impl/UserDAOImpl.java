package ru.meshgroup.dao.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.meshgroup.controller.bean.AccountBean;
import ru.meshgroup.controller.bean.LinkedBean;
import ru.meshgroup.controller.bean.MailBean;
import ru.meshgroup.controller.bean.PhoneBean;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.controller.exceptions.AccountIsLockedException;
import ru.meshgroup.controller.exceptions.MoneyException;
import ru.meshgroup.dao.UserDAO;
import static ru.meshgroup.utils.DateUtils.toLocalDateFromSql;
import ru.meshgroup.utils.SecurityUtils;

@Slf4j
@Repository
public class UserDAOImpl implements UserDAO {

    @Autowired
    @Qualifier("meshJdbc")
    NamedParameterJdbcTemplate meshJdbcTemplate;

    @Override
    public List<UserBean> getUserList(String name, LocalDate dateOfBirth, String email, String phone, int offset, int size) {
        StringBuilder query = new StringBuilder("select u.id,u.name,u.date_of_birth,u.password from users u");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("size", size);
        parameters.put("offset", offset);
        boolean wasAnd = false;
        if (name != null || dateOfBirth != null || email != null || phone != null) {
            query.append(" where");
        }
        if (name != null) {
            wasAnd = wasAnd(query, wasAnd);
            query.append(" u.name like :name");
            parameters.put("name", name + "%");
        }
        if (dateOfBirth != null) {
            wasAnd = wasAnd(query, wasAnd);
            query.append("  u.date_of_birth >= :dateOfBirth");
            parameters.put("dateOfBirth", dateOfBirth);
        }
        if (email != null) {
            wasAnd = wasAnd(query, wasAnd);
            query.append(" exists (select 1 from email_data m where u.id=m.user_id and m.email=:email)");
            parameters.put("email", email);
        }
        if (phone != null) {
            wasAnd = wasAnd(query, wasAnd);
            query.append(" exists (select 1 from phone_data p where u.id=p.user_id and p.phone=:phone)");
            parameters.put("phone", phone);
        }
        query.append(" order by u.id limit :size offset :offset");
        List<UserBean> userList = meshJdbcTemplate.query(query.toString(), parameters, getUserRowMapper()).stream().collect(Collectors.toList());
        return userList;
    }

    private boolean wasAnd(StringBuilder query, boolean wasAnd) {
        if (wasAnd) {
            query.append(" and");
        }
        return true;
    }

    @Override
    @Transactional
    public void insertUser(UserBean userBean) {
        meshJdbcTemplate.update("insert into users (id, name, date_of_birth, password) values(:id,:name,:date,:password)", Map.of("id", userBean.getId(), "name", userBean.getName(), "date", userBean.getDateOfBirth(), "password", userBean.getPassword()));
        userBean.getAccountBeanList().forEach(accountBean -> meshJdbcTemplate.update("insert into account (id, user_id, balance) values(:id,:userId,:balance)", Map.of("id", accountBean.getId(), "userId", accountBean.getUserId(), "balance", accountBean.getBalance())));
        userBean.getMailBeanList().forEach(mail -> meshJdbcTemplate.update("insert into email_data (id, user_id, email) values(:id,:userId,:email)", Map.of("id", mail.getId(), "userId", mail.getUserId(), "email", mail.getEmail())));
        userBean.getPhoneBeanList().forEach(phone -> meshJdbcTemplate.update("insert into phone_data (id, user_id, phone) values(:id,:userId,:phone)", Map.of("id", phone.getId(), "userId", phone.getUserId(), "phone", phone.getPhone())));
    }

    @Override
    public UserBean getUserByName(String name) {
        UserBean user = Optional.ofNullable(meshJdbcTemplate.query("select id,name,date_of_birth,password from users where name=:name", Map.of("name", name), getUserRowMapper()).stream().collect(Collectors.toList())).filter(l -> l.size() == 1).map(l -> l.get(0)).orElse(null);
        return user;
    }

    private RowMapper<UserBean> getUserRowMapper() {
        return (rs, rowNum) -> new UserBean(rs.getLong("id"), rs.getString("name"), toLocalDateFromSql(rs.getDate("date_of_birth")), rs.getString("password"));
    }

    @Override
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
        List<AccountBean> accountBeanList = meshJdbcTemplate.query("select id,user_id,balance from account where user_id=:userId", Map.of("userId", userId), accountRowMapper()).stream().collect(Collectors.toList());
        return accountBeanList;
    }

    private RowMapper<AccountBean> accountRowMapper() {
        return (rs, rowNum) -> new AccountBean(rs.getLong("id"), rs.getLong("user_id"), rs.getBigDecimal("balance"));
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

    int insert(String tableName, TreeMap<String, Object> parameters) {
        return meshJdbcTemplate.update(new StringBuilder("insert into ").append(tableName).append(" (").append(parameters.keySet().stream().reduce((p1, p2) -> p1 + "," + p2).get()).append(")").append(" values(").append(":").append(parameters.keySet().stream().reduce((p1, p2) -> p1 + ",:" + p2).get()).append(")").toString(), parameters);
    }

    int update(String tableName, String fieldName, Map<String, Object> parameters) {
        return meshJdbcTemplate.update(new StringBuilder("update ").append(tableName).append(" set ").append(fieldName).append("=:value where id=:id and user_id=:userId").toString(), parameters);
    }

    int delete(String tableName, Map<String, Object> parameters) {
        return meshJdbcTemplate.update(new StringBuilder("delete from ").append(tableName).append(" where id=:id and user_id=:userId").toString(), parameters);
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void transferMoney(Long userIdFrom, Long userIdTo, BigDecimal money) throws MoneyException {
        AccountBean accountBeanFrom;
        AccountBean accountBeanTo;
        log.debug("transferMoney started!");
        if (userIdFrom.compareTo(userIdTo) < 0) {
            accountBeanFrom = getLockedAccountBean(userIdFrom);
            accountBeanTo = getLockedAccountBean(userIdTo);
        } else {
            accountBeanTo = getLockedAccountBean(userIdTo);
            accountBeanFrom = getLockedAccountBean(userIdFrom);
        }
        waitSomeTime(10000);
        if (accountBeanFrom != null && accountBeanTo != null && accountBeanFrom.getBalance().add(money.negate()).compareTo(BigDecimal.ZERO) >= 0) {
            log.debug("update " + userIdFrom + " started!");
            update("account", "balance", new TreeMap<>(Map.of("userId", userIdFrom, "id", accountBeanFrom.getId(), "value", accountBeanFrom.getBalance().add(money.negate()))));
            log.debug("update " + userIdFrom + " finished!");
            log.debug("update " + userIdTo + " started!");
            update("account", "balance", new TreeMap<>(Map.of("userId", userIdTo, "id", accountBeanTo.getId(), "value", accountBeanTo.getBalance().add(money))));
            log.debug("update " + userIdTo + " finished!");
        } else if (accountBeanFrom == null && accountBeanTo == null) {
            throw new MoneyException("It's impossible to transfer money because both accounts aren't found!");
        } else if (accountBeanFrom == null) {
            throw new MoneyException("It's impossible to transfer money because source account " + accountBeanFrom + " isn't found!");
        } else if (accountBeanTo == null) {
            throw new MoneyException("It's impossible to transfer money because target account " + accountBeanTo + " isn't found!");
        } else {
            throw new MoneyException("It's impossible to transfer money because balance of " + userIdFrom + " will be negative!");
        }
        log.debug("transferMoney finished!");
    }

    private void waitSomeTime(int timeToWait) {
        while (true) {
            try {
                Thread.sleep(timeToWait);
                break;
            } catch (InterruptedException ex) {
                Logger.getLogger(UserDAOImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private AccountBean getLockedAccountBean(Long userId) throws DataAccessException {
        try {
            return Optional.ofNullable(meshJdbcTemplate.query(getAccountUpdateQuery(), Map.of("userId", userId), accountRowMapper()).stream().collect(Collectors.toList())).filter(l -> l.size() == 1).map(l -> l.get(0)).orElse(null);
        } catch (CannotSerializeTransactionException e) {
            throw new AccountIsLockedException(userId);
        }
    }

    String getAccountUpdateQuery() {
        return "select id,user_id,balance from account where user_id=:userId" + forUpdate();
    }

    String forUpdate() {
        return " for update";
    }

    private int DEFAULT_SIZE = 2048;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateAllAccounts(double k) {
        int offset = 0;
        List<AccountBean> accountBeanList = null;
        do {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("size", getSize());
            parameters.put("offset", offset);
            accountBeanList = meshJdbcTemplate.query("select id,user_id,balance from account order by id limit :size offset :offset", parameters, accountRowMapper());
            if (!accountBeanList.isEmpty()) {
                List<Long> idList = accountBeanList.stream().map(AccountBean::getId).collect(Collectors.toList());
                Map<String, Object> parameters2 = Map.of("k", k, "idList", idList);
                meshJdbcTemplate.update("update account set balance=balance*:k where id in(:idList)", parameters2);
                offset += getSize();
            }
        } while (!accountBeanList.isEmpty());
    }

    protected int getSize() {
        return DEFAULT_SIZE;
    }
}
