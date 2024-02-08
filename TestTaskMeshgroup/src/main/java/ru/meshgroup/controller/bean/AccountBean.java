package ru.meshgroup.controller.bean;

import java.math.BigDecimal;
import javax.validation.constraints.NotNull;

public class AccountBean {

    @NotNull(message = "Идентификатор учётной записи не может быть пустым!")
    private Long id;
    @NotNull(message = "Ссылка учётной записи на пользователя не может быть пустой!")
    private Long userId;
    @NotNull(message = "Баланс учётной записи не может быть пустым!")
    private BigDecimal balance;

    public AccountBean() {
    }

    public AccountBean(Long id, Long userId, BigDecimal balance) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
