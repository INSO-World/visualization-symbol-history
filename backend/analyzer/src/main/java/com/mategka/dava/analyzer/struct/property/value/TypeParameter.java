package com.mategka.dava.analyzer.struct.property.value;

import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.struct.property.value.bound.UpperTypeBound;

import lombok.*;

@RequiredArgsConstructor(staticName = "of")
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public final class TypeParameter {

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

}
