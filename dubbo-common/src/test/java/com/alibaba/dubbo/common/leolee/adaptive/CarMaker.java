package com.alibaba.dubbo.common.leolee.adaptive;

import com.alibaba.dubbo.common.URL;

/**
 * @author: LeoLee
 * @date: 2019/9/7 14:15
 */
public interface CarMaker {
    Car makeCar(URL url);
}
