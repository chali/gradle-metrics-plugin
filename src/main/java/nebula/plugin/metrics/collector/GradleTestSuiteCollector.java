/*
 *  Copyright 2015-2019 Netflix, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package nebula.plugin.metrics.collector;

import nebula.plugin.metrics.MetricsLoggerFactory;
import nebula.plugin.metrics.dispatcher.MetricsDispatcher;
import nebula.plugin.metrics.model.Result;
import nebula.plugin.metrics.model.Test;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import org.gradle.api.Task;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Collector for Gradle test suite metrics, implementing the {@link TestListener} interface.
 *
 * @author Danny Thomas
 */
public class GradleTestSuiteCollector implements TestListener {
    private static final Logger logger = MetricsLoggerFactory.getLogger(GradleTestSuiteCollector.class);
    private final Supplier<MetricsDispatcher> dispatcherSupplier;
    private final Task task;

    public GradleTestSuiteCollector(Supplier<MetricsDispatcher> dispatcherSupplier, Task task) {
        this.dispatcherSupplier = checkNotNull(dispatcherSupplier);
        this.task = checkNotNull(task);
    }

    @Override
    public void beforeSuite(TestDescriptor suite) {
        checkNotNull(suite);
    }

    @Override
    public void afterSuite(TestDescriptor suite, TestResult result) {
        checkNotNull(suite);
        checkNotNull(result);
    }

    @Override
    public void beforeTest(TestDescriptor testDescriptor) {
        checkNotNull(testDescriptor);
    }

    @Override
    public void afterTest(TestDescriptor testDescriptor, TestResult testResult) {
        checkNotNull(testDescriptor);
        checkNotNull(testResult);
        Result result = getTestResult(testResult);
        org.gradle.api.tasks.testing.Test testTask = (org.gradle.api.tasks.testing.Test) task;
        String suiteName = testTask.getName();
        long startTime = testResult.getStartTime();
        long elapsed = testResult.getEndTime() - startTime;
        Test test = new Test(testDescriptor.getName(), testDescriptor.getClassName(), suiteName, result, new DateTime(startTime), elapsed);
        dispatcherSupplier.get().test(test);
    }

    @VisibleForTesting
    Result getTestResult(TestResult testResult) {
        TestResult.ResultType testResultType = testResult.getResultType();
        List<Throwable> exceptions = testResult.getExceptions();
        Result result;
        switch (testResultType) {
            case SUCCESS:
                result = Result.success();
                break;
            case SKIPPED:
                result = Result.skipped();
                break;
            case FAILURE:
                //noinspection ConstantConditions
                result = Result.failure(exceptions);
                break;
            default:
                logger.warn("Test result carried unknown result type '{}'. Assuming success", testResultType);
                result = Result.success();
        }
        return result;
    }
}
