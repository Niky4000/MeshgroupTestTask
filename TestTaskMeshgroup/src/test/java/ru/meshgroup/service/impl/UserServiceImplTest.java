package ru.meshgroup.service.impl;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteLock;
import org.apache.ignite.IgniteQueue;
import org.junit.Assert;
import org.junit.Test;
import ru.meshgroup.config.Configuration;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.controller.exceptions.MoneyException;
import ru.meshgroup.dao.UserDAO;
import ru.meshgroup.dao.impl.UserDAOImpl;
import ru.meshgroup.service.bean.OperationBean;
import static ru.meshgroup.service.impl.UserServiceImpl.CAN_NOT_UPDATE_ACCOUNTS;
import ru.meshgroup.utils.FieldUtil;

public class UserServiceImplTest {

    @Test
    public void test() {
        Configuration configuration = new Configuration();
        Ignite ignite = configuration.getIgnite("localhost", "local", 50000, 50010, 50020, 50040, 4);
        try {
            ConcurrentHashMap<String, AtomicInteger> map = new ConcurrentHashMap<>();
            final String transferMoneyMessage1 = "transferMoney finished ";
            final String transferMoneyMessage2 = "!";
            Semaphore updateAllAccountsSemaphore = new Semaphore(0);
            CountDownLatch beforeMoneyTransferLatch = new CountDownLatch(1);
            CountDownLatch accountsLatch = new CountDownLatch(2);
            CountDownLatch mainThreadLatch = new CountDownLatch(3);
            UserDAO userDAO = new UserDAOImpl() {
                @Override
                public void transferMoney(Long userIdFrom, Long userIdTo, BigDecimal money) throws MoneyException {
                    String key = transferMoneyMessage1 + Thread.currentThread().getName() + transferMoneyMessage2;
                    System.out.println(key);
                    map.putIfAbsent(key, new AtomicInteger(0));
                    map.get(key).incrementAndGet();
                }

                @Override
                public UserBean getUserByName(String name) {
                    return null;
                }

                @Override
                public void updateAllAccounts(double k) {
                    beforeMoneyTransferLatch.countDown();
                    System.out.println("updateAllAccounts started!");
                    awaitOnLatch(accountsLatch);
                    System.out.println("updateAllAccounts finished!");
                }
            };
            IgniteAtomicLong activeOperationsCounter = configuration.getActiveOperationsCounter(ignite);
            IgniteQueue<OperationBean> operationQueue = configuration.getOperationQueue(ignite);
            AtomicReference<String> errorMessage = new AtomicReference<>();
            Executor executor = configuration.getExecutor();
            IgniteLock lock = configuration.getLock(ignite);
            IgniteLock queuedOperationsLock = configuration.getQueuedOperationsLock(ignite);
            UserServiceImpl userServiceImpl = new UserServiceImpl() {
                @Override
                public void transferMoney(Long userIdFrom, Long userIdTo, BigDecimal money) throws MoneyException {
                    super.transferMoney(userIdFrom, userIdTo, money);
                    accountsLatch.countDown();
                }

                @Override
                void executeAllQueuedOperations() {
                    System.out.println("executeAllQueuedOperations started!");
                    super.executeAllQueuedOperations();
                    System.out.println("executeAllQueuedOperations finished!");
                    mainThreadLatch.countDown();

                    updateAllAccountsSemaphore.acquireUninterruptibly();
                    System.out.println("executeAllQueuedOperations finished after updateAllAccountsSemaphore " + Thread.currentThread().getName() + "!");
                }

                @Override
                void execute(Method method, Object[] parameters) {
                    methodToTunnable(method, parameters).run();
                }

                @Override
                void logError(String message) {
                    errorMessage.set(message);
                }
            };
            FieldUtil.setField(userServiceImpl, UserServiceImpl.class, lock, "lock");
            FieldUtil.setField(userServiceImpl, UserServiceImpl.class, queuedOperationsLock, "queuedOperationsLock");
            FieldUtil.setField(userServiceImpl, UserServiceImpl.class, activeOperationsCounter, "activeOperationsCounter");
            FieldUtil.setField(userServiceImpl, UserServiceImpl.class, operationQueue, "operationQueue");
            FieldUtil.setField(userServiceImpl, UserServiceImpl.class, userDAO, "userDAO");
            FieldUtil.setField(userServiceImpl, UserServiceImpl.class, executor, "executor");
            Thread thread = new Thread(() -> {
                userServiceImpl.updateAllAccounts(2.2);
            });
            final String mainThreadName = "thread";
            thread.setName(mainThreadName);
            thread.start();
            Thread thread2 = new Thread(() -> {
                awaitOnLatch(beforeMoneyTransferLatch);
                System.out.println("transferMoney started for thread " + Thread.currentThread().getName() + "!");
                userServiceImpl.transferMoney(0L, 0L, BigDecimal.TEN);
                System.out.println("transferMoney finished for thread " + Thread.currentThread().getName() + "!");
                mainThreadLatch.countDown();
            });
            thread2.setName("thread2");
            thread2.start();
            Thread thread3 = new Thread(() -> {
                awaitOnLatch(beforeMoneyTransferLatch);
                System.out.println("transferMoney started for thread " + Thread.currentThread().getName() + "!");
                userServiceImpl.transferMoney(0L, 0L, BigDecimal.TEN);
                System.out.println("transferMoney finished for thread " + Thread.currentThread().getName() + "!");
                mainThreadLatch.countDown();
            });
            thread3.setName("thread3");
            thread3.start();
            awaitOnLatch(mainThreadLatch);
            Assert.assertTrue(map.get(transferMoneyMessage1 + mainThreadName + transferMoneyMessage2).get() == 2);
            userServiceImpl.updateAllAccounts(4.4);
            updateAllAccountsSemaphore.release();
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException ex) {
                    continue;
                }
            }
            Assert.assertTrue(errorMessage.get().equals(CAN_NOT_UPDATE_ACCOUNTS));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            ignite.close();
        }
    }

    void awaitOnLatch(CountDownLatch latch) {
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException ex) {
                Logger.getLogger(UserServiceImplTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
