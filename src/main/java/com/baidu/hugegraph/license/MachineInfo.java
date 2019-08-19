/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.hugegraph.license;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MachineInfo {

    private List<String> ipAddressList;
    private List<String> macAddressList;

    public MachineInfo() {
        this.ipAddressList = null;
        this.macAddressList = null;
    }

    public List<String> getIpAddress() {
        if (this.ipAddressList != null) {
            return this.ipAddressList;
        }
        this.ipAddressList = new ArrayList<>();
        List<InetAddress> inetAddresses = this.getLocalAllInetAddress();
        if (inetAddresses != null && inetAddresses.size() > 0) {
            this.ipAddressList = inetAddresses.stream()
                                              .map(InetAddress::getHostAddress)
                                              .distinct()
                                              .map(String::toLowerCase)
                                              .collect(Collectors.toList());
        }
        return this.ipAddressList;
    }

    public List<String> getMacAddress() {
        if (this.macAddressList != null) {
            return this.macAddressList;
        }
        this.macAddressList = new ArrayList<>();
        List<InetAddress> inetAddresses = this.getLocalAllInetAddress();
        if (inetAddresses != null && inetAddresses.size() > 0) {
            // Get the Mac address of all network interfaces
            List<String> list = new ArrayList<>();
            Set<String> uniqueValues = new HashSet<>();
            for (InetAddress inetAddress : inetAddresses) {
                String macByInetAddress = this.getMacByInetAddress(inetAddress);
                if (uniqueValues.add(macByInetAddress)) {
                    list.add(macByInetAddress);
                }
            }
            this.macAddressList = list;
        }
        return this.macAddressList;
    }

    public List<InetAddress> getLocalAllInetAddress() {
        Enumeration interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException("Failed to get network interfaces");
        }

        List<InetAddress> result = new ArrayList<>(4);
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = (NetworkInterface) interfaces.nextElement();
            for (Enumeration inetAddresses = iface.getInetAddresses();
                 inetAddresses.hasMoreElements(); ) {
                InetAddress inetAddr = (InetAddress) inetAddresses.nextElement();
                if (!inetAddr.isLoopbackAddress() &&
                    !inetAddr.isLinkLocalAddress() &&
                    !inetAddr.isMulticastAddress()) {
                    result.add(inetAddr);
                }
            }
        }
        return result;
    }

    public String getMacByInetAddress(InetAddress inetAddr) {
        byte[] mac;
        try {
            mac = NetworkInterface.getByInetAddress(inetAddr)
                                  .getHardwareAddress();
        } catch (SocketException e) {
            throw new RuntimeException("Failed to get hardware addresses");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            if (i != 0) {
                sb.append("-");
            }
            String temp = Integer.toHexString(mac[i] & 0xff);
            if (temp.length() == 1) {
                sb.append("0").append(temp);
            } else {
                sb.append(temp);
            }
        }
        return sb.toString().toUpperCase();
    }
}
