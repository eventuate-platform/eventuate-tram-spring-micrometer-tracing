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
        }
    }

    public enum HighCardinalityKeys implements KeyName {
        MESSAGING_MESSAGE_ID {
            @Override
            public String asString() {
                return "messaging.message_id";
            }
        },
        MESSAGING_SUBSCRIBER_ID {
            @Override
            public String asString() {
                return "messaging.subscriber_id";
            }
        }
    }
}
