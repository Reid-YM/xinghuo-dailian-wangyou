package com.xinghuo.frontendcontrol.application;

import java.util.Map;

public record FrontendControlUpdateCommand(Map<String, String> values) {
}
