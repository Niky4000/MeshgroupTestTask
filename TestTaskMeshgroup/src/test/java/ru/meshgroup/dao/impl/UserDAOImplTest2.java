package ru.meshgroup.dao.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.meshgroup.controller.exceptions.MoneyException;
import ru.meshgroup.dao.UserDAO;
import ru.meshgroup.test.utils.InitUtils;

@RunWith(SpringRunner.class)
@Import(TestConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties")
public class UserDAOImplTest2 extends InitUtils {

    @Autowired
    UserDAO userDAO;
    @Autowired
    PostgreSQLContainer postgres;

    @Test
    public void test() {
        try {
            transferMoneyTestImpl2(userDAO);
        } finally {
            postgres.stop();
        }
    }

    private void transferMoneyTestImpl2(UserDAO userDAOImpl) throws RuntimeException {
        try {
            userDAOImpl.insertUser(createUserBean2(1L, LocalDate.of(2000, Month.APRIL, 28), "name", BigDecimal.valueOf(200.82), 1, 2, 3, 4));
            userDAOImpl.insertUser(createUserBean2(2L, LocalDate.of(2001, Month.APRIL, 28), "name2", BigDecimal.valueOf(200.82), 5, 6, 7, 8, 9, 10, 11, 12));
            Thread thread = new Thread(() -> userDAOImpl.transferMoney(1L, 2L, BigDecimal.valueOf(20)));
            thread.setName("thread1");
            thread.start();
            waitSomeTime(4000);
            Thread thread2 = new Thread(() -> userDAOImpl.transferMoney(2L, 1L, BigDecimal.valueOf(20)));
            thread2.setName("thread2");
            thread2.start();
            join(thread);
            join(thread2);
        } catch (MoneyException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void join(Thread thread) {
        while (true) {
            try {
                thread.join();
                break;
            } catch (InterruptedException ex) {
                Logger.getLogger(UserDAOImplTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void waitSomeTime(int timeToWait) {
        while (true) {
            try {
                Thread.sleep(timeToWait);
                break;
            } catch (InterruptedException ex) {
                Logger.getLogger(UserDAOImplTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
