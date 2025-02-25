package com.mategka.dava.analyzer.struct.workspace;

import com.mategka.dava.analyzer.collections.DefaultMap;
import com.mategka.dava.analyzer.struct.Strand;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class StrandWorkspaceIndex {

  Map<Strand, StrandWorkspaceImpl> workspaces = new DefaultMap<>(HashMap::new, StrandWorkspaceImpl::new);

  public @NotNull MutableStrandWorkspace get(Strand strand) {
    return workspaces.get(strand);
  }

  public @NotNull StrandWorkspace getReadonly(Strand strand) {
    return new StrandWorkspaceView(get(strand));
  }

}
