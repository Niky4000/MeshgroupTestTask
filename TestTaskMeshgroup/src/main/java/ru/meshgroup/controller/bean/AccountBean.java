package ru.meshgroup.controller.bean;

import javax.validation.constraints.NotNull;

public class AccountBean {

    @NotNull(message = "Идентификатор учётной записи не может быть пустым!")
    private Long id;
    @NotNull(message = "Ссылка учётной записи на пользователя не может быть пустой!")
    private Long userId;
    @NotNull(message = "Баланс учётной записи не может быть пустым!")
    private Long balance;

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

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }
}
