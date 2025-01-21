package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import lombok.NonNull;
import lombok.Value;

@Value
public class SymbolUpdate implements PropertyIndexable {

  long id;

  @NonNull
  CommitSha commit;

  @NonNull
  PropertyMap properties;

}
