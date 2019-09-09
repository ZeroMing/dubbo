package com.alibaba.dubbo.common.leolee;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author: LeoLee
 * @date: 2019/9/4 15:43
 */
public class DubboSPITest {

    @Test
    public void sayHello() throws Exception {
        ExtensionLoader<Robot> extensionLoader =
                ExtensionLoader.getExtensionLoader(Robot.class);
        // SPI 扩展
        Robot optimusPrime = extensionLoader.getExtension("optimusPrime");
        optimusPrime.sayHello();
        Robot bumblebee = extensionLoader.getExtension("bumblebee");
        bumblebee.sayHello();

        // 获得自适应拓展对象
        Robot adaptiveExtension = extensionLoader.getAdaptiveExtension();
        adaptiveExtension.sayAdaptive(new URL("dubbo","127.0.0.1",20880,new HashMap<String, String>(){{
            put("robot","bumblebee");
        }}),"Nike",10);
        adaptiveExtension.sayAdaptive(new URL("dubbo","127.0.0.1",20880,new HashMap<String, String>(){{
            put("robot","optimusPrime");
        }}),"Nike",10);


    }
}
