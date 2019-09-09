package com.alibaba.dubbo.common.leolee;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * 机器人接口
 * @author: LeoLee
 * @date: 2019/9/4 15:35
 */
@SPI
public interface Robot {
    void sayHello();
    @Adaptive
    void sayAdaptive(URL url, String brand, long weight);
}
