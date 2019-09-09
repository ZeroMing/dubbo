package com.alibaba.dubbo.common.leolee;

import com.alibaba.dubbo.common.URL;

/**
 * 擎天柱
 * @author: LeoLee
 * @date: 2019/9/4 15:36
 */
public class OptimusPrime implements Robot {

    @Override
    public void sayHello() {
        System.out.println("Hello, I am Optimus Prime.");
    }

    @Override
    public void sayAdaptive(URL url, String brand, long weight) {
        System.out.println("Adaptive,Optimus Prime.");
    }

}