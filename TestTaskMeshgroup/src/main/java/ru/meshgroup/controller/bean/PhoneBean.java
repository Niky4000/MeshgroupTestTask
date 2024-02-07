package ru.meshgroup.controller.bean;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class PhoneBean {

    @NotNull(message = "Идентификатор учётной записи не может быть пустым!")
    private Long id;
    @NotNull(message = "Ссылка телефона на пользователя не может быть пустой!")
    private Long userId;
    @NotNull(message = "Телефон не может быть пустой!")
    @Pattern(regexp = "^\\d-\\d\\d\\d-\\d\\d\\d-\\d\\d-\\d\\d$", message = "Телефон указан неправильно!")
    private String phone;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
