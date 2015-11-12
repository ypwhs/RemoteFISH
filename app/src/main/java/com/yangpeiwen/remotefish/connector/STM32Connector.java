package com.yangpeiwen.remotefish.connector;

/**
 * Created by ypw
 * on 2015-11-11 下午12:08.
 */
public class STM32Connector extends Connector{

    public STM32Connector(String RPi_ip, int RPi_port) {
        super(RPi_ip, RPi_port);
        startTransfer();
    }

}