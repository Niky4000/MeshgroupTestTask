package ru.meshgroup.controller;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.service.UserService;
import ru.meshgroup.validator.UserValidator;

@Slf4j
@RestController
@RequestMapping("/api/test-controller")
public class TestController {

    @Autowired
    UserValidator validator;
    @Autowired
    UserService userService;

    @PostMapping("/addUser")
    public @ResponseBody
    ResponseEntity<String> addUser(@RequestBody UserBean userBean) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        DataBinder binder = new DataBinder(userBean);
        binder.setValidator(validator);
        binder.validate();
        BindingResult results = binder.getBindingResult();
        List<ObjectError> allErrors = results.getAllErrors();
        if (!allErrors.isEmpty()) {
            return new ResponseEntity<>(allErrors.stream().map(ObjectError::getDefaultMessage).reduce((s1, s2) -> s1 + " " + s2).get(), HttpStatus.BAD_REQUEST);
        } else {
            try {
                userService.insertUser(userBean);
                return new ResponseEntity<>("Ok!", HttpStatus.OK);
            } catch (Exception e) {
                log.error("userBean insertion failed!", e);
                return new ResponseEntity<>("Failure!", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    @GetMapping("/test")
    @ResponseBody
    public String test(@RequestParam("str") String str) {
        System.out.println(str);
        return UUID.randomUUID().toString();
    }

    @PostMapping("/test2")
    @ResponseBody
    public String test2(@RequestBody String str) {
        System.out.println(str);
        return UUID.randomUUID().toString();
    }
}
