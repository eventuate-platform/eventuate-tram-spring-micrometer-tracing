package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

public enum TramObservationDocumentation implements ObservationDocumentation {

    PRODUCER {
        @Override
        public String getName() {
            return "eventuate.tram.producer";
        }

        @Override
        public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
            return DefaultConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeys.values();
        }

        @Override
        public KeyName[] getHighCardinalityKeyNames() {
            return HighCardinalityKeys.values();
        }
    },

    CONSUMER {
        @Override
        public String getName() {
            return "eventuate.tram.consumer";
        }

        @Override
        public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
            return DefaultConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeys.values();
        }

        @Override
        public KeyName[] getHighCardinalityKeyNames() {
            return HighCardinalityKeys.values();
        }
    },

    DEDUPLICATION {
        @Override
        public String getName() {
            return "eventuate.tram.deduplication";
        }

        @Override
        public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
            return DefaultConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return new KeyName[0];
        }

        @Override
        public KeyName[] getHighCardinalityKeyNames() {
            return new KeyName[0];
        }
    };

    public enum LowCardinalityKeys implements KeyName {
        MESSAGING_OPERATION {
            @Override
            public String asString() {
                return "messaging.operation";
            }
        },
        MESSAGING_DESTINATION {
            @Override
            public String asString() {
                return "messaging.destination";
            }
        },
        MESSAGING_SYSTEM {
            @Override
            public String asString() {
                return "messaging.system";
            }
        },
        MESSAGING_SUBSCRIBER_ID {
            @Override
            public String asString() {
                return "messaging.subscriber.id";
            }
        }
    }

    public enum HighCardinalityKeys implements KeyName {
        MESSAGING_MESSAGE_ID {
            @Override
            public String asString() {
                return "messaging.message.id";
            }
        }
    }

    public enum DefaultConvention implements ObservationConvention<Observation.Context> {
        INSTANCE;

        @Override
        public boolean supportsContext(Observation.Context context) {
            return context instanceof TramProducerObservationContext ||
                   context instanceof TramConsumerObservationContext;
        }

        @Override
        public KeyValues getLowCardinalityKeyValues(Observation.Context context) {
            if (context instanceof TramProducerObservationContext producerContext) {
                return KeyValues.of(
                    KeyValue.of(LowCardinalityKeys.MESSAGING_SYSTEM, "eventuate-tram"),
                    KeyValue.of(LowCardinalityKeys.MESSAGING_OPERATION, "publish"),
                    KeyValue.of(LowCardinalityKeys.MESSAGING_DESTINATION, producerContext.getDestination())
                );
            } else if (context instanceof TramConsumerObservationContext consumerContext) {
                return KeyValues.of(
                    KeyValue.of(LowCardinalityKeys.MESSAGING_SYSTEM, "eventuate-tram"),
                    KeyValue.of(LowCardinalityKeys.MESSAGING_OPERATION, "receive"),
                    KeyValue.of(LowCardinalityKeys.MESSAGING_DESTINATION, consumerContext.getDestination()),
                    KeyValue.of(LowCardinalityKeys.MESSAGING_SUBSCRIBER_ID, consumerContext.getSubscriberId())
                );
            }
            return KeyValues.empty();
        }

        @Override
        public KeyValues getHighCardinalityKeyValues(Observation.Context context) {
            if (context instanceof TramProducerObservationContext producerContext) {
                String messageId = producerContext.getMessageId();
                if (messageId != null) {
                    return KeyValues.of(KeyValue.of(HighCardinalityKeys.MESSAGING_MESSAGE_ID, messageId));
                }
            } else if (context instanceof TramConsumerObservationContext consumerContext) {
                String messageId = consumerContext.getMessageId();
                if (messageId != null) {
                    return KeyValues.of(KeyValue.of(HighCardinalityKeys.MESSAGING_MESSAGE_ID, messageId));
                }
            }
            return KeyValues.empty();
        }
    }
}
