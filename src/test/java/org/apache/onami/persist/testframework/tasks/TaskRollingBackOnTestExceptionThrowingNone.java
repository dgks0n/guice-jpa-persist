/**
 * Copyright (C) 2012 Stephan Classen
 * Based on guice-perist (Copyright (C) 2010 Google, Inc.)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.onami.persist.testframework.tasks;

import org.apache.onami.persist.Transactional;
import org.apache.onami.persist.testframework.TransactionTestEntity;
import org.apache.onami.persist.testframework.TransactionalTask;
import org.apache.onami.persist.testframework.exceptions.RuntimeTestException;
import org.apache.onami.persist.testframework.exceptions.TestException;

/**
 * Task which stores an entity and will:
 *  - roll back on {@link TestException}.
 *  - throw no new Exception
 *
 * @author Stephan Classen
 */
public class TaskRollingBackOnTestExceptionThrowingNone extends TransactionalTask {

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional(rollbackOn = TestException.class)
  public void doTransactional() throws TestException, RuntimeTestException {
    storeEntity(new TransactionTestEntity());
    doOtherTasks();
  }

}
