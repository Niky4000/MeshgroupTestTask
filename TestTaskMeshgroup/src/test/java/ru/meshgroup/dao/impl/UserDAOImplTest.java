package ru.meshgroup.dao.impl;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import liquibase.exception.LiquibaseException;
import org.junit.Assert;
import org.junit.Test;
import ru.meshgroup.controller.bean.MailBean;
import ru.meshgroup.controller.bean.PhoneBean;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.controller.exceptions.MoneyException;
import ru.meshgroup.test.utils.InitUtils;

public class UserDAOImplTest extends InitUtils {

    @Test
    public void test() throws LiquibaseException, SQLException {
        String userName = "name";
        execute(() -> new UserDAOImpl() {
            @Override
            String getUserName() {
                return userName;
            }
        }, userDAOImpl -> {
            BigDecimal balance = BigDecimal.valueOf(200.82);
            userDAOImpl.insertUser(createUserBean(userName, balance));
            UserBean user = userDAOImpl.getUser(userDAOImpl.getUserByName(userName));
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
            UserBean user2 = userDAOImpl.getUser(userDAOImpl.getUserByName(userName));
            Assert.assertTrue(user2.getMailBeanList().size() == 4);
            Assert.assertTrue(user2.getPhoneBeanList().size() == 4);
            Map<Long, MailBean> mailMap2 = user2.getMailBeanList().stream().collect(Collectors.toMap(MailBean::getId, o -> o));
            Map<Long, PhoneBean> phoneMap2 = user2.getPhoneBeanList().stream().collect(Collectors.toMap(PhoneBean::getId, o -> o));
            Assert.assertTrue(mailMap2.get(1L).getEmail().equals(newMail));
            Assert.assertTrue(phoneMap2.get(1L).getPhone().equals(newPhone));
        });
    }

    @Test
    public void readUserTest() throws LiquibaseException, SQLException {
        execute(() -> new UserDAOImpl(), userDAOImpl -> {
            userDAOImpl.insertUser(createUserBean2(1L, LocalDate.of(2000, Month.APRIL, 28), "name", BigDecimal.valueOf(200.82), 1, 2, 3, 4));
            userDAOImpl.insertUser(createUserBean2(2L, LocalDate.of(2001, Month.APRIL, 28), "name2", BigDecimal.valueOf(400.82), 5, 6, 7, 8, 9, 10, 11, 12));
            userDAOImpl.insertUser(createUserBean2(3L, LocalDate.of(2002, Month.APRIL, 28), "name3", BigDecimal.valueOf(800.82), 13, 14));
            userDAOImpl.insertUser(createUserBean2(4L, LocalDate.of(2003, Month.APRIL, 28), "name4", BigDecimal.valueOf(1600.82), 15, 16, 17, 18, 19, 20, 21, 22, 23, 24));
            UserBean user = userDAOImpl.getUser(userDAOImpl.getUserByName("name"));
            List<UserBean> userList = userDAOImpl.getUserList("name", null, null, null, 0, 2);
            Assert.assertTrue(userList.size() == 2 && userList.stream().allMatch(u -> Set.of(1L, 2L).contains(u.getId())));
            List<UserBean> userList2 = userDAOImpl.getUserList("name", null, null, null, 2, 4);
            Assert.assertTrue(userList2.size() == 2 && userList2.stream().allMatch(u -> Set.of(3L, 4L).contains(u.getId())));
            List<UserBean> userList3 = userDAOImpl.getUserList("name2", null, null, null, 0, 128);
            Assert.assertTrue(userList3.size() == 1 && userList3.get(0).getId().equals(2L));
            List<UserBean> userList4 = userDAOImpl.getUserList("name", LocalDate.of(2000, Month.APRIL, 30), null, null, 0, 128);
            Assert.assertTrue(userList4.size() == 3 && userList4.stream().allMatch(u -> Set.of(2L, 3L, 4L).contains(u.getId())));
            List<UserBean> userList5 = userDAOImpl.getUserList("name", LocalDate.of(2000, Month.APRIL, 30), "email14", null, 0, 128);
            Assert.assertTrue(userList5.size() == 1 && userList5.get(0).getId().equals(3L));
            List<UserBean> userList6 = userDAOImpl.getUserList("name", LocalDate.of(2000, Month.APRIL, 30), null, "phone24", 0, 128);
            Assert.assertTrue(userList6.size() == 1 && userList6.get(0).getId().equals(4L));
            List<UserBean> userList7 = userDAOImpl.getUserList("name", LocalDate.of(2000, Month.APRIL, 30), "email10", "phone10", 0, 128);
            Assert.assertTrue(userList7.size() == 1 && userList7.get(0).getId().equals(2L));
        });
    }

    @Test
    public void transferMoneyTest() throws LiquibaseException, SQLException {
        execute(() -> new UserDAOImpl() {
            @Override
            String forUpdate() {
                return "";
            }
        }, userDAOImpl -> {
            try {
                final BigDecimal money1 = BigDecimal.valueOf(200.82);
                final BigDecimal moneyToTransfer = BigDecimal.valueOf(100);
                final BigDecimal money2 = money1.add(moneyToTransfer.multiply(BigDecimal.valueOf(2)));
                userDAOImpl.insertUser(createUserBean2(1L, LocalDate.of(2000, Month.APRIL, 28), "name", money1, 1, 2, 3, 4));
                userDAOImpl.insertUser(createUserBean2(2L, LocalDate.of(2001, Month.APRIL, 28), "name2", money2, 5, 6, 7, 8, 9, 10, 11, 12));
                userDAOImpl.transferMoney(1L, 2L, moneyToTransfer);
                UserBean user1 = userDAOImpl.getUser(userDAOImpl.getUserByName("name"));
                UserBean user2 = userDAOImpl.getUser(userDAOImpl.getUserByName("name2"));
                Assert.assertTrue(user1.getAccountBeanList().get(0).getBalance().equals(BigDecimal.valueOf(100.82)));
                Assert.assertTrue(user2.getAccountBeanList().get(0).getBalance().equals(BigDecimal.valueOf(500.82)));
                userDAOImpl.transferMoney(2L, 1L, moneyToTransfer.multiply(BigDecimal.valueOf(2)));
                UserBean user3 = userDAOImpl.getUser(userDAOImpl.getUserByName("name"));
                UserBean user4 = userDAOImpl.getUser(userDAOImpl.getUserByName("name2"));
                Assert.assertTrue(user3.getAccountBeanList().get(0).getBalance().equals(user4.getAccountBeanList().get(0).getBalance()));
            } catch (MoneyException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
