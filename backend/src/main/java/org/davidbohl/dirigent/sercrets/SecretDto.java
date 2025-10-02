package org.davidbohl.dirigent.sercrets;

import java.util.List;

public record SecretDto(String environmentVariable, String value, List<String> deployments) {

}

