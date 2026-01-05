package io.eventuate.tram.spring.micrometer.tracing.test;

import java.util.Map;

public class ZipkinSpan {

    private String id;
    private String traceId;
    private String parentId;
    private String name;
    private Map<String, String> tags;

    @Override
    public String toString() {
        return "ZipkinSpan{" +
                "id='" + id + '\'' +
                ", traceId='" + traceId + '\'' +
                ", parentId='" + parentId + '\'' +
                ", name='" + name + '\'' +
                ", tags=" + tags +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public boolean hasTag(String tag, String value) {
        return tags != null && value.equals(tags.get(tag));
    }

    public boolean hasName(String name) {
        return name.equals(this.name);
    }

    public boolean isChildOf(ZipkinSpan parent) {
        return parent.getId().equals(this.parentId);
    }
}
