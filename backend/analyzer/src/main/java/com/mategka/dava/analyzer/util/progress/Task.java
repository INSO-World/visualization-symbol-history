package com.mategka.dava.analyzer.util.progress;

public sealed interface Task permits SimpleTask, CompoundTask {

  /*
  Notes:
  var task = Task.new(count: 10); // main task size is always 1 (cannot finish manually)
  begin loop;
  var subtask = task.begin(size: 1240, count: 4); // default count is 1; subtasks can be finished manually if count is 1
  begin inner loop;
  var innerSubtask = subtask.begin(size: 1000);
  innerSubtask.end(); // updates subtask; if last inner subtask, then updates task as well
  end inner loop;
  subtask.end();
  end loop;
  // At any time: task.estimateProgress() and task.estimateTimeRemaining()
   */

}
