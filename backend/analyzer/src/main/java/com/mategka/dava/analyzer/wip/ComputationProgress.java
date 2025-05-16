package com.mategka.dava.analyzer.wip;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A class for modeling and tracking the progress of multi-layered sequential computations.
 * This allows for nested tasks with different size units and provides progress estimation
 * based on completed work.
 */
public class ComputationProgress {

  private final Map<UUID, Task> tasks = new HashMap<>();
  @Getter
  private Task rootTask;

  /**
   * Completes a leaf task by its ID.
   *
   * @param taskId The ID of the task to complete
   */
  public void completeTask(UUID taskId) {
    Task task = tasks.get(taskId);
    if (task == null) {
      throw new IllegalArgumentException("Task not found");
    }
    task.complete();
  }

  /**
   * Creates a new computation progress tracker with a root task.
   *
   * @param name         The name of the root task
   * @param size         The size of the root task
   * @param subtaskCount The number of subtasks the root task will have
   * @return The created root task
   */
  public Task createRootTask(String name, double size, int subtaskCount) {
    rootTask = new Task(name, size, subtaskCount, null);
    tasks.put(rootTask.getId(), rootTask);
    return rootTask;
  }

  /**
   * Creates a new subtask for the specified parent task.
   *
   * @param parentId     The ID of the parent task
   * @param name         The name of the subtask
   * @param size         The size of the subtask
   * @param subtaskCount The number of subtasks this subtask will have
   * @return The created subtask
   */
  public Task createSubtask(UUID parentId, String name, double size, int subtaskCount) {
    Task parent = tasks.get(parentId);
    if (parent == null) {
      throw new IllegalArgumentException("Parent task not found");
    }
    Task subtask = new Task(name, size, subtaskCount, parent);
    tasks.put(subtask.getId(), subtask);
    return subtask;
  }

  /**
   * Gets a task by its ID.
   *
   * @param taskId The ID of the task to get
   * @return The task, or null if not found
   */
  public Task getTask(UUID taskId) {
    return tasks.get(taskId);
  }

  /**
   * Prints the current state of the computation progress tree.
   */
  public void printProgressTree() {
    if (rootTask != null) {
      printTaskTree(rootTask, 0);
    }
  }

  /**
   * Recursively prints a task and its subtasks with indentation.
   */
  private void printTaskTree(Task task, int depth) {
    System.out.println("  ".repeat(Math.max(0, depth)) + task);
    for (Task subtask : task.getSubtasks()) {
      printTaskTree(subtask, depth + 1);
    }
  }

}
