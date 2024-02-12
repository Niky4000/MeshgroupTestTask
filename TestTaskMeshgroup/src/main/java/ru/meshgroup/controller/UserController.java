package ru.meshgroup.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.meshgroup.controller.bean.TransferMoneyRequest;
import ru.meshgroup.controller.bean.UserBean;
import ru.meshgroup.controller.exceptions.AccountIsLockedException;
import ru.meshgroup.controller.exceptions.DatabaseIsLocked;
import ru.meshgroup.controller.exceptions.MoneyException;
import ru.meshgroup.service.UserService;
import ru.meshgroup.validator.UserOnlyValidator;
import ru.meshgroup.validator.UserValidator;

@Slf4j
@RestController
@RequestMapping("/api/test-controller")
public class UserController {

    @Autowired
    UserValidator validator;
    @Autowired
    UserOnlyValidator userOnlyValidator;
    @Autowired
    UserService userService;

    @GetMapping("/getUserList")
    @ResponseBody
    public ResponseEntity<List<UserBean>> getUserList(@RequestParam(value = "name", required = false) String name, @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "email", required = false) String email, @RequestParam(value = "phone", required = false) String phone, @RequestParam(value = "size") int size, @RequestParam(value = "offset") int offset) {
        try {
            return new ResponseEntity<>(userService.getUserList(name, date != null ? LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null, email, phone, offset, size), HttpStatus.OK);
        } catch (DatabaseIsLocked databaseIsLocked) {
            return new ResponseEntity<>(new ArrayList<>(1), HttpStatus.LOCKED);
        }
    }

    @PostMapping("/addUser")
    public @ResponseBody
    ResponseEntity<String> addUser(@RequestBody UserBean userBean) {
        return requestHandler(() -> {
            try {
                userService.insertUser(userBean);
                return new ResponseEntity<>("Ok!", HttpStatus.OK);
            } catch (Exception e) {
                log.error("userBean insertion failed!", e);
                return new ResponseEntity<>("Failure!", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, userBean, validator);
    }

    private ResponseEntity<String> requestHandler(Supplier<ResponseEntity<String>> requestHandler, Object requestBean, Validator validator) {
        List<ObjectError> allErrors = checkUser(requestBean, validator);
        if (!allErrors.isEmpty()) {
            return createErrorResponse(allErrors);
        } else {
            return requestHandler.get();
        }
    }

    private ResponseEntity<String> createErrorResponse(List<ObjectError> allErrors) {
        return new ResponseEntity<>(allErrors.stream().map(ObjectError::getDefaultMessage).reduce((s1, s2) -> s1 + " " + s2).get(), HttpStatus.BAD_REQUEST);
    }

    private List<ObjectError> checkUser(Object requestBean, Validator validator) {
        DataBinder binder = new DataBinder(requestBean);
        binder.setValidator(validator);
        binder.validate();
        BindingResult results = binder.getBindingResult();
        List<ObjectError> allErrors = results.getAllErrors();
        return allErrors;
    }

    @PostMapping("/updateUser")
    @ResponseBody
    public ResponseEntity<String> updateUser(@RequestBody UserBean userBean) {
        return requestHandler(() -> {
            try {
                userService.updateUser(userBean);
                return new ResponseEntity<>("Ok!", HttpStatus.OK);
            } catch (Exception e) {
                log.error("userBean update failed!", e);
                return new ResponseEntity<>("Failure!", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }, userBean, userOnlyValidator);
    }

    @PostMapping("/transferMoney")
    public ResponseEntity<String> transferMoney(@RequestBody TransferMoneyRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User principal = (User) authentication.getPrincipal();
            String username = principal.getUsername();
            UserBean user = userService.getUser(username);
            if (user != null) {
                userService.transferMoney(user.getId(), request.getUserIdTo(), request.getMoney());
                return new ResponseEntity<>("Ok!", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("It's impossible to transfer money because source account " + username + " isn't found!", HttpStatus.BAD_REQUEST);
            }
        } catch (DatabaseIsLocked databaseIsLocked) {
            return new ResponseEntity<>("Database is locked! Please try later!", HttpStatus.LOCKED);
        } catch (MoneyException me) {
            return new ResponseEntity<>(me.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (AccountIsLockedException lockedException) {
            return new ResponseEntity<>(lockedException.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception me) {
            return new ResponseEntity<>("Internal server error!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/test")
    @ResponseBody
    public String test(@RequestParam("str") String str) {
        System.out.println(str);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User principal = (User) authentication.getPrincipal();
        String username = principal.getUsername();
        return UUID.randomUUID().toString();
    }

    @PostMapping("/test2")
    @ResponseBody
    public String test2(@RequestBody String str) {
        System.out.println(str);
        return UUID.randomUUID().toString();
    }
}
