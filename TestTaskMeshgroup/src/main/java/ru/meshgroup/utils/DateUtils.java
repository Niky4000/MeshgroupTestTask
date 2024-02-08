package ru.meshgroup.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

public class DateUtils {

    public static LocalDate toLocalDateFromSql(java.sql.Date sqlDate) {
        Date utilDate = null;
        if (sqlDate != null) {
            utilDate = new java.util.Date(sqlDate.getTime());
        }
        return toLocalDate(utilDate);
    }

    private static LocalDate toLocalDate(Date date) {
        return Optional.ofNullable(date).map(date1 -> date1.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).orElseGet(() -> null);
    }
}
