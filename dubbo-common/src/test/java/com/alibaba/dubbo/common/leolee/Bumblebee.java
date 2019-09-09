package com.alibaba.dubbo.common.leolee;

import com.alibaba.dubbo.common.URL;

/**
 * 大黄蜂
 * @author: LeoLee
 * @date: 2019/9/4 15:36
 */
public class Bumblebee implements Robot {

    @Override
    public void sayHello() {
        System.out.println("Hello, I am Bumblebee.");
    }

    @Override
    public void sayAdaptive(URL url, String brand, long weight) {
        String format = String.format("Adaptive Bumblebee%s,%s,%d", url, brand, weight);
        System.out.println(format);

    }


}
