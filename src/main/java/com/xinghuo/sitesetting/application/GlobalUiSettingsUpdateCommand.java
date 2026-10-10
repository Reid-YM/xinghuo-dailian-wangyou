package com.xinghuo.sitesetting.application;

import java.util.Map;

public record GlobalUiSettingsUpdateCommand(Map<String, String> values) {
}
