package ru.meshgroup.controller.bean;

import java.math.BigDecimal;

public class TransferMoneyRequest {

    private Long userIdTo;
    private BigDecimal money;

    public Long getUserIdTo() {
        return userIdTo;
    }

    public void setUserIdTo(Long userIdTo) {
        this.userIdTo = userIdTo;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }
}
