package ru.meshgroup.scheduled;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import liquibase.exception.LiquibaseException;
import org.junit.Assert;
import org.junit.Test;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.dao.impl.UserDAOImpl;
import static ru.meshgroup.scheduled.ScheduledTask.SKIP_VALUE_BACK_ITERATIONS_AMOUNT;
import ru.meshgroup.service.UserService;
import ru.meshgroup.service.impl.UserServiceImpl;
import ru.meshgroup.test.utils.InitUtils;
import ru.meshgroup.utils.FieldUtil;

public class ScheduledTaskTest extends InitUtils {

    @Test
    public void test() throws LiquibaseException, SQLException {
        long kk = 2;
        UserDAOImpl userDAO = new UserDAOImpl() {
            @Override
            protected int getSize() {
                return 2;
            }
        };
        UserServiceImpl userService = new UserServiceImpl() {
            @Override
            public void updateAllAccounts(double k) {
                userDAO.updateAllAccounts(kk);
            }
        };
        FieldUtil.setField(userService, UserServiceImpl.class, userDAO, "userDAO");
        ScheduledTask task = new ScheduledTask();
        FieldUtil.setField(task, userService, "userService");
        execute(() -> userDAO, userDAOImpl -> {
            BigDecimal money1 = BigDecimal.valueOf(200.82);
            BigDecimal money2 = BigDecimal.valueOf(400.82);
            BigDecimal money3 = BigDecimal.valueOf(800.82);
            BigDecimal money4 = BigDecimal.valueOf(1600.82);
            userDAOImpl.insertUser(createUserBean2(1L, LocalDate.of(2000, Month.APRIL, 28), "name", money1, 1, 2, 3, 4));
            userDAOImpl.insertUser(createUserBean2(2L, LocalDate.of(2001, Month.APRIL, 28), "name2", money2, 5, 6, 7, 8, 9, 10, 11, 12));
            userDAOImpl.insertUser(createUserBean2(3L, LocalDate.of(2002, Month.APRIL, 28), "name3", money3, 13, 14));
            userDAOImpl.insertUser(createUserBean2(4L, LocalDate.of(2003, Month.APRIL, 28), "name4", money4, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24));
            userDAOImpl.insertUser(createUserBean2(5L, LocalDate.of(2004, Month.APRIL, 28), "name5", money1, 25));
            userDAOImpl.insertUser(createUserBean2(6L, LocalDate.of(2005, Month.APRIL, 28), "name6", money2, 26));
            userDAOImpl.insertUser(createUserBean2(7L, LocalDate.of(2006, Month.APRIL, 28), "name7", money3, 27));
            userDAOImpl.insertUser(createUserBean2(8L, LocalDate.of(2007, Month.APRIL, 28), "name8", money4, 28));
            task.updateBalance();
            UserBean user1 = userDAOImpl.getUser(userDAOImpl.getUserByName("name"));
            UserBean user2 = userDAOImpl.getUser(userDAOImpl.getUserByName("name2"));
            UserBean user3 = userDAOImpl.getUser(userDAOImpl.getUserByName("name3"));
            UserBean user4 = userDAOImpl.getUser(userDAOImpl.getUserByName("name4"));
            UserBean user5 = userDAOImpl.getUser(userDAOImpl.getUserByName("name5"));
            UserBean user6 = userDAOImpl.getUser(userDAOImpl.getUserByName("name6"));
            UserBean user7 = userDAOImpl.getUser(userDAOImpl.getUserByName("name7"));
            UserBean user8 = userDAOImpl.getUser(userDAOImpl.getUserByName("name8"));
            Assert.assertTrue(user1.getAccountBeanList().get(0).getBalance().equals(money1.multiply(BigDecimal.valueOf(kk))));
            Assert.assertTrue(user2.getAccountBeanList().get(0).getBalance().equals(money2.multiply(BigDecimal.valueOf(kk))));
            Assert.assertTrue(user3.getAccountBeanList().get(0).getBalance().equals(money3.multiply(BigDecimal.valueOf(kk))));
            Assert.assertTrue(user4.getAccountBeanList().get(0).getBalance().equals(money4.multiply(BigDecimal.valueOf(kk))));
            Assert.assertTrue(user5.getAccountBeanList().get(0).getBalance().equals(money1.multiply(BigDecimal.valueOf(kk))));
            Assert.assertTrue(user6.getAccountBeanList().get(0).getBalance().equals(money2.multiply(BigDecimal.valueOf(kk))));
            Assert.assertTrue(user7.getAccountBeanList().get(0).getBalance().equals(money3.multiply(BigDecimal.valueOf(kk))));
            Assert.assertTrue(user8.getAccountBeanList().get(0).getBalance().equals(money4.multiply(BigDecimal.valueOf(kk))));
        });
    }

    @Test
    public void test2() {
        ScheduledTask task = new ScheduledTask();
        UserService userService = new UserServiceImpl() {
            @Override
            public void updateAllAccounts(double k) {
            }
        };
        FieldUtil.setField(task, userService, "userService");
        for (int i = 0; i <= SKIP_VALUE_BACK_ITERATIONS_AMOUNT; i++) {
            task.updateBalance();
        }
        BigDecimal baseValue = (BigDecimal) FieldUtil.getField(task, "baseValue");
        Assert.assertTrue(baseValue.equals(BigDecimal.ONE));
    }
}
