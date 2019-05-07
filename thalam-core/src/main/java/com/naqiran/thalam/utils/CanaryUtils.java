package com.naqiran.thalam.utils;

import com.naqiran.thalam.configuration.CanaryResource;
import com.naqiran.thalam.configuration.CanaryTemplate;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.service.model.ServiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@Slf4j
public class CanaryUtils {

    /**
     * Ensure the total value is distributed in percentage. Also
     * @param service
     * @param counter
     * @return CanaryResource
     */
    public static CanaryResource computeWeightedRoundRobin(final Service service, ServiceRequest request, final double counter) {
        if (service != null) {
            final CanaryTemplate template = new CanaryTemplate();
            if (CollectionUtils.isNotEmpty(template.getResources())) {
                double gcd = template.getResources().stream().mapToDouble(CanaryResource::getWeight)
                        .reduce(0, (first, second) -> CanaryUtils.gcd(first, second));
                double baseMod = 0;
                final NavigableMap<Double, CanaryResource> resourceMap = new TreeMap<>();
                for (CanaryResource canaryResource : template.getResources()) {
                    double div = canaryResource.getWeight() / gcd;
                    resourceMap.put((baseMod += div) - 1, canaryResource);
                }

                final Map.Entry<Double, CanaryResource> resourceEntry = resourceMap.ceilingEntry(counter % baseMod);
                return resourceEntry != null ? resourceEntry.getValue() : null;
            } else {
                log.error("Canary Resource is set up incorrectly!!!");
            }
        }
        return null;
    }

    private static double gcd(double first, double second) {
        return (second == 0) ? first : gcd(second, first % second);
    }
}
