package org.nextples.places.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class HtmlController {

    public static String getPage(String path) {
        StringBuilder sb = new StringBuilder();
        ClassLoader classLoader= HtmlController.class.getClassLoader();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(classLoader.getResourceAsStream(path))));
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }
}
