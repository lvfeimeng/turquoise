/*
 * Copyright (C) 2015-2016 S.Violet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project GitHub: https://github.com/shepherdviolet/turquoise
 * Email: shepherdviolet@163.com
 */

package sviolet.turquoise.x.imageloader.node.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import sviolet.turquoise.x.imageloader.node.Task;

/**
 * Created by S.Violet on 2016/3/3.
 */
public class InfiniteRequestQueue implements RequestQueue {

    private List<Task> tasks = new ArrayList<>();

    private final ReentrantLock tasksLock = new ReentrantLock();

    @Override
    public Task put(Task task) {
        try{
            tasksLock.lock();
            tasks.add(task);
        }finally {
            tasksLock.unlock();
        }
        return null;
    }

    @Override
    public Task get() {
        Task task = null;
        try{
            tasksLock.lock();
            if (tasks.size() > 0) {
                task = tasks.remove(0);
            }
        }finally {
            tasksLock.unlock();
        }
        return task;
    }

    @Override
    public void setSize(int size) {
        //do nothing
    }

    @Override
    public void clear() {
        try{
            tasksLock.lock();
            tasks.clear();
        }finally {
            tasksLock.unlock();
        }
    }
}
