package org.davidbohl.dirigent.system.api;

import org.davidbohl.dirigent.system.SystemInformation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(path = "/api/v1/system-information")
public class SystemInformationController {

    @Value("${dirigent.instanceName:'Unknown Instance'}")
    private String instanceName;


    @Value("${dirigent.deployments.git.url:}")
    private String gitUrl;

    @GetMapping()
    public SystemInformation get() {
        return new SystemInformation(instanceName, gitUrl);
    }

}
