package org.davidbohl.dirigent.sercrets.model;

import java.util.List;

public record SecretDto(String key, String environmentVariable, String value, List<String> deployments) {

}

