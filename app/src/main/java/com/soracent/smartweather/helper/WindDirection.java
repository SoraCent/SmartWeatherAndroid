package com.soracent.smartweather.helper;

public class WindDirection {
    public static String getWindDirection(String windDeg) {
        int windAngle = Integer.parseInt(windDeg);
        if (windAngle >= 0 && windAngle <= 11.25) {
            return "N";
        } else if (windAngle > 348.75 && windAngle <= 360) {
            return "N";
        } else if (windAngle > 11.25 && windAngle <= 33.75) {
            return "NNO";
        } else if (windAngle > 33.75 && windAngle <= 56.25) {
            return "NO";
        } else if (windAngle > 56.25 && windAngle <= 78.75) {
            return "ONO";
        } else if (windAngle > 78.75 && windAngle <= 101.25) {
            return "O";
        } else if (windAngle > 101.25 && windAngle <= 123.75) {
            return "OSO";
        } else if (windAngle > 123.75 && windAngle <= 146.25) {
            return "SO";
        } else if (windAngle > 146.25 && windAngle <= 168.75) {
            return "SSO";
        } else if (windAngle > 168.75 && windAngle <= 191.25) {
            return "S";
        } else if (windAngle > 191.25 && windAngle <= 213.75) {
            return "SSW";
        } else if (windAngle > 213.75 && windAngle <= 236.25) {
            return "SW";
        } else if (windAngle > 236.25 && windAngle <= 258.75) {
            return "WSW";
        } else if (windAngle > 258.75 && windAngle <= 281.25) {
            return "W";
        } else if (windAngle > 281.25 && windAngle <= 303.75) {
            return "WNW";
        } else if (windAngle > 303.75 && windAngle <= 326.25) {
            return "NW";
        } else if (windAngle > 326.25 && windAngle <= 348.75) {
            return "NNW";
        }
        return "";
    }
}
