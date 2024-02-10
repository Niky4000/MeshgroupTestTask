package ru.meshgroup.test.utils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import ru.meshgroup.controller.bean.AccountBean;
import ru.meshgroup.controller.bean.MailBean;
import ru.meshgroup.controller.bean.PhoneBean;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.dao.impl.UserDAOImpl;
import static ru.meshgroup.utils.DbUtils.createInMemoryDataSource;
import static ru.meshgroup.utils.DbUtils.getJdbcTemplate;
import static ru.meshgroup.utils.DbUtils.getTransactionManager;
import static ru.meshgroup.utils.DbUtils.liquibase;
import static ru.meshgroup.utils.DbUtils.shutdown;
import ru.meshgroup.utils.FieldUtil;

public class InitUtils {

    protected void execute(Supplier<UserDAOImpl> userDAOImplSupplier, Consumer<UserDAOImpl> testImplementation) throws LiquibaseException, SQLException {
        DataSource dataSource = createInMemoryDataSource();
        try {
            liquibase(dataSource, "classpath:db/changelog/master.xml");
            PlatformTransactionManager transactionManager = getTransactionManager(dataSource);
            NamedParameterJdbcTemplate meshJdbcTemplate = getJdbcTemplate(dataSource);
            UserDAOImpl userDAOImpl = userDAOImplSupplier.get();
            FieldUtil.setField(userDAOImpl, UserDAOImpl.class, meshJdbcTemplate, "meshJdbcTemplate");
            testImplementation.accept(userDAOImpl);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            shutdown(dataSource);
        }
    }

    protected UserBean createUserBean(String userName, BigDecimal balance) {
        Long userId = 1L;
        UserBean userBean = new UserBean(userId, userName, LocalDate.of(2000, Month.APRIL, 28), "password");
        userBean.setAccountBeanList(Arrays.asList(new AccountBean(1L, userId, balance)));
        userBean.setMailBeanList(Arrays.asList(new MailBean(1L, userId, "email1"), new MailBean(2L, userId, "email2"), new MailBean(3L, userId, "email3")));
        userBean.setPhoneBeanList(Arrays.asList(new PhoneBean(1L, userId, "phone1"), new PhoneBean(2L, userId, "phone2"), new PhoneBean(3L, userId, "phone3")));
        return userBean;
    }

    protected UserBean createUserBean2(Long userId, LocalDate userBirthDate, String userName, BigDecimal balance, int... indexes) {
        UserBean userBean = new UserBean(userId, userName, userBirthDate, "password");
        userBean.setAccountBeanList(Arrays.asList(new AccountBean(userId, userId, balance)));
        userBean.setMailBeanList(IntStream.of(indexes).mapToObj(i -> new MailBean((long) i, userId, "email" + i)).collect(Collectors.toList()));
        userBean.setPhoneBeanList(IntStream.of(indexes).mapToObj(i -> new PhoneBean((long) i, userId, "phone" + i)).collect(Collectors.toList()));
        return userBean;
    }
}
