package com.mategka.dava.analyzer.struct.property.value;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.struct.property.value.bound.UpperTypeBound;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor(staticName = "of")
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public final class TypeParameter implements Serializable {

  @Serial
  private static final long serialVersionUID = 6426442880530610709L;

  @NonNull
  @Getter
  final String name;

  UpperTypeBound bound = null;

  public Option<UpperTypeBound> getTypeBound() {
    return Options.fromNullable(bound);
  }

  @Override
  public String toString() {
    return name + getTypeBound().map(b -> " " + b).getOrElse("");
  }

  @JsonValue
  public Map<String, Object> toMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("name", name);
    if (bound != null) {
      map.put("bound", bound);
    }
    return map;
  }

}
