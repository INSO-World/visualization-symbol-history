package com.mategka.dava.analyzer.struct;

import com.mategka.dava.analyzer.struct.property.index.PropertyIndexable;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import lombok.*;

@Value
public class SymbolUpdate implements PropertyIndexable {

  long id;

  @NonNull
  CommitSha commit;

  @NonNull
  PropertyMap properties;

}
