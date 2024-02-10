package ru.meshgroup.scheduled;

import java.math.BigDecimal;
import java.math.MathContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.meshgroup.service.UserService;

@Slf4j
@Component
public class ScheduledTask {

    @Autowired
    UserService userService;
    private static final BigDecimal maxPerCent = BigDecimal.valueOf(2.07);
    private static final BigDecimal minPerCent = BigDecimal.valueOf(0.1);
    private BigDecimal baseValue = BigDecimal.ONE;
    private int iteration = 0;
    static final int SKIP_VALUE_BACK_ITERATIONS_AMOUNT = 10;

    @Scheduled(fixedDelayString = "${app.task.scheduling.updateBalance}")
    void updateBalance() {
        BigDecimal k;
        if (iteration < SKIP_VALUE_BACK_ITERATIONS_AMOUNT) {
            k = BigDecimal.ONE.add(BigDecimal.valueOf(Math.random()).multiply(maxPerCent.add(minPerCent.negate())));
            baseValue = baseValue.multiply(k);
        } else {
            iteration = 0;
            k = BigDecimal.ONE.divide(baseValue, new MathContext(2));
            baseValue = BigDecimal.ONE;
        }
        userService.updateAllAccounts(k.doubleValue());
        iteration++;
        log.info("All accounts were updated using k = " + k.toString() + "!");
    }
}
