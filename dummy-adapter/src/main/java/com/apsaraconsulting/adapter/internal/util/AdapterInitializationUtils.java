package com.apsaraconsulting.adapter.internal.util;

import com.apsaraconsulting.adapter.internal.logger.CpiLoggingDecorator;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ilya Nesterov
 */
public class AdapterInitializationUtils {

    public static void loadDependenciesEagerly(CpiLoggingDecorator log) {

        Optional<Bundle> adapterBundleOpt = Optional.ofNullable(FrameworkUtil.getBundle(AdapterInitializationUtils.class));
        adapterBundleOpt.ifPresent(adapterBundle -> {
            try {
                for (Bundle bundle : adapterBundle.getBundleContext().getBundles()) {
                    String bundleName = bundle.getSymbolicName();
                    log.trace("Processing bundle {}", bundleName);

                    if (bundleName.startsWith("org.osgi") || bundleName.endsWith(".monitor")) {
                        log.trace("Skipping bundle {}", bundleName);
                        continue;
                    }

                    List<String> classNames = Collections.list(bundle.findEntries("/", "*.class", true))
                        .stream()
                        .map(url -> url.getPath()
                            .substring(1)
                            .replace(".class", "")
                            .replace("/", ".")
                        )
                        .collect(Collectors.toList());
                    log.trace("Bundle {} has {} classes", bundleName, classNames.size());
                    for (String className : classNames) {
                        try {
                            bundle.loadClass(className);
                        } catch (Throwable ex) {
                            log.warn("Couldn't load class {} from bundle {}. Cause: {}:{}", className, bundleName, ex.getClass(), ex.getMessage());
                        }
                    }
                }
            } catch (Throwable ex) {
                log.error("Couldn't load dependencies eagerly: ", ex);
            }
        });
    }
}
