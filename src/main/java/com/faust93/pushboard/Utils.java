package com.faust93.pushboard;

/**
 * Created by faust93 on 23.04.2014.
 */
public class Utils {

    static long idCounter = 0;

    public static synchronized long createID()
    {
        return idCounter++;
    }
}
