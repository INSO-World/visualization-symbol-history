package com.mategka.dava.analyzer.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.io.Serial;

@SuppressWarnings("rawtypes")
public class MultimapSerializer extends StdSerializer<Multimap> {

  @Serial
  private static final long serialVersionUID = -8050289080926674584L;

  public MultimapSerializer() {
    this(null);
  }

  public MultimapSerializer(Class<Multimap> clazz) {
    super(clazz);
  }

  @Override
  public void serialize(Multimap multimap, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
    throws IOException {
    jsonGenerator.writeObject(multimap.asMap());
  }

}
