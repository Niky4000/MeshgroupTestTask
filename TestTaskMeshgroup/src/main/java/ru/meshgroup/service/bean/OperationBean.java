package ru.meshgroup.service.bean;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ru.meshgroup.dao.UserDAO;

public class OperationBean {

    private String operation;
    private Object[] parameters;
    static Map<String, Method> map = Stream.of(UserDAO.class.getDeclaredMethods()).filter(m -> Modifier.isPublic(m.getModifiers())).collect(Collectors.toMap(Method::getName, m -> m));

    public OperationBean(String operation, Object[] parameters) {
        this.operation = operation;
        this.parameters = parameters;
    }

    public String getOperation() {
        return operation;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public Method getMethod() {
        return map.get(operation);
    }
}
