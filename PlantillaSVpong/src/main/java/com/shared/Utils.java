package com.shared; // clients

import java.util.List;

public class Utils {

    public static String getColorByName(String name, List<ClientData> clients) {
        return clients.stream()
                      .filter(c -> c.getName().equals(name))
                      .map(ClientData::getColor)
                      .findFirst()
                      .orElse("gray");
    }

    public static String getClientColor(String clientName, List<ClientData> clients) {
        return getColorByName(clientName, clients);
    }
}
