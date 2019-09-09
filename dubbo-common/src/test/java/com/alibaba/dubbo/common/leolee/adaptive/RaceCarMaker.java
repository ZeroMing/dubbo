package com.alibaba.dubbo.common.leolee.adaptive;

import com.alibaba.dubbo.common.URL;

/**
 * @author: LeoLee
 * @date: 2019/9/7 14:17
 */
public class RaceCarMaker implements CarMaker{
    WheelMaker wheelMaker;

    // 通过 setter 注入 AdaptiveWheelMaker
    public void setWheelMaker(WheelMaker wheelMaker) {
        this.wheelMaker = wheelMaker;
    }

    @Override
    public Car makeCar(URL url) {
        Wheel wheel = wheelMaker.makeWheel(url);
        return new RaceCar(wheel);
    }
}
