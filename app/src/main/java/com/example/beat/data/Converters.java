package com.example.beat.data;

import androidx.room.TypeConverter;
import java.util.Date;
import java.util.List;
import java.util.Arrays;

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static List<Integer> fromIntegerList(String value) {
        if (value == null) return null;
        return Arrays.asList(value.split(","))
                .stream()
                .map(Integer::parseInt)
                .toList();
    }

    @TypeConverter
    public static String toIntegerList(List<Integer> list) {
        if (list == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}

