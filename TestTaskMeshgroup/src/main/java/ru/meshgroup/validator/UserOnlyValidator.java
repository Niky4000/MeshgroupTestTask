package ru.meshgroup.validator;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.meshgroup.controller.bean.UserBean;

@Service
public class UserOnlyValidator implements Validator {

    @Autowired
    LocalValidatorFactoryBean localValidatorFactoryBean;
    Set<String> fieldsToIgnore = Set.of("accountBeanList", "mailBeanList", "phoneBeanList");
    List<String> userFieldList = Stream.of(UserBean.class.getDeclaredFields()).filter(f -> !f.getType().equals(List.class)).map(Field::getName).collect(Collectors.toList());

    @Override
    public boolean supports(Class<?> type) {
        return UserBean.class.equals(type);
    }

    @Override
    public void validate(Object o, Errors errors) {
        UserBean userBean = (UserBean) o;
        userFieldList.forEach(fieldName -> {
            Set<ConstraintViolation<Object>> validateProperty = localValidatorFactoryBean.validateProperty(userBean, fieldName);
            validateProperty.forEach(err -> errors.reject(fieldName, err.getMessage()));
        });
        if (userBean.getAccountBeanList() != null && !userBean.getAccountBeanList().isEmpty()) {
            errors.reject("accountBeanList", "Пользователь не может обновить свои собственные учётные данные!");
        }
        if (userBean.getMailBeanList() != null) {
            userBean.getMailBeanList().forEach(mail -> localValidatorFactoryBean.validate(mail, errors));
        }
        if (userBean.getPhoneBeanList() != null) {
            userBean.getPhoneBeanList().forEach(phone -> localValidatorFactoryBean.validate(phone, errors));
        }
    }
}
