package ru.meshgroup.dao.impl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import ru.meshgroup.controller.bean.AccountBean;
import ru.meshgroup.controller.bean.MailBean;
import ru.meshgroup.controller.bean.PhoneBean;
import ru.meshgroup.controller.bean.UserBean;
import static ru.meshgroup.utils.DbUtils.createInMemoryDataSource;
import static ru.meshgroup.utils.DbUtils.getJdbcTemplate;
import static ru.meshgroup.utils.DbUtils.getTransactionManager;
import static ru.meshgroup.utils.DbUtils.liquibase;
import static ru.meshgroup.utils.DbUtils.shutdown;
import ru.meshgroup.utils.FieldUtil;

public class UserDAOImplTest {

    @Test
    public void test() throws LiquibaseException, SQLException {
        String userName = "name";
        UserDAOImpl userDAOImpl = new UserDAOImpl() {
            String getUserName() {
                return userName;
            }
        };
        DataSource dataSource = createInMemoryDataSource();
        try {
            liquibase(dataSource, "classpath:db/changelog/master.xml");
            PlatformTransactionManager transactionManager = getTransactionManager(dataSource);
            NamedParameterJdbcTemplate meshJdbcTemplate = getJdbcTemplate(dataSource);
            FieldUtil.setField(userDAOImpl, UserDAOImpl.class, meshJdbcTemplate, "meshJdbcTemplate");
            BigDecimal balance = BigDecimal.valueOf(200.82);
            userDAOImpl.insertUser(createUserBean(userName, balance));
            UserBean user = userDAOImpl.getUser(userDAOImpl.getUser(userName));
            Assert.assertTrue(user.getAccountBeanList().get(0).getBalance().equals(balance));
            user.getMailBeanList().remove(2);
            user.getPhoneBeanList().remove(2);
            Map<Long, MailBean> mailMap = user.getMailBeanList().stream().collect(Collectors.toMap(MailBean::getId, o -> o));
            Map<Long, PhoneBean> phoneMap = user.getPhoneBeanList().stream().collect(Collectors.toMap(PhoneBean::getId, o -> o));
            final String newMail = "newMail";
            mailMap.get(1L).setEmail(newMail);
            final String newPhone = "newPhone";
            phoneMap.get(1L).setPhone(newPhone);
            user.getMailBeanList().add(new MailBean(4L, user.getId(), "email4"));
            user.getMailBeanList().add(new MailBean(5L, user.getId(), "email5"));
            user.getPhoneBeanList().add(new PhoneBean(4L, user.getId(), "phone4"));
            user.getPhoneBeanList().add(new PhoneBean(5L, user.getId(), "phone5"));
            userDAOImpl.updateUser(user);
            UserBean user2 = userDAOImpl.getUser(userDAOImpl.getUser(userName));
            Assert.assertTrue(user2.getMailBeanList().size() == 4);
            Assert.assertTrue(user2.getPhoneBeanList().size() == 4);
            Map<Long, MailBean> mailMap2 = user2.getMailBeanList().stream().collect(Collectors.toMap(MailBean::getId, o -> o));
            Map<Long, PhoneBean> phoneMap2 = user2.getPhoneBeanList().stream().collect(Collectors.toMap(PhoneBean::getId, o -> o));
            Assert.assertTrue(mailMap2.get(1L).getEmail().equals(newMail));
            Assert.assertTrue(phoneMap2.get(1L).getPhone().equals(newPhone));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            shutdown(dataSource);
        }
    }

    private UserBean createUserBean(String userName, BigDecimal balance) {
        Long userId = 1L;
        UserBean userBean = new UserBean(userId, userName, LocalDate.of(2000, Month.APRIL, 28), "password");
        userBean.setAccountBeanList(Arrays.asList(new AccountBean(1L, userId, balance)));
        userBean.setMailBeanList(Arrays.asList(new MailBean(1L, userId, "email1"), new MailBean(2L, userId, "email2"), new MailBean(3L, userId, "email3")));
        userBean.setPhoneBeanList(Arrays.asList(new PhoneBean(1L, userId, "phone1"), new PhoneBean(2L, userId, "phone2"), new PhoneBean(3L, userId, "phone3")));
        return userBean;
    }
}
