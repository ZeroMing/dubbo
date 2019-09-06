package com.alibaba.dubbo.common.leolee;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import org.junit.Test;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author: LeoLee
 * @date: 2019/9/4 15:43
 */
public class DubboSPITest {

    @Test
    public void sayHello() throws Exception {
        ExtensionLoader<Robot> extensionLoader =
                ExtensionLoader.getExtensionLoader(Robot.class);
        Robot optimusPrime = extensionLoader.getExtension("optimusPrime");
        optimusPrime.sayHello();
        Robot bumblebee = extensionLoader.getExtension("bumblebee");
        bumblebee.sayHello();

        // 获得自适应拓展对象
        Robot adaptiveExtension = extensionLoader.getAdaptiveExtension();
        adaptiveExtension.sayHello();
    }
}
