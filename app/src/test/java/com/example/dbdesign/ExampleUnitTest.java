package com.example.dbdesign;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void test() {
        Map<String, Object> map1 = new HashMap<String, Object>();
        Map<String, Object> map2 = new Hashtable<String, Object>();
        Map<String, Object> map3 = new ConcurrentHashMap<String, Object>();
        Map<String, Object> map4 = Collections.synchronizedMap(new HashMap<String, Object>());
        Map<String, Object> map = map3;
        for (int i = 1; i <= 100; i++) {
            map.put("key" + i, "value" + i);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                System.out.println("i" + i);
                if (map.size() > 0) {
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        i++;
                        System.out.println(String.format("%s: %s", entry.getKey(), entry.getValue()));
                    }
//                        map.clear();
                }
                System.out.println("i" + i);

            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 1000; i++) {
                    map.put("key" + i, "value" + i);
                                    System.out.println("i" + i);
                }
            }
        }).start();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                for (int i = 1; i <= 1000; i++) {
//                    map.put("key" + i, "value" + i);
//                }
//            }
//        }).start();
    }

    @Test
    public void testFor() {
        for (int i = 0; i < 10; i++) {
            System.out.println("i"+i);
            for (int j = 0; j < 100; j++) {
                System.out.println("j"+j);
                break;
            }
        }
    }

    @Test
    public void testAdd() {
        List<String> list = new ArrayList<>();
        try {
            list.add(null);
            list.add(null);
            System.out.println(list.size());
        } catch (Exception e) {
            System.out.println(e);
        }

        for (String s : list) {
            System.out.println("String is " + (s == null?"null":s));
        }
    }
}