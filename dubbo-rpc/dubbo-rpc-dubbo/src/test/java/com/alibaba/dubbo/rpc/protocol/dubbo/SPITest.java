package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.*;

/**
 * @author: LeoLee
 * @date: 2019/9/9 17:03
 */
public class SPITest {

    public static void main(String[] args) {
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

        protocol.export(new Invoker<Object>() {
            @Override
            public Class<Object> getInterface() {
                return null;
            }

            @Override
            public Result invoke(Invocation invocation) throws RpcException {
                return null;
            }

            @Override
            public URL getUrl() {
                return null;
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public void destroy() {

            }
        });


    }
}
