package com.mategka.dava.analyzer;

import com.mategka.dava.analyzer.git.Repositories;
import com.mategka.dava.analyzer.struct.History;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import spoon.MavenLauncher;

import java.io.IOException;

public class App {

  public static void main(String[] args) {
    System.out.println("Hello World!");
    try (Repository repository = Repositories.open("?REPO")) {
      Ref mainBranch = repository.findRef("main");
      var history = History.emptyOfBranch(repository, mainBranch);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // Goal 1: Get all changes for one commit
    // Goal 2: Get all changes for history (starting from specified commit)
    // Goal 3: Get all changes and refactorings for history
    // Goal 4: Get all symbol changes for history
    // Goal 5: Get all structured symbol changes for history (symbol parentage, ...)
    // Side Goal: Make sure each commit-file combo is only read and parsed ONCE
  }

}
