package ru.meshgroup.controller.exceptions;

public class AccountIsLockedException extends RuntimeException {

    public AccountIsLockedException(Long userId) {
        super("Account for user id = " + userId + " is locked! It's impossible to transfer money right now!");
    }
}
