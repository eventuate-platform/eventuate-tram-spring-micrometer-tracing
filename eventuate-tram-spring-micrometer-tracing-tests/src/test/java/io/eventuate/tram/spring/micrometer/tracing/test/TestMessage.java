package io.eventuate.tram.spring.micrometer.tracing.test;

public class TestMessage {

    private String content;

    public TestMessage() {
    }

    public TestMessage(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
