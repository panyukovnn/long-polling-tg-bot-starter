package ru.panyukovnn.longpollingtgbotstarter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram.bot")
public class TgBotProperties {

    private String name;
    private String token;

    /**
     * Таймаут установки соединения в миллисекундах
     */
    private int connectionTimeoutMs = 30_000;

    /**
     * Таймаут ожидания данных (socket timeout) в миллисекундах
     */
    private int socketTimeoutMs = 75_000;

    /**
     * Таймаут получения соединения из пула в миллисекундах
     */
    private int requestTimeoutMs = 30_000;

    /**
     * Интервал обновления стримящегося сообщения в миллисекундах
     */
    private long streamingUpdateIntervalMs = 1000;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public void setSocketTimeoutMs(int socketTimeoutMs) {
        this.socketTimeoutMs = socketTimeoutMs;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public long getStreamingUpdateIntervalMs() {
        return streamingUpdateIntervalMs;
    }

    public void setStreamingUpdateIntervalMs(long streamingUpdateIntervalMs) {
        this.streamingUpdateIntervalMs = streamingUpdateIntervalMs;
    }
}
