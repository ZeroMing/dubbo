package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;

/**
 * @author: LeoLee
 * @date: 2019/9/16 17:09
 */
public class TestMock implements DemoService {
    @Override
    public String sayHello(String name) {
        return "系统繁忙，请稍后再试!!!";
    }
}

