package ru.meshgroup.service.impl;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteCondition;
import org.apache.ignite.IgniteLock;
import org.apache.ignite.IgniteQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.controller.exceptions.MoneyException;
import ru.meshgroup.dao.UserDAO;
import ru.meshgroup.service.UserService;
import ru.meshgroup.service.bean.OperationBean;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    IgniteLock lock;
    @Autowired
    IgniteLock queuedOperationsLock;
    @Autowired
    UserDAO userDAO;
    @Autowired
    IgniteAtomicLong activeOperationsCounter;
    @Autowired
    IgniteQueue<OperationBean> operationQueue;
    @Autowired
    @Qualifier("meshExecutor")
    Executor executor;
    private static final String CONDITION_NAME = "blockReadersAndWriters";
    static final String CAN_NOT_UPDATE_ACCOUNTS = "updateAllAccounts operation can't be executed because previous one hasn't been finished yet!";

    @Override
    public void insertUser(UserBean userBean) {
        exec(u -> userDAO.insertUser(userBean), userBean);
    }

    @Override
    public void updateUser(UserBean userBean) {
        exec(u -> userDAO.updateUser((UserBean) u[0]), userBean);
    }

    @Override
    public UserBean getUser(String name) {
        return exec(() -> userDAO.getUserByName(name), name);
    }

    @Override
    public void transferMoney(Long userIdFrom, Long userIdTo, BigDecimal money) throws MoneyException {
        exec(arr -> userDAO.transferMoney((Long) arr[0], (Long) arr[1], (BigDecimal) arr[2]), userIdFrom, userIdTo, money);
    }

    <T> T exec(Supplier<T> supplier, Object... obj) {
        if (before(obj)) {
            try {
                return supplier.get();
            } finally {
                after();
            }
        } else {
            return null;
        }
    }

    <T> void exec(Consumer<T[]> supplier, T... obj) {
        if (before(obj)) {
            try {
                supplier.accept(obj);
            } finally {
                after();
            }
        }
    }

    boolean before(Object... obj) {
        if (lock.tryLock()) {
            try {
                activeOperationsCounter.incrementAndGet();
            } finally {
                lock.unlock();
            }
            return true;
        } else {
            operationQueue.add(new OperationBean(getMethodName(), obj));
            return false;
        }
    }

    private String getMethodName() {
        return Thread.currentThread().getStackTrace()[4].getMethodName();
    }

    void after() {
        try {
            lock.lock();
            activeOperationsCounter.decrementAndGet();
            IgniteCondition condition = lock.getOrCreateCondition(CONDITION_NAME);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateAllAccounts(double k) {
        if (queuedOperationsLock.tryLock()) {
            try {
                try {
                    lock.lock();
                    IgniteCondition condition = lock.getOrCreateCondition(CONDITION_NAME);
                    while (activeOperationsCounter.get() > 0L) {
                        condition.await();
                    }
                    log.debug("updateAllAccounts operation started!");
                    userDAO.updateAllAccounts(k);
                    log.debug("updateAllAccounts operation finished!");
                } finally {
                    lock.unlock();
                }
                log.debug("executeAllQueuedOperations operation started!");
                executeAllQueuedOperations();
                log.debug("executeAllQueuedOperations operation started!");
            } finally {
                queuedOperationsLock.unlock();
            }
        } else {
            logError(CAN_NOT_UPDATE_ACCOUNTS);
        }
    }

    void logError(String message) {
        log.error(message);
    }

    void executeAllQueuedOperations() {
        List<OperationBean> objectList = new ArrayList<>();
        operationQueue.drainTo(objectList);
        for (OperationBean operation : objectList) {
            Method method = operation.getMethod();
            Object[] parameters = operation.getParameters();
            execute(method, parameters);
        }
    }

    void execute(Method method, Object[] parameters) {
        executor.execute(methodToTunnable(method, parameters));
    }

    Runnable methodToTunnable(Method method, Object[] parameters) {
        return () -> {
            try {
                method.invoke(userDAO, parameters);
            } catch (Exception ex) {
                log.error("Exception during method execution!", ex);
            }
        };
    }
}
