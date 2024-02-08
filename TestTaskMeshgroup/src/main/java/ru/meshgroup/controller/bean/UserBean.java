package ru.meshgroup.controller.bean;

import java.time.LocalDate;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class UserBean {

    @NotNull(message = "Идентификатор пользователя не может быть пустым!")
    private Long id;
    @NotNull(message = "Имя пользователя не может быть пустым!")
    private String name;
    @NotNull(message = "День Рождения пользователя не может быть пустым!")
    private LocalDate dateOfBirth;
    @NotNull(message = "Пароль пользователя не может быть пустым!")
    private String password;
    @NotEmpty(message = "У пользователя нет учётных записей!")
    @Size(min = 1, max = 1, message = "У пользователя должна быть только одна учётная запись!")
    private List<AccountBean> accountBeanList;
    @NotEmpty(message = "У пользователя нет ни одной почты!")
    @Size(min = 1, message = "У пользователя должна быть как минимум одна почта!")
    private List<MailBean> mailBeanList;
    @NotEmpty(message = "У пользователя нет ни одного телефона!")
    @Size(min = 1, message = "У пользователя должен быть как минимум один телефон!")
    private List<PhoneBean> phoneBeanList;

    public UserBean() {
    }

    public UserBean(Long id, String name, LocalDate dateOfBirth, String password) {
        this.id = id;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<AccountBean> getAccountBeanList() {
        return accountBeanList;
    }

    public void setAccountBeanList(List<AccountBean> accountBeanList) {
        this.accountBeanList = accountBeanList;
    }

    public List<MailBean> getMailBeanList() {
        return mailBeanList;
    }

    public void setMailBeanList(List<MailBean> mailBeanList) {
        this.mailBeanList = mailBeanList;
    }

    public List<PhoneBean> getPhoneBeanList() {
        return phoneBeanList;
    }

    public void setPhoneBeanList(List<PhoneBean> phoneBeanList) {
        this.phoneBeanList = phoneBeanList;
    }
}
