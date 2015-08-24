/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ratpack.dropwizard.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class RequestTimingHandler implements Handler {

  private final MetricRegistry metricRegistry;
  private final DropwizardMetricsConfig config;

  @Inject
  public RequestTimingHandler(MetricRegistry metricRegistry, DropwizardMetricsConfig config) {
    this.metricRegistry = metricRegistry;
    this.config = config;
  }

  @Override
  public void handle(final Context context) throws Exception {
    context.onClose(outcome -> {
      String timerName = buildRequestTimerTag(outcome.getRequest().getPath(), outcome.getRequest().getMethod().getName());
      String responseCodeCounter = String.valueOf(outcome.getResponse().getStatus().getCode()).substring(0, 1) + "xx-responses";
      metricRegistry.timer(timerName).update(outcome.getDuration().getNano(), TimeUnit.NANOSECONDS);
      metricRegistry.counter(responseCodeCounter).inc();
    });
    context.next();
  }

  private String buildRequestTimerTag(String requestPath, String requestMethod) {
    String tagName = requestPath.equals("") ? "root" : requestPath.replace("/", ".");

    if (config.getRequestMetricGroups() != null) {
      for (Map.Entry<String, String> metricGrouping : config.getRequestMetricGroups().entrySet()) {
        if (requestPath.matches(metricGrouping.getValue())) {
          tagName = metricGrouping.getKey();
          break;
        }
      }
    }

    return tagName + "." + requestMethod.toLowerCase() + "-requests";
  }

}
