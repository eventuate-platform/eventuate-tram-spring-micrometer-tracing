package io.eventuate.tram.spring.micrometer.tracing.test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JaegerSpan {

    private String traceID;
    private String spanID;
    private String operationName;
    private List<Reference> references;
    private List<Tag> tags;

    public String getTraceID() {
        return traceID;
    }

    public void setTraceID(String traceID) {
        this.traceID = traceID;
    }

    public String getSpanID() {
        return spanID;
    }

    public void setSpanID(String spanID) {
        this.spanID = spanID;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public List<Reference> getReferences() {
        return references;
    }

    public void setReferences(List<Reference> references) {
        this.references = references;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public Map<String, String> getTagsAsMap() {
        if (tags == null) return Map.of();
        return tags.stream()
                .collect(Collectors.toMap(Tag::getKey, t -> String.valueOf(t.getValue())));
    }

    public boolean hasTag(String key, String value) {
        return value.equals(getTagsAsMap().get(key));
    }

    public boolean hasName(String name) {
        return name.equals(this.operationName);
    }

    public String getParentSpanID() {
        if (references == null || references.isEmpty()) return null;
        return references.stream()
                .filter(r -> "CHILD_OF".equals(r.getRefType()))
                .map(Reference::getSpanID)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return "JaegerSpan{" +
                "traceID='" + traceID + '\'' +
                ", spanID='" + spanID + '\'' +
                ", operationName='" + operationName + '\'' +
                ", tags=" + getTagsAsMap() +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reference {
        private String refType;
        private String traceID;
        private String spanID;

        public String getRefType() {
            return refType;
        }

        public void setRefType(String refType) {
            this.refType = refType;
        }

        public String getTraceID() {
            return traceID;
        }

        public void setTraceID(String traceID) {
            this.traceID = traceID;
        }

        public String getSpanID() {
            return spanID;
        }

        public void setSpanID(String spanID) {
            this.spanID = spanID;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tag {
        private String key;
        private String type;
        private Object value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
