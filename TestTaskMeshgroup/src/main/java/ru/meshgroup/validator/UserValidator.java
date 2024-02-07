package ru.meshgroup.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.meshgroup.controller.bean.UserBean;

@Service
public class UserValidator implements Validator {

    @Autowired
    LocalValidatorFactoryBean localValidatorFactoryBean;

    @Override
    public boolean supports(Class<?> type) {
        return UserBean.class.equals(type);
    }

    @Override
    public void validate(Object o, Errors errors) {
        UserBean userBean = (UserBean) o;
        localValidatorFactoryBean.validate(o, errors);
        if (userBean.getAccountBeanList() != null && !userBean.getAccountBeanList().isEmpty()) {
            userBean.getAccountBeanList().forEach(account -> localValidatorFactoryBean.validate(account, errors));
        }
        if (userBean.getMailBeanList() != null && !userBean.getMailBeanList().isEmpty()) {
            userBean.getMailBeanList().forEach(mail -> localValidatorFactoryBean.validate(mail, errors));
        }
        if (userBean.getPhoneBeanList() != null && !userBean.getPhoneBeanList().isEmpty()) {
            userBean.getPhoneBeanList().forEach(phone -> localValidatorFactoryBean.validate(phone, errors));
        }
    }
}
