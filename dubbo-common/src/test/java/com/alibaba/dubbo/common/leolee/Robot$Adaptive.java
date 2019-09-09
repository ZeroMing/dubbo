package com.alibaba.dubbo.common.leolee;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

public class Robot$Adaptive implements com.alibaba.dubbo.common.leolee.Robot {

    public void sayAdaptive(com.alibaba.dubbo.common.URL arg0, java.lang.String arg1, long arg2) {
        if (arg0 == null) {
            throw new IllegalArgumentException("url == null");
        }
        // 第一参数为 url
        com.alibaba.dubbo.common.URL url = arg0;
        // 第二个参数为 robot
        String extName = url.getParameter("robot");
        if (extName == null) {
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.common.leolee.Robot) name from url(" + url.toString() + ") use keys([robot])");
        }
        com.alibaba.dubbo.common.leolee.Robot extension =
                (com.alibaba.dubbo.common.leolee.Robot) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.leolee.Robot.class).getExtension(extName);
        extension.sayAdaptive(arg0, arg1, arg2);
    }

    public void sayHello() {
        throw new UnsupportedOperationException("method public abstract void com.alibaba.dubbo.common.leolee.Robot.sayHello() of interface com.alibaba.dubbo.common.leolee.Robot is not adaptive method!");
    }
}