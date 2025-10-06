package org.davidbohl.dirigent.sercrets;

import java.util.List;

public record SecretDto(String key, String environmentVariable, String value, List<String> deployments) {

}

