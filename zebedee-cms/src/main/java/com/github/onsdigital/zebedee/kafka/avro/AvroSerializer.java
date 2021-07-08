package com.github.onsdigital.zebedee.kafka.avro;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;


/**
 * A class used by kafka to serialize any avro messages from Java Objects
 *
 * @param <T> The type of object to be serialized
 */
public class AvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {
    private final Class<T> targetType;

    public AvroSerializer(final Class<T> targetType) {
        this.targetType = targetType;
    }

    /**
     * Serialize an object to avro format according to the object's schema
     *
     * @param s Unused string that takes the kafka topic
     * @param t Type to be serialised
     * @return
     */
    @Override
    public byte[] serialize(String s, T t) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            DatumWriter<T> writer = new SpecificDatumWriter<>(targetType.newInstance().getSchema());
            writer.write(t, encoder);
            encoder.flush();
        } catch (Exception ex) {
            throw new SerializationException(
                    "Can't serialize data '" , ex);
        }
        return out.toByteArray();
    }
}
