package ru.meshgroup.controller.bean;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class MailBean implements LinkedBean {

    @NotNull(message = "Идентификатор учётной записи не может быть пустым!")
    private Long id;
    @NotNull(message = "Ссылка почты на пользователя не может быть пустой!")
    private Long userId;
    @NotNull(message = "Почта не может быть пустой!")
    @Pattern(regexp = "^(.+)@(.+\\..+)$", message = "Почта указана непавильно!")
    private String email;

    public MailBean() {
    }

    public MailBean(Long id, Long userId, String email) {
        this.id = id;
        this.userId = userId;
        this.email = email;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getValue() {
        return email;
    }
}
